package com.zgt.ojcodesandbox.Service.impl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.sun.xml.internal.bind.v2.TODO;
import com.zgt.ojcodesandbox.Service.DockerService;
import com.zgt.ojcodesandbox.model.DockerInfo;
import org.springframework.stereotype.Service;
import com.zgt.ojcodesandbox.Service.DockerService;

import java.util.ArrayList;
import java.util.List;

@Service
public class DockerServiceImpl implements DockerService {

    private DockerClient dockerClient = DockerClientBuilder.getInstance().build();

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

    // TODO: 找这个userCodeParentPath如何传递 2024年6月14日
    @Override
    public DockerInfo createDockerContainer(String imageName, String userCodeParentPath) {
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
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
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
        System.out.println("容器启动完成");
        DockerInfo dockerInfo = new DockerInfo();
        dockerInfo.setImageName(imageName);
        dockerInfo.setContainerId(containerId);
        dockerInfo.setRunCount(0L);

        return dockerInfo;
    }

    @Override
    public boolean deleteDockerContainer(DockerInfo dockerInfo) {
        return false;
    }
}
