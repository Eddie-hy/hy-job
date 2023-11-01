package com.hy.job.core.context;

import com.hy.job.core.log.XxlJobFileAppender;
import com.hy.job.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/**
 * @Author: HY
 * @Date: 2023-10-21-16:32
 * @Description:处理信息类（包括日志等）
 */
public class XxlJobHelper {

    // ---------------------- base info ----------------------

    /**
     * 得到当前JobId
     */
    public static long getJobId(){
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if(xxlJobContext == null){
            return -1;
        }

        return xxlJobContext.getJobId();
    }

    /**
     * 返回当前JobParam
     */
    public static String getJobParam(){
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if(xxlJobContext == null){
            return null;
        }
        return xxlJobContext.getJobParam();
    }

    // ---------------------- for log ----------------------
    /**
     * 当前的JobLogFileName
     */
    public static String getJobLogFileName(){
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if(xxlJobContext == null) {
            return null;
        }
        return xxlJobContext.getJobLogFileName();
        }

    // ---------------------- for shard ----------------------
    /**
     * current ShardIndex
     *
     * @return
     */
    public static int getShardIndex() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }

        return xxlJobContext.getShardIndex();
    }

    /**
     * current ShardTotal
     *
     * @return
     */
    public static int getShardTotal() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }

        return xxlJobContext.getShardTotal();
    }


    // ---------------------- tool for log ----------------------

    private static Logger logger = LoggerFactory.getLogger("xxl-job logger");

    /**
     * append log with pattern 记录日志并用数据替换字符串
     *
     * @param appendLogPattern  like "aaa {} bbb {} ccc"
     * @param appendLogArguments    like "111, true"
     */
    public static boolean log(String appendLogPattern, Object ... appendLogArguments) {

        FormattingTuple ft = MessageFormatter.arrayFormat(appendLogPattern, appendLogArguments);
        String appendLog = ft.getMessage();

        /*appendLog = appendLogPattern;
        if (appendLogArguments!=null && appendLogArguments.length>0) {
            appendLog = MessageFormat.format(appendLogPattern, appendLogArguments);
        }*/

        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }

    /**
     * append exception stack
     *
     * @param e
     */
    public static boolean log(Throwable e) {

        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String appendLog = stringWriter.toString();

        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }

    /**
     * append log 这个方法的目的是根据上下文决定如何记录日志信息，如果在作业执行上下文中，将日志追加到日志文件中，否则只打印到控制台。
     *
     * @param callInfo
     * @param appendLog
     */
    private static boolean logDetail(StackTraceElement callInfo, String appendLog) {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return false;
        }

        /*// "yyyy-MM-dd HH:mm:ss [ClassName]-[MethodName]-[LineNumber]-[ThreadName] log";
        StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
        StackTraceElement callInfo = stackTraceElements[1];*/

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(DateUtil.formatDateTime(new Date())).append(" ")
                .append("["+ callInfo.getClassName() + "#" + callInfo.getMethodName() +"]").append("-")
                .append("["+ callInfo.getLineNumber() +"]").append("-")
                .append("["+ Thread.currentThread().getName() +"]").append(" ")
                .append(appendLog!=null?appendLog:"");
        String formatAppendLog = stringBuffer.toString();

        // appendlog
        String logFileName = xxlJobContext.getJobLogFileName();

        if (logFileName!=null && logFileName.trim().length()>0) {
            XxlJobFileAppender.appendLog(logFileName, formatAppendLog);
            return true;
        } else {
            logger.info(">>>>>>>>>>> {}", formatAppendLog);
            return false;
        }
    }

    // ---------------------- tool for handleResult ----------------------
    /**
     * handle success
     *
     * @return
     */
    public static boolean handleSuccess(){
        return handleResult(XxlJobContext.HANDLE_CODE_SUCCESS, null);
    }

    /**
     * handle success with log msg
     *
     * @param handleMsg
     * @return
     */
    public static boolean handleSuccess(String handleMsg) {
        return handleResult(XxlJobContext.HANDLE_CODE_SUCCESS, handleMsg);
    }

    /**
     * handle fail
     *
     * @return
     */
    public static boolean handleFail(){
        return handleResult(XxlJobContext.HANDLE_CODE_FAIL, null);
    }

    /**
     * handle fail with log msg
     *
     * @param handleMsg
     * @return
     */
    public static boolean handleFail(String handleMsg) {
        return handleResult(XxlJobContext.HANDLE_CODE_FAIL, handleMsg);
    }

    /**
     * handle timeout
     *
     * @return
     */
    public static boolean handleTimeout(){
        return handleResult(XxlJobContext.HANDLE_CODE_TIMEOUT, null);
    }

    /**
     * handle timeout with log msg
     *
     * @param handleMsg
     * @return
     */
    public static boolean handleTimeout(String handleMsg){
        return handleResult(XxlJobContext.HANDLE_CODE_TIMEOUT, handleMsg);
    }


    /**
     * @param handleCode
     *
     *      200 : success
     *      500 : fail
     *      502 : timeout
     *
     * @param handleMsg
     * @return
     */
    public static boolean handleResult(int handleCode, String handleMsg) {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return false;
        }

        xxlJobContext.setHandleCode(handleCode);
        if (handleMsg != null) {
            xxlJobContext.setHandleMsg(handleMsg);
        }
        return true;
    }

}
