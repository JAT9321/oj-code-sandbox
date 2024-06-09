package com.zgt.ojcodesandbox.controller;

import com.zgt.ojcodesandbox.languageCodeSandbox.GTJavaDockerCodeSandbox;
import com.zgt.ojcodesandbox.languageCodeSandbox.JavaNativeCodeSandbox;
import com.zgt.ojcodesandbox.model.ExecuteCodeRequest;
import com.zgt.ojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

@RestController("/")
public class ApiController {

    // 远程调用的鉴权请求头中放入密钥
    public static final String AUTH_REQUEST_HEADER = "JIAO";
    public static final String AUTH_REQUEST_SECRET = "TIAN";
    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    @Resource
    private GTJavaDockerCodeSandbox gtJavaDockerCodeSandbox;

    /**
     * 对外提供接口
     *
     * @param executeCodeRequest
     * @return
     */
    @PostMapping("/execCode")
    public ExecuteCodeResponse execCode(@RequestBody ExecuteCodeRequest executeCodeRequest,
                                        HttpServletRequest request, HttpServletResponse response) {

        // 基本权限验证，防止无关人员调用接口
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)) {
            response.setStatus(403);
            return null;
        }

        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }
        return javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }


}
