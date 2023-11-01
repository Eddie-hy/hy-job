package com.hy.job.core.thread;

import com.hy.job.core.biz.AdminBiz;
import com.hy.job.core.biz.model.HandleCallbackParam;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.context.XxlJobContext;
import com.hy.job.core.context.XxlJobHelper;
import com.hy.job.core.enums.RegistryConfig;
import com.hy.job.core.executor.XxlJobExecutor;
import com.hy.job.core.log.XxlJobFileAppender;
import com.hy.job.core.util.FileUtil;
import com.hy.job.core.util.JdkSerializeTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @Author: HY
 * @Date: 2023-10-21-15:28
 * @Description:触发反馈处理线程
 */
public class TriggerCallbackThread {

    private static Logger logger = LoggerFactory.getLogger(TriggerCallbackThread.class);

    private static TriggerCallbackThread instance = new TriggerCallbackThread();

    public static TriggerCallbackThread getInstance(){return instance;}

    /**
     * 任务结果反馈队列
     */
    private LinkedBlockingQueue<HandleCallbackParam> callBackQueue = new LinkedBlockingQueue<HandleCallbackParam>();

    public static void pushCallBack(HandleCallbackParam callback){
        getInstance().callBackQueue.add(callback);
        logger.debug(">>>>>>>>>>> xxl-job, push callback request, logId:{}", callback.getLogId());
    }

    /**
     * 反馈线程
     */
    private Thread triggerCallbackThread;
    private Thread triggerRetryCallbackThread;
    private volatile boolean toStop = false;

