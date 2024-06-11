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
import org.springframework.stereotype.Service;
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
 * @since : 2024/5/25
 **/

@Service
public class GTJavaDockerCodeSandbox implements CodeSandbox {

    // 保存用户上传代码的临时目录
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    // 固定java代码的类名
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    // 输入文件名称
    private static final String GLOBAL_INPUT_ARGS_NAME = "input.txt";
    // 超时时间
    private static final long TIME_OUT = 5000L;
    // 是否拉取过镜像了
    private static final boolean FIRST_INIT = false;


    public static void main(String[] args) {
        JavaNativeCodeSandboxOld javaNativeCodeSandbox = new JavaNativeCodeSandboxOld();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        //  String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        //  String code = ResourceUtil.readStr("testCode/unsafe/SleepError.java", StandardCharsets.UTF_8);
        String code = ResourceUtil.readStr("testCode/unsafe/RunFileError.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;

        // 判断全局代码目录是否存在，没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        // 用户代码隔离存放 临时为其创建一个目录存放
        String randomName = UUID.randomUUID() + "";
        String userCodeParentPath = globalCodePathName + File.separator + randomName;
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        String testInputFilePath = userCodeParentPath + File.separator + GLOBAL_INPUT_ARGS_NAME;

        // 编译代码 得到class文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            // 执行cmd命令，会返回一个进程
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExceuteMessage exceuteMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(exceuteMessage);
        } catch (IOException e) {
            return getErrorResponse(e);
        }
        // 创建容器，上传编译过后的文件
        // 获取默认的 Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        String containerId = "8a01c44ae767bc86420a66a5c24647fa1639963b8d68481749f820b3aa673810";

        // 存放执行结果
        List<ExceuteMessage> executeMessageList = new ArrayList<>();
        // 统计执行时间
        StopWatch stopWatch = new StopWatch();

        for (String inputArgs : inputList) {
            // 输入命令
            // docker exec -it 8a0 /bin/sh /app/test/run.sh
            String[] cmdArray = new String[]{"/bin/sh", "/app/run.sh", randomName};
            // 测试数据保存到数据库\n会变成\\n，这里先替换掉这种方式
            inputArgs = inputArgs.replace("\\n", "\n");
            // 创建输入文件 覆盖模式
            File testInputFile = FileUtil.writeString(inputArgs, testInputFilePath, StandardCharsets.UTF_8);
            System.out.println("测试用例路径：" + testInputFile.getAbsolutePath());
            // 创建命令 java命令
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();
            System.out.println("创建执行命令：" + execCreateCmdResponse);
            // 直接结果返回
            ExceuteMessage exceuteMessage = new ExceuteMessage();
            // java的执行要求，在匿名内部类或者lambna表达式中的外部变量的引用不能变，所以设置为数组形式
            final String[] message = {""};
            final String[] errorMessage = {""};
            final Long[] time = {0L};
            // 超时判定
            final boolean[] timedOut = {true};
            // 命令id，执行命令穿这个id，
            String execId = execCreateCmdResponse.getId();
            // 执行回调
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    // 输出结果类型
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    } else {
                        byte[] payload = frame.getPayload();
                        // System.out.println(new String(payload));
                        message[0] += new String(payload);
                        // System.out.println("输出结果：" + message[0]);
                    }
                    super.onNext(frame);
                }

                @Override
                public void onComplete() {
                    timedOut[0] = false;
                    super.onComplete();

                }
            };


            // 内存占用
            final long[] maxMemory = {0L};
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            final boolean[] isOver = {false};
            ResultCallback<Statistics> statisticsResultCallback = new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    Long usage = statistics.getMemoryStats().getUsage();
                    maxMemory[0] = Math.max(usage == null ? 0 : usage, maxMemory[0]);
                    System.out.println("内存占用：" + maxMemory[0]);
                    if (isOver[0]) {
                        Thread.currentThread().stop();
                    }
                }

                @Override
                public void onStart(Closeable closeable) {
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onComplete() {
                }

                @Override
                public void close() throws IOException {
                }
            };
            statsCmd.exec(statisticsResultCallback);
            // 执行命令
            try {
                stopWatch.start();
                dockerClient.execStartCmd(execId).exec(execStartResultCallback).awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
                time[0] = stopWatch.getLastTaskTimeMillis();
                isOver[0] = true;
                statsCmd.close();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            } finally {
            }
            // 保存执行输出结果
            // 因为是ACM格式，我们得到用户的输出是通过System.out.println(answer) 这样会多出一个换行符，我们要把这个换行符拿掉
            int lastIndexOf = message[0].lastIndexOf("\n");
            if (lastIndexOf != -1) {
                message[0] = message[0].substring(0, lastIndexOf);
            }
            exceuteMessage.setMessage(message[0]);
            exceuteMessage.setErrorMessage(errorMessage[0]);
            // 超时判断， 如果timedOut[0]为真说明没有完成判题（返回-1），为假完成判题，
            exceuteMessage.setTime(!timedOut[0] ? time[0] : -1);
            exceuteMessage.setMemory(maxMemory[0]);
            executeMessageList.add(exceuteMessage);

        }
        // 收集需要的输出响应数据
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setMessage("=== Accepted ===");
        ArrayList<String> outputList = new ArrayList<>();
        // 最大用时
        long maxTime = 0;
        // 最大内存占用
        long maxMemory = 0L;
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
            Long execMemory = exceuteMessage.getMemory();
            maxTime = Math.max(maxTime, execTime);
            maxMemory = Math.max(maxMemory, execMemory);
        }

        // 正常运行完成全部用例,则outputlist的长度和executeMessageList长度相同,否则出错了
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }

        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);

        executeCodeResponse.setJudgeInfo(judgeInfo);

        // 文件清理 把临时生成存放文件的目录一起删掉
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }
        return executeCodeResponse;
    }

    /**
     * 错误处理方法,执行沙箱出错时,直接返回响应对象
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
