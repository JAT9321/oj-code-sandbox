package com.zgt.ojcodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.zgt.ojcodesandbox.model.ExceuteMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 执行cmd命令后的 信息处理
 */
public class ProcessUtils {

    /**
     * @param runProcess 当前执行命令的进程
     * @param opName     进程操作名称 比如 是编译 执行等
     * @return
     */
    public static ExceuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        ExceuteMessage exceuteMessage = new ExceuteMessage();
        try {
            // 程序执行用时
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            // 等待程序执行完成，得到返回码
            int exitValue = runProcess.waitFor();

            stopWatch.stop();
            long taskTimeMillis = stopWatch.getLastTaskTimeMillis();

            exceuteMessage.setTime(taskTimeMillis);
            exceuteMessage.setExitValue(exitValue);
            // 正常退出
            if (exitValue == 0) {
                System.out.println(opName + "成功");
                // 获得执行的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                List<String> outputList = new ArrayList<String>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputList.add(compileOutputLine);
                }
                exceuteMessage.setMessage(StringUtils.join(outputList, "\n"));
//                System.out.println(compileOutputStringBuilder.toString());
            } else {
                // 异常退出
                System.out.println(opName + "失败，错误码：" + exitValue);
                // 获得执行的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                List<String> outputList = new ArrayList<String>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputList.add(compileOutputLine);
                }
                exceuteMessage.setMessage(StringUtils.join(outputList, "\n"));
//                System.out.println(compileOutputStringBuilder.toString());
                // 获取执行过程中的错误输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                List<String> errorCompileOutputList = new ArrayList<String>();
                // 逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorCompileOutputList.add(errorCompileOutputLine);
                }
                exceuteMessage.setErrorMessage(StringUtils.join(errorCompileOutputList, "\n"));
            }
        } catch (InterruptedException | IOException e) {
//            throw new RuntimeException(e);
            e.printStackTrace();
        }
        return exceuteMessage;
    }

    /**
     * 交互式的运行方式，就跟那个ex的ACM类似 用户用new Scanner 接收，自己处理输入到可用的形式
     *
     * @param process
     * @param args
     * @return
     */
    public static ExceuteMessage runInteractProcessAndGetMessage(Process runProcess, String args) {
        ExceuteMessage exceuteMessage = new ExceuteMessage();
        try {
            // 向控制台输入程序
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            // String[] s = args.split(" ");
            // String join = StrUtil.join("\n", s) + "\n";
            outputStreamWriter.write(args);
            // 相当于按了回车，执行输入的发送
            outputStreamWriter.flush();

            System.out.println("执行成功");

            // 分批获取进程的正常输出
            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            // 逐行读取
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }
            exceuteMessage.setMessage(compileOutputStringBuilder.toString());
            // 记得资源的释放，否则会卡死
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return exceuteMessage;
    }


    public static ExceuteMessage runInteractProcessAndGetMessage2(Process runProcess, String args) {
        ExceuteMessage exceuteMessage = new ExceuteMessage();
        try {
            // 向控制台输入程序
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            // 无语，JSONUtil工具会把\n变\\n，失去转移字符的意义，这里换回去，先这样吧
            outputStreamWriter.write(args.replace("\\n", "\n"));
            // 相当于按了回车，执行输入的发送
            outputStreamWriter.flush();

            // 记得资源的释放，否则会卡死
            outputStreamWriter.close();
            outputStream.close();
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            int exitValue = runProcess.waitFor();
            stopWatch.stop();
            exceuteMessage.setTime(stopWatch.getLastTaskTimeMillis());
            exceuteMessage.setExitValue(exitValue);

            if (exitValue == 0) {
                System.out.println("执行成功");
                // 获得执行的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                List<String> outputList = new ArrayList<String>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputList.add(compileOutputLine);
                }
                exceuteMessage.setMessage(StringUtils.join(outputList, "\n"));
                // System.out.println(exceuteMessage);
            } else {
                // 异常退出
                // 获得执行的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                List<String> outputList = new ArrayList<String>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputList.add(compileOutputLine);
                }
                exceuteMessage.setMessage(StringUtils.join(outputList, "\n"));
                // System.out.println(exceuteMessage.getMessage());
                // 获取执行过程中的错误输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                List<String> errorCompileOutputList = new ArrayList<String>();
                // 逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorCompileOutputList.add(errorCompileOutputLine);
                }
                exceuteMessage.setErrorMessage(StringUtils.join(errorCompileOutputList, "\n"));
                // System.out.println(exceuteMessage.getErrorMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            runProcess.destroy();
        }
        return exceuteMessage;
    }
}
