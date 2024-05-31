package com.zgt.ojcodesandbox.languageCodeSandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.zgt.ojcodesandbox.model.ExceuteMessage;
import com.zgt.ojcodesandbox.model.ExecuteCodeRequest;
import com.zgt.ojcodesandbox.model.ExecuteCodeResponse;
import com.zgt.ojcodesandbox.model.JudgeInfo;
import com.zgt.ojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author : JAT
 * @version : 1.0
 * @email : zgt9321@qq.com
 * @since : 2024/5/27
 **/
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {


    // 保存用户上传代码的临时目录
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    // 固定java代码的类名
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    // 超时时间
    private static final long TIME_OUT = 5L;

    ////////// 模板方法拆分  ///////////////////

    // 1 保存用户文件
    public File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录是否存在，没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        // 用户代码隔离存放 临时为其创建一个目录存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    // 2 编译用户代码
    public ExceuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            // 执行cmd命令，会返回一个进程
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExceuteMessage exceuteMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            // System.out.println(exceuteMessage);
            if (exceuteMessage.getExitValue() != 0) {
                System.out.println(exceuteMessage);
                throw new RuntimeException("编译错误");
            }
            return exceuteMessage;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 3 执行代码
    public List<ExceuteMessage> runFile(File userCodeFile, List<String> inputList) {

        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();

        // 执行编译后的代码， 得到输出结果  -Dfile.encoding=UTF-8解决控制台输出乱码 -Xms限制最大堆
        // 保存每组示例的执行结果
        List<ExceuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            // 设置字符集
            //  String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            // 限制堆大小
            // String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main", userCodeParentPath);
            // 限制权限
            //  String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeParentPath, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 开启一个新的线程,相当于监听着runProcess执行的时间,超时就杀了它,不完美,示例.
                // 当前的命令是否超时的标识符
                final boolean[] isSuccess = {false};
                new Thread(() -> {
                    try {
                        int usedTime = 0;
                        while (!isSuccess[0] && usedTime <= TIME_OUT) {
                            Thread.sleep(1000L);
                            usedTime++;
                        }
                        if (usedTime >= TIME_OUT) {
                            System.out.println("超时,中断");
                            runProcess.destroy();
                        } else {
                            System.out.println("程序正常执行完成");
                        }
                    } catch (InterruptedException e) {
                        System.out.println(e);
                    }
                }).start();

                // ExceuteMessage exceuteMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                // ACM格式 用户代码编写时用Scanner接收数据
                // ExceuteMessage exceuteMessage = ProcessUtils.runInteractProcessAndGetMessage(runProcess, inputArgs);
                ExceuteMessage exceuteMessage = ProcessUtils.runInteractProcessAndGetMessage2(runProcess, inputArgs);
                // 程序执行完成
                isSuccess[0] = true;
                System.out.println(exceuteMessage);
                executeMessageList.add(exceuteMessage);
            } catch (IOException e) {
                throw new RuntimeException("程序执行异常");
            }
        }
        return executeMessageList;
    }

    // 4 处理返回结果
    public ExecuteCodeResponse getOutputResponse(List<ExceuteMessage> executeMessageList) {
        // 收集需要的输出响应数据
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        ArrayList<String> outputList = new ArrayList<>();
        // 最大用时
        long maxTime = 0;
        for (ExceuteMessage exceuteMessage : executeMessageList) {
            String errorMessage = exceuteMessage.getErrorMessage();
            // 在现在的逻辑里,如果某个用例运行出错,才会有错误信息(ErrorMessage),否则ErrorMessage为空
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                // 执行出错
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(exceuteMessage.getMessage());
            Long execTime = exceuteMessage.getTime();
            maxTime = Math.max(maxTime, execTime);
        }

        // 正常运行完成全部用例,则outputlist的长度和executeMessageList长度相同,否则出错了
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }

        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        // 内存消耗信息,没搞,统计起来麻烦
//        judgeInfo.setMemory();

        executeCodeResponse.setJudgeInfo(judgeInfo);

        return executeCodeResponse;
    }

    // 5 删除用户代码文件
    public boolean deleteFile(File userCodeFile) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        // 文件清理 把临时生成存放文件的目录一起删掉
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        // 用户代码保存为文件
        File userCodeFile = saveCodeToFile(code);
        // 编译代码 得到class文件
        ExceuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println(compileFileExecuteMessage);
        // 执行代码，得到输出
        List<ExceuteMessage> exceuteMessageList = runFile(userCodeFile, inputList);
        // 处理输出结果
        ExecuteCodeResponse executeCodeResponse = getOutputResponse(exceuteMessageList);
        // 删除文件
        boolean deleteFileFlag = deleteFile(userCodeFile);
        if (!deleteFileFlag) {
            log.error("deleteFIle error, userCodeFilePath = {}", userCodeFile.getAbsolutePath());
        }
        return executeCodeResponse;
    }

    /**
     * 错误处理方法,执行沙箱出错时,直接返回响应对象
     *
     * @param e
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 目前  抛出异常代表沙箱出问题,代码出错不会抛异常
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
