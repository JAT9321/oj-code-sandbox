package com.zgt.ojcodesandbox.docker;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONParser;
import cn.hutool.json.JSONUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class DockerDemo {



    public static void main(String[] args) throws InterruptedException {
        // 获取默认的 Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance("tcp://121.37.154.99:2375").build();

        // docker环境信息
        Info info = dockerClient.infoCmd().exec();
        String infoStr = JSONUtil.toJsonStr(info);
        System.out.println("docker的环境信息如下：=================");
        System.out.println(infoStr);

        // String image = "nginx:latest";
        // // 拉取镜像
        // PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
        // PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
        //     @Override
        //     public void onNext(PullResponseItem item) {
        //         System.out.println("下载镜像：" + item.getStatus());
        //         super.onNext(item);
        //     }
        // };
        // pullImageCmd
        //         .exec(pullImageResultCallback)
        //         .awaitCompletion();
        // System.out.println("下载完成");


        // 创建容器
        // CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        // CreateContainerResponse createContainerResponse = containerCmd
        //         .withCmd("echo", "Hello Docker")
        //         .exec();
        // System.out.println(createContainerResponse);
        // String containerId = createContainerResponse.getId();
        //
        // // 查看容器状态
        // ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
        // List<Container> containerList = listContainersCmd.withShowAll(true).exec();
        // for (Container container : containerList) {
        //     System.out.println(container);
        // }

        // 启动容器
        // String containerId = "009f66e6d02a29740ba42ce6be89989d41daad7dd1b3749041f11098aaa4bde1";
        // dockerClient.startContainerCmd(containerId).exec();
        //
        //
        // // 查看日志
        //
        // LogContainerResultCallback logContainerResultCallback = new LogContainerResultCallback() {
        //     @Override
        //     public void onNext(Frame item) {
        //         System.out.println(item.getStreamType());
        //         System.out.println("日志：" + new String(item.getPayload()));
        //         super.onNext(item);
        //     }
        // };
        //
        //
        // dockerClient.logContainerCmd(containerId)
        //         .withStdErr(true)
        //         .withStdOut(true)
        //         .exec(logContainerResultCallback)
        //         .awaitCompletion();

        // 删除容器
        // String containerId = "009f66e6d02a29740ba42ce6be89989d41daad7dd1b3749041f11098aaa4bde1";
        // dockerClient.removeContainerCmd(containerId).withForce(true).exec();

        // 删除镜像
        // dockerClient.removeImageCmd(image).exec();
    }
}
