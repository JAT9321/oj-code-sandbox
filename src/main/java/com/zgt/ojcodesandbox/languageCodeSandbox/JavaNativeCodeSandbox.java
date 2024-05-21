package com.zgt.ojcodesandbox.languageCodeSandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.zgt.ojcodesandbox.CodeSandbox;
import com.zgt.ojcodesandbox.model.ExceuteMessage;
import com.zgt.ojcodesandbox.model.ExecuteCodeRequest;
import com.zgt.ojcodesandbox.model.ExecuteCodeResponse;
import com.zgt.ojcodesandbox.model.JudgeInfo;
import com.zgt.ojcodesandbox.security.DefaultSecurityManager;
import com.zgt.ojcodesandbox.utils.ProcessUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaNativeCodeSandbox implements CodeSandbox {

    // 保存用户上传代码的临时目录
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    // 固定java代码的类名
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    //超时时间
    private static final long TIME_OUT = 5000L;
    //代码里敏感包,方法调用检测 黑名单
    private static final List<String> BLACK_LIST = Arrays.asList("Files", "exec");
    private static final WordTree WORD_TREE;

    // 自定义的安全管理器,只在运行用户的代码时实施,在cmd命令中指定
    private static final String SECURITY_MANAGER_PATH = "C:\\code\\oj-code-sandbox\\src\\main\\resources\\security";
    // 自定义的安全管理器名称
    private static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";

    static {
        // 初始化字典树 校验代码是否包含黑名单中的命令
        // HuTool工具包中的字典树(高效查找词汇)
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(BLACK_LIST);
    }


    public static void main(String[] args) {
        JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();
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

        //权限校验 读写权限. 实际情况我们只要限制用户代码执行时的权限控制,放在这里是不合适的.
//        System.setSecurityManager(new DefaultSecurityManager());

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        //检验用户代码中的敏感词,防攻击
        //  WordTree wordTree = new WordTree(); 在静态代码块中初始化
        // wordTree.addWords(BLACK_LIST);
        FoundWord foundWord = WORD_TREE.matchWord(code);
        if (foundWord != null) {
            System.out.println("包含敏感词: " + foundWord);
            return null;
        }


        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;

        // 判断全局代码目录是否存在，没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        //用户代码隔离存放 临时为其创建一个目录存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

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

        // 执行编译后的代码， 得到输出结果  -Dfile.encoding=UTF-8解决控制台输出乱码 -Xms限制最大堆
        // 保存每组示例的执行结果
        List<ExceuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            // 设置字符集
            //  String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            // 限制堆大小
              String runCmd = String.format("java -Xms256 -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            // 限制权限
            //  String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeParentPath, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);

                // 开启一个新的线程,相当于监听着runProcess执行的时间,超时就杀了它,不完美,示例.
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时,中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();

                ExceuteMessage exceuteMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                // ACM格式 用户代码编写时用Scanner接收数据
                // ExceuteMessage exceuteMessage = ProcessUtils.runInteractProcessAndGetMessage(runProcess, inputArgs);
                System.out.println(exceuteMessage);
                executeMessageList.add(exceuteMessage);
            } catch (IOException e) {
                return getErrorResponse(e);
            }
        }

        // 收集需要的输出响应数据
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        ArrayList<String> outputList = new ArrayList<>();
        //最大用时
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

        //正常运行完成全部用例,则outputlist的长度和executeMessageList长度相同,否则出错了
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }

        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        //内存消耗信息,没搞,统计起来麻烦
//        judgeInfo.setMemory();

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
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        //目前  抛出异常代表沙箱出问题,代码出错不会抛异常
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }

}
