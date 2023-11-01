package com.hy.job.core.log;

import com.hy.job.core.biz.model.LogResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author: HY
 * @Date: 2023-10-06-14:21
 * @Description:日志信息处理
 */
public class XxlJobFileAppender {

    private static Logger logger = LoggerFactory.getLogger(XxlJobFileAppender.class);
    /**
     * log base path
     *
     * strut like:
     * 	---/
     * 	---/gluesource/
     * 	---/gluesource/10_1514171108000.js
     * 	---/gluesource/10_1514171108000.js
     * 	---/2017-12-25/
     * 	---/2017-12-25/639.log
     * 	---/2017-12-25/821.log
     *
     */
    private static String logBasePath = "/data/applogs/xxl-job/jobhandler";

    private static String glueSrcPath = logBasePath.concat("/gluesource");

    /**
     * 初始化路径与目录
     * @param logPath
     */
    public static void initLogPath(String logPath){
        //初始化
        if(logPath != null && logPath.trim().length() > 0){
            logBasePath = logPath;
        }

        //创建目录
        File logPathDir = new File(logBasePath);
        if(!logPathDir.exists()){
            logPathDir.mkdirs();
        }
        logBasePath = logPathDir.getPath();

        //创建glue目录
        File glueBaseDir = new File(logPathDir,"gluesource");
        if(!glueBaseDir.exists()){
            glueBaseDir.mkdirs();
        }
        glueSrcPath = glueBaseDir.getPath();
    }
    public static String getLogPath() {
        return logBasePath;
    }

    public static String getGlueSrcPath(){return glueSrcPath;}

    /**
     * log filename, like "logPath/yyyy-MM-dd/9999.log"
     *用于创建存放日志的文件夹和创建并返回日志路径
     * @param triggerDate
     * @param logId
     * @return
     */
    public static String makeLogFileName(Date triggerDate, long logId) {

        //文件路径形式：filePath/yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");   //避免格式出问题，统一格式
        File logFilePath = new File(getLogPath(),sdf.format(triggerDate));
        //判断目录下是否有这个子目录,没有就创建新的
        if(!logFilePath.exists()){
            logFilePath.mkdir();
        }

        //创建日志的路径与名称
        String logFileName = logFilePath.getPath()
                .concat(File.separator)
                .concat(String.valueOf(logId))
                .concat(".log");

        return logFileName;
    }

    /**
     * 在指定日志文件下添加日志内容
     * @param logFileName
     * @param appendLog
     */
    public static void appendLog(String logFileName, String appendLog) {

        // log file
        if (logFileName==null || logFileName.trim().length()==0) {
            return;
        }
        File logFile = new File(logFileName);

        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return;
            }
        }

        // log
        if (appendLog == null) {
            appendLog = "";
        }
        appendLog += "\r\n";

        // append file content
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(logFile, true);
            fos.write(appendLog.getBytes("utf-8"));
            fos.flush();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

    }



    /**
     * 从指定行开始取日志内容
     * @param logFileName
     * @param fromLineNum
     * @return
     */
    public static LogResult readLog(String logFileName, int fromLineNum) {

        //判断文件名是否有效
        if (logFileName==null || logFileName.trim().length()==0) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not found", true);
        }

        //判断文件是否存在
        File logFile = new File(logFileName);
        if (!logFile.exists()) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not exists", true);
        }

        //读取文件内容
        StringBuffer logContentBuffer = new StringBuffer();
        int toLineNum = 0;
        LineNumberReader reader = null;

        try {
            reader = new LineNumberReader(new InputStreamReader(new FileInputStream(logFile),"utf-8"));
            String line = null;

            while((line = reader.readLine()) != null){
                toLineNum = reader.getLineNumber();
                if(toLineNum >= fromLineNum){
                    logContentBuffer.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if(reader != null){
                try{
                    reader.close();
                }catch (IOException e){
                    logger.error(e.getMessage(),e);
                }
            }
            }

        //返回结果
        LogResult logResult = new LogResult(fromLineNum, toLineNum, logContentBuffer.toString(), false);
        return logResult;
    }


    /**
     * 读取全部日志文件
     * @param logFile
     * @return
     */
    public static String readLines(File logFile){
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), "utf-8"));
            if (reader != null) {
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return null;
    }


}
