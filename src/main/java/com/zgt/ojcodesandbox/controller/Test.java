package com.zgt.ojcodesandbox.controller;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.DockerClientBuilder;
import com.zgt.ojcodesandbox.service.DockerService;
import com.zgt.ojcodesandbox.languageCodeSandbox.*;
import com.zgt.ojcodesandbox.model.DockerInfo;
import com.zgt.ojcodesandbox.model.ExecuteCodeRequest;
import com.zgt.ojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * @author : JAT
 * @version : 1.0
 * @email : zgt9321@qq.com
 * @since : 2024/5/25
 **/

@RestController
public class Test {

    @Resource
    private DockerService dockerService;

    @Resource
    private GTJavaDockerCodeSandbox gtJavaDockerCodeSandbox;

    @Resource
    DockerClient dockerClient;

    @GetMapping("/dk")
    public String dk() {
        String image = "openjdk:8-alpine";
        // DockerInfo dockerInfo = dockerService.createDockerContainer(image);
        // System.out.println("新建容器：" + dockerInfo.toString());
        List<DockerInfo> dockerInfoList = dockerService.getDockerInfo(image);
        DockerInfo delInfo = new DockerInfo();
        // delInfo.setContainerId("20b55fd646eb284654fc269145a69610a6c64ef6bbae0e45aa5e7d5db7ca427a");
        // dockerService.deleteDockerContainer(delInfo);
        // System.out.println("删除成功");
        return JSONUtil.toJsonStr(dockerInfoList);
    }

    @GetMapping("/hello")
    public String hello() {
        // docker环境信息
        Info info = dockerClient.infoCmd().exec();
        String infoStr = JSONUtil.toJsonStr(info);
        System.out.println("docker的环境信息如下：=================");
        System.out.println(infoStr);
        return infoStr;

    }

    @GetMapping("/docker")
    public String docker() {
        // CodeSandbox CodeSandbox = new GTJavaDockerCodeSandbox();
        // JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("4\n1 2 3 4", "3\n1 2 3"));
        // executeCodeRequest.setInputList(Arrays.asList("4", "3"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        // ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        ExecuteCodeResponse executeCodeResponse = gtJavaDockerCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
        return JSONUtil.toJsonStr(executeCodeResponse);
    }

}
