package com.zgt.ojcodesandbox.languageCodeSandbox;

import cn.hutool.core.io.resource.ResourceUtil;
import com.zgt.ojcodesandbox.model.ExecuteCodeRequest;
import com.zgt.ojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author : JAT
 * @version : 1.0
 * @email : zgt9321@qq.com
 * @since : 2024/5/27
 **/

@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate{

    public static void main(String[] args) {
        JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
         String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
         // String code = ResourceUtil.readStr("testCode/unsafe/SleepError.java", StandardCharsets.UTF_8);
        // String code = ResourceUtil.readStr("testCode/unsafe/RunFileError.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
