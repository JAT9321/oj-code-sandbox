package com.zgt.ojcodesandbox.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.zgt.ojcodesandbox.service.DockerService;
import com.zgt.ojcodesandbox.model.DockerInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class DockerServiceImpl implements DockerService {

    @Resource
    private DockerClient dockerClient;

    // 保存用户上传代码的临时目录
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public static final String DOCKER_INFO_KEY = "docker:info";

    @Override
    public List<DockerInfo> getDockerInfo(String imageName) {
        List<Container> containerList = dockerClient.listContainersCmd().exec();
        List<DockerInfo> dockerInfoList = new ArrayList<DockerInfo>();
        for (Container container : containerList) {
            if (container.getImage().equals(imageName)) {
                DockerInfo dockerInfo = new DockerInfo();
                dockerInfo.setContainerId(container.getId());
                dockerInfo.setRunCount(0L);
                dockerInfo.setStatus(container.getStatus());
                dockerInfo.setState(container.getState());
                dockerInfoList.add(dockerInfo);
                dockerInfo.setImageName(container.getImage());
            }
        }
        return dockerInfoList;
    }

    @Override
    public DockerInfo createDockerContainer(String imageName) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;

        // 判断全局代码目录是否存在，没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        // 拉取镜像
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(imageName);
        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
            @Override
            public void onNext(PullResponseItem item) {
                System.out.println("下载镜像：" + item.getStatus());
                super.onNext(item);
            }
        };
        try {
            pullImageCmd
                    .exec(pullImageResultCallback)
                    .awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("下载完成");

        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(imageName);
        // 镜像运行时的相关参数指定，卷映射，内存指定等
        HostConfig hostConfig = new HostConfig();
        // 卷映射，挂载
        hostConfig.setBinds(new Bind(globalCodePathName, new Volume("/app")));
        // 内存限制
        hostConfig.withMemory(100 * 1000 * 1000L);
        // cpu限制
        hostConfig.withCpuCount(1L);

        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                // 关闭容器的网络 安全性
                .withNetworkDisabled(true)
                // 限制用户不能在root目录下进行写文件
                .withReadonlyRootfs(true)
                // 容器和外部的输入输出打开
                .withAttachStdin(true).withAttachStdout(true).withAttachStderr(true).withTty(true)
                // 执行
                .exec();
        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();

        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();
        System.out.println("容器id：" + containerId + " 容器启动完成");
        DockerInfo dockerInfo = new DockerInfo();
        dockerInfo.setImageName(imageName);
        dockerInfo.setContainerId(containerId);
        dockerInfo.setRunCount(0L);

        return dockerInfo;
    }

    @Override
    public void deleteDockerContainer(DockerInfo dockerInfo) {
        dockerClient.stopContainerCmd(dockerInfo.getContainerId()).exec();
        dockerClient.removeContainerCmd(dockerInfo.getContainerId()).exec();
    }

    // 执行时，可能会有多线程访问问题，直接上锁
    @Override
    public String getContainerIdByLRU() {
        synchronized (DockerServiceImpl.class) {
            // System.out.println("here ====================");
            String containerList = redisTemplate.opsForValue().get(DOCKER_INFO_KEY);

            // 如果没有一个可用的，暂时切换到本地的JavaNativeCodeSandBox执行代码，返回Null
            if (containerList == null || containerList.length() == 0) {
                return null;
            }
            List<DockerInfo> dockerInfos = JSONUtil.toList(containerList, DockerInfo.class);
            DockerInfo useContainerByLRU = dockerInfos.get(0);
            for (DockerInfo dockerInfo : dockerInfos) {
                if (dockerInfo.getLastExecTime() < useContainerByLRU.getLastExecTime()) {
                    useContainerByLRU = dockerInfo;
                }
            }
            // 更新使用时间
            useContainerByLRU.setLastExecTime(LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli());
            redisTemplate.opsForValue().set(DOCKER_INFO_KEY, JSONUtil.toJsonStr(dockerInfos));
            return useContainerByLRU.getContainerId();
        }
    }
}
