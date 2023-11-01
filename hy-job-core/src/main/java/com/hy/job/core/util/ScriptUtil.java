package com.hy.job.core.util;

import com.hy.job.core.context.XxlJobHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: HY
 * @Date: 2023-10-26-16:05
 * @Description:
 * /**
 *  *  1、内嵌编译器如"PythonInterpreter"无法引用扩展包，因此推荐使用java调用控制台进程方式"Runtime.getRuntime().exec()"来运行脚本(shell或python)；
 *  *  2、因为通过java调用控制台进程方式实现，需要保证目标机器PATH路径正确配置对应编译器；
 *  *  3、暂时脚本执行日志只能在脚本执行结束后一次性获取，无法保证实时性；因此为确保日志实时性，可改为将脚本打印的日志存储在指定的日志文件上；
 *  *  4、python 异常输出优先级高于标准输出，体现在Log文件中，因此推荐通过logging方式打日志保持和异常信息一致；否则用prinf日志顺序会错乱
 *  *
 *  */

public class ScriptUtil {

    /**
     * 创建脚本文件
     * @param scriptFileName
     * @param content
     * @throws IOException
     */
    public static void markScriptFile(String scriptFileName , String content)throws IOException {
        //创建文件
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(scriptFileName);
            fileOutputStream.write(content.getBytes("UTF-8"));
            fileOutputStream.close();
        }catch (Exception e){
            throw e;
        }finally {
            if(fileOutputStream != null){
                fileOutputStream.close();
            }
        }
    }

    /**
     * 脚本执行，日志文件实时输出
     *
     * @param command
     * @param scriptFile
     * @param logFile
     * @param params
     * @return
     * @throws IOException
     */
    public static int execToFile(String command, String scriptFile, String logFile, String... params) throws IOException {

        FileOutputStream fileOutputStream = null;
        Thread inputThread = null;
        Thread errThread = null;

        try {
            //创建文件
            fileOutputStream = new FileOutputStream(logFile, true);

            //命令
            List<String> cmdarray = new ArrayList<>();
            cmdarray.add(command);
            cmdarray.add(scriptFile);
            if(params != null && params.length > 0){
                for(String param: params){
                    cmdarray.add(param);
                }
            }

            //动态数组转为静态数组
            String[] cmdarrayFinal = cmdarray.toArray(new String[cmdarray.size()]);

            //执行外部命令
            final Process process = Runtime.getRuntime().exec(cmdarrayFinal);

            //日志线程
            final FileOutputStream finalFileOutputStream = fileOutputStream;

            //执行把外部命令的状态和结果写进输出流
            inputThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        copy(process.getInputStream(), finalFileOutputStream, new byte[1024]);
                    } catch (IOException e) {
                        XxlJobHelper.log(e);
                    }
                }
            });

            //执行发生错误，把错误结果写进输出流
            errThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        copy(process.getErrorStream(), finalFileOutputStream, new byte[1024]);
                    } catch (IOException e) {
                        XxlJobHelper.log(e);
                    }
                }
            });

            inputThread.start();
            errThread.start();

            //等待程序退出值
            int exitValue = process.waitFor();   //exit code: 0=success, 1=error

            //log-thread join
            inputThread.join();
            errThread.join();

            return exitValue;
        }catch (Exception e){
            XxlJobHelper.log(e);
            return -1;
        }finally {
            if (fileOutputStream != null){
                try {
                    fileOutputStream.close();
                }catch (IOException e){
                    XxlJobHelper.log(e);
                }
            }
            if(inputThread != null && inputThread.isAlive()){
                inputThread.interrupt();
            }
            if(errThread != null && errThread.isAlive()){
                errThread.interrupt();
            }
        }
    }


    /**
     * 复制输入流的内容到输出流，同时返回复制的字节数
     *
     * @param inputStream
     * @param outputStream
     * @param buffer
     * @return
     * @throws IOException
     */
    private static long copy(InputStream inputStream, OutputStream outputStream, byte[] buffer) throws IOException {
        try {
            long total = 0;
            for(;;){
                int res = inputStream.read(buffer);
                if(res == -1){
                    break;
                }

                if(res > 0){
                    total += res;
                    if(outputStream != null){
                        outputStream.write(buffer,0,res);
                    }
                }
            }
            outputStream.flush();
            inputStream.close();
            inputStream = null;
            return total;
        }finally {
            if(inputStream != null){
                inputStream.close();
            }
        }
    }


}
