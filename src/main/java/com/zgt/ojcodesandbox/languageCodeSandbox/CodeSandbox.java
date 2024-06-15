package com.zgt.ojcodesandbox.languageCodeSandbox;


import com.zgt.ojcodesandbox.model.ExecuteCodeRequest;
import com.zgt.ojcodesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口定义
 */
public interface CodeSandbox {

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
