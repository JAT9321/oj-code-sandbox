package com.zgt.ojcodesandbox.cron;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.zgt.ojcodesandbox.service.DockerService;
import com.zgt.ojcodesandbox.model.DockerInfo;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : JAT
 * @version : 1.0
 * @email : zgt9321@qq.com
 * @since : 2024/6/15
 **/
@Configuration
public class Task {


    @Resource
    private DockerService dockerService;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public static final String DOCKER_INFO_KEY = "docker:info";

    public static final int DOCKER_CONTAINER_COUNT = 2;
    public static final String DOCKER_IMAGE_NAME = "openjdk:8-alpine";

    /**
     * //每2小时检测 是否有三个docker 容器存活，没有三个就新建
     * * 定时任务，用于维护docker容器的存活状态
     */
    @Scheduled(cron = "0 0 0/2 * * ? ") // 每两个小时执行一次
    public void executeDocker() {
        // 从redis中获得之前正常运行的容器信息
        String containersStr = redisTemplate.opsForValue().get(DOCKER_INFO_KEY);
        if (StrUtil.isBlank(containersStr)) {
            System.out.println("未从redis中获得列表");
            return;
        }
        List<DockerInfo> dockerInfoListFromRedis = JSONUtil.toList(containersStr, DockerInfo.class);
        // 从docker中获得容器信息
        List<DockerInfo> dockerInfoListFromDocker = dockerService.getDockerInfo(DOCKER_IMAGE_NAME);
        // 如果redis中存有，而docker里面现在已经没有了，代表出问题了，删了他，新建个。
        List<DockerInfo> dockerInfoListDeath = new ArrayList<DockerInfo>();
        for (DockerInfo inRedis : dockerInfoListFromRedis) {
            boolean isAlive = false;
            for (DockerInfo inDocker : dockerInfoListFromDocker) {
                if (inRedis.getContainerId().equals(inDocker.getContainerId())) {
                    isAlive = true;
                    inDocker.setLastExecTime(inRedis.getLastExecTime());
                    inDocker.setRunCount(inRedis.getRunCount());
                    break;
                }
            }
            if (!isAlive) {
                dockerInfoListDeath.add(inRedis);
            }
        }
        for (DockerInfo inDeath : dockerInfoListDeath) {
            dockerService.deleteDockerContainer(inDeath);
        }
        // 删除后，我们再新建相同个数的容器，替代死掉的容器
        for (int i = 0; i < dockerInfoListDeath.size(); i++) {
            DockerInfo dockerInfo = dockerService.createDockerContainer(DOCKER_IMAGE_NAME);
            // 当前的时间戳
            dockerInfo.setLastExecTime(LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli());
            dockerInfo.setRunCount(0);
            dockerInfoListFromDocker.add(dockerInfo);
        }
        // 将新的存活docker列表存放到redis中
        redisTemplate.opsForValue().set(DOCKER_INFO_KEY, JSONUtil.toJsonStr(dockerInfoListFromDocker));
    }

    /**
     * 项目启动时，需要做的操作
     */
    @PostConstruct
    public void executeInit() {
        System.out.println("\n== 初始化容器 ==\n");
        // 从docker中获得容器信息
        List<DockerInfo> dockerInfoListFromDocker = dockerService.getDockerInfo(DOCKER_IMAGE_NAME);
        // 删除后，我们再新建相同个数的容器，替代死掉的容器
        int needNewCount = DOCKER_CONTAINER_COUNT - dockerInfoListFromDocker.size();
        for (int i = 0; i < needNewCount; i++) {
            DockerInfo dockerInfo = dockerService.createDockerContainer(DOCKER_IMAGE_NAME);
            dockerInfoListFromDocker.add(dockerInfo);
        }
        List<DockerInfo> dockerInfoList = dockerService.getDockerInfo(DOCKER_IMAGE_NAME);
        for (DockerInfo dockerInfo : dockerInfoList) {
            // 当前的时间戳
            dockerInfo.setLastExecTime(LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli());
            dockerInfo.setRunCount(0);
        }
        // 将新的存活docker列表存放到redis中
        redisTemplate.opsForValue().set(DOCKER_INFO_KEY, JSONUtil.toJsonStr(dockerInfoListFromDocker));
        System.out.println("\n== 完成容器初始化 ==\n");
    }
}
