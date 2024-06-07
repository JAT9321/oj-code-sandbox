package com.zgt.ojcodesandbox.controller;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.DockerClientBuilder;
import com.zgt.ojcodesandbox.languageCodeSandbox.CodeSandbox;
import com.zgt.ojcodesandbox.languageCodeSandbox.JavaDockerCodeSandbox;
import com.zgt.ojcodesandbox.languageCodeSandbox.JavaDockerCodeSandboxOld;
import com.zgt.ojcodesandbox.languageCodeSandbox.JavaNativeCodeSandbox;
import com.zgt.ojcodesandbox.model.ExecuteCodeRequest;
import com.zgt.ojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author : JAT
 * @version : 1.0
 * @email : zgt9321@qq.com
 * @since : 2024/5/25
 **/

@RestController
public class Test {

    @GetMapping("/hello")
    public String hello() {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        // docker环境信息
        Info info = dockerClient.infoCmd().exec();
        String infoStr = JSONUtil.toJsonStr(info);
        System.out.println("docker的环境信息如下：=================");
        System.out.println(infoStr);
        return infoStr;

    }

    @GetMapping("/docker")
    public String docker() {
        CodeSandbox CodeSandbox = new JavaDockerCodeSandboxOld();
        // JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        // executeCodeRequest.setInputList(Arrays.asList("4\\n1 2 3 4", "3\\n1 2 3"));
        executeCodeRequest.setInputList(Arrays.asList("4", "3"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        // ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        ExecuteCodeResponse executeCodeResponse = CodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
        return JSONUtil.toJsonStr(executeCodeResponse);
    }


}