    public void start(){

        //判断执行列表是否有东西
        if (XxlJobExecutor.getAdminBizList() == null) {
            logger.warn(">>>>>>>>>>> xxl-job, executor callback config fail, adminAddresses is null.");
            return;
        }

        //反馈
        triggerCallbackThread = new Thread(new Runnable() {

            @Override
            public void run() {

                while (!toStop){
                    try {
                        //从反馈队列调取一个参数
                        HandleCallbackParam callback = getInstance().callBackQueue.take();
                        if(callback != null){

                            //回调执行参数集合
                            List<HandleCallbackParam> callbackParamList = new ArrayList<HandleCallbackParam>();
                            //将队列所有参数都放在集合中
                            int drainToNum = getInstance().callBackQueue.drainTo(callbackParamList);
                            callbackParamList.add(callback);

                            //反馈信息
                            if(callbackParamList != null && callbackParamList.size() > 0){
                                doCallback(callbackParamList);
                            }
                        }
                    }catch (Exception e){
                        if(!toStop){
                            logger.error(e.getMessage(),e);
                        }
                    }
                }

                //获取剩余元素并再次执行反馈
                try {
                    //反馈执行参数集合
                    List<HandleCallbackParam> callbackParamList = new ArrayList<HandleCallbackParam>();
                    //将队列所有参数都放在集合中
                    int drainToNum = getInstance().callBackQueue.drainTo(callbackParamList);
                    //反馈，错误重试
                    if(callbackParamList != null && callbackParamList.size() > 0){
                        doCallback(callbackParamList);
                    }
                }catch (Exception e){
                    if(!toStop){
                        logger.error(e.getMessage(),e);
                    }
                }
                logger.info(">>>>>>>>>>> xxl-job, executor callback thread destroy.");
            }
        });
        triggerCallbackThread.setDaemon(true);
        triggerCallbackThread.setName("xxl-job, executor TriggerCallbackThread");
        triggerCallbackThread.start();

        //重试
        triggerRetryCallbackThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (!toStop){
                    try {
                        retryFailCallbackFile();
                    }catch (Exception e){
                        if(!toStop){
                            logger.error(e.getMessage(), e);
                        }
                    }

                    try {
                        TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                    }catch (InterruptedException e) {
                         if(!toStop){
                             logger.error(e.getMessage(),e);
                         }
                    }
                }
                logger.info(">>>>>>>>>>> xxl-job, executor retry callback thread destroy.");
            }
        });
        triggerRetryCallbackThread.setDaemon(true);
        triggerRetryCallbackThread.start();
    }


    public void toStop(){

        toStop = true;
        //停止回调，打断然后等待
        if(triggerCallbackThread != null){
            triggerCallbackThread.interrupt();
            try {
                triggerCallbackThread.join();
            }catch (InterruptedException e){
                logger.error(e.getMessage(), e);
            }
        }

        //停止重试，中断然后等待线程运行完
        if(triggerRetryCallbackThread != null){
            triggerRetryCallbackThread.interrupt();
            try {
                triggerRetryCallbackThread.join();
            }catch (InterruptedException e){
                logger.error(e.getMessage() , e);
            }
        }
    }


    /**
     * 任务执行反馈
     */
    private void doCallback(List<HandleCallbackParam> callbackParamList){
        boolean callbackRet = false;

        //回调，失败重试
        for(AdminBiz adminBiz: XxlJobExecutor.getAdminBizList()){
            try {
                ReturnT<String> callbackResult = adminBiz.callback(callbackParamList);
                if (callbackResult!=null && ReturnT.SUCCESS_CODE == callbackResult.getCode()) {
                    callbackLog(callbackParamList, "<br>----------- xxl-job job callback finish.");
                    callbackRet = true;
                    break;
                } else {
                    callbackLog(callbackParamList, "<br>----------- xxl-job job callback fail, callbackResult:" + callbackResult);
                }
            }catch (Exception e){
                callbackLog(callbackParamList, "<br>----------- xxl-job job callback error, errorMsg:" + e.getMessage());
            }
        }
        if(!callbackRet){
            appendFailCallbackFile(callbackParamList);
        }
    }


    /**
     *回调日志记录
     */
    private void callbackLog(List<HandleCallbackParam> callbackParamList, String logContent){
        for (HandleCallbackParam callbackParam: callbackParamList) {
            String logFileName = XxlJobFileAppender.makeLogFileName(new Date(callbackParam.getLogDateTim()), callbackParam.getLogId());
            XxlJobContext.setXxlJobContext(new XxlJobContext(
                    -1,
                    null,
                    logFileName,
                    -1,
                    -1));
            XxlJobHelper.log(logContent);
        }
    }



    // ---------------------- fail-callback file ----------------------
    private static String failCallbackFilePath = XxlJobFileAppender.getLogPath().concat(File.separator).concat("callbacklog").concat(File.separator);
    private static String failCallbackFileName = failCallbackFilePath.concat("xxl-job-callback-{x}").concat(".log");

    /**
     * 将回调失败的参数列表写入文件
     * @param callbackParamList
     */
    private void appendFailCallbackFile(List<HandleCallbackParam> callbackParamList){

        //判断参数是否有效
        if(callbackParamList == null || callbackParamList.size() == 0){
            return;
        }

        //序列化参数
        byte[] callbackParamList_bytes = JdkSerializeTool.serialize(callbackParamList);

        //创建文件避免重命名
        File callbackLogFile = new File(failCallbackFileName.replace("{x}", String.valueOf(System.currentTimeMillis())));
        if(callbackLogFile.exists()){
            for(int i = 0; i < 100; i++){
                callbackLogFile = new File(failCallbackFileName.replace("{x}", String.valueOf(System.currentTimeMillis()).concat("-").concat(String.valueOf(i)) ));
                if (!callbackLogFile.exists()) {
                    break;
                }
            }
        }
        FileUtil.writeFileContent(callbackLogFile, callbackParamList_bytes);
    }

    /**
     *重新尝试之前失败的参数列表，从本地文件读取
     */
    private void retryFailCallbackFile(){

        //判断路径
        File callbackLogPath = new File(failCallbackFilePath);
        if(!callbackLogPath.exists()){
            return;
        }
        if(callbackLogPath.isFile()){
            callbackLogPath.delete();
        }
        if(!(callbackLogPath.isDirectory() && callbackLogPath.list() != null && callbackLogPath.list().length > 0)){
            return;
        }

        //加载清理文件唉，重试
        for(File callbackLogFile : callbackLogPath.listFiles()){
            byte[] callbackParamList_bytes = FileUtil.readFileContent(callbackLogFile);

            //避免空文件夹
            if(callbackParamList_bytes == null || callbackParamList_bytes.length < 1){
                callbackLogFile.delete();
                continue;
            }

            //反序列化
            List<HandleCallbackParam> callbackParamList = (List<HandleCallbackParam>) JdkSerializeTool.deserialize(callbackParamList_bytes, List.class);

            callbackLogFile.delete();
            doCallback(callbackParamList);
        }
    }


}
