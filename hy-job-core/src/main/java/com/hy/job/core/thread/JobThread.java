package com.hy.job.core.thread;

import com.hy.job.core.biz.model.HandleCallbackParam;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.biz.model.TriggerParam;
import com.hy.job.core.context.XxlJobContext;
import com.hy.job.core.context.XxlJobHelper;
import com.hy.job.core.executor.XxlJobExecutor;
import com.hy.job.core.handler.IJobHandler;
import com.hy.job.core.log.XxlJobFileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @Author: HY
 * @Date: 2023-10-07-20:08
 * @Description:处理线程
 */
public class JobThread extends Thread{

    private static Logger logger = LoggerFactory.getLogger(JobThread.class);

    private int jobId;

    private IJobHandler handler;

    private LinkedBlockingQueue<TriggerParam> triggerQueue;    //阻塞队列

    private Set<Long> triggerLogIdSet;    //避免对相同的 TRIGGER_LOG_ID 重复触发

    private volatile boolean toStop = false;

    private String stopReason;

    private boolean running = false;   //如果正在跑任务

    private int idleTimes = 0;    //空闲时间

    public JobThread(int jobId, IJobHandler handler) {
        this.jobId = jobId;
        this.handler = handler;
        this.triggerQueue = new LinkedBlockingQueue<TriggerParam>();
        this.triggerLogIdSet = Collections.synchronizedSet(new HashSet<Long>());

        // assign job thread name
        this.setName("xxl-job, JobThread-"+jobId+"-"+System.currentTimeMillis());
    }

    public IJobHandler getHandler() {
        return handler;
    }

    /**
     * 添加执行参数进队列和集合
     * @param triggerParam
     * @return
     */
    public ReturnT<String> pushTriggerQueue(TriggerParam triggerParam){
        //避免重复
        if(triggerLogIdSet.contains(triggerParam.getLogId())){
            logger.info(">>>>>>>>>>> repeate trigger job, logId:{}", triggerParam.getLogId());
            return new ReturnT<String>(ReturnT.FAIL_CODE, "repeate trigger job, logId:" + triggerParam.getLogId());
        }

        triggerLogIdSet.add(triggerParam.getLogId());
        triggerQueue.add(triggerParam);

        return ReturnT.SUCCESS;
    }

    /**
     * 中止线程任务
     *
     * @param stopReason
     */
    public void toStop(String stopReason) {
        /**
         * Thread.interrupt只支持终止线程的阻塞状态(wait、join、sleep)，
         * 在阻塞出抛出InterruptedException异常,但是并不会终止运行的线程本身；
         * 所以需要注意，此处彻底销毁本线程，需要通过共享变量方式；
         */
        this.toStop = true;
        this.stopReason = stopReason;
    }


    /**
     * 判断是否有正在运行的程序
     * @return
     */
    public boolean isRunningOrHasQueue() {
        return running || triggerQueue.size()>0;
    }


    @Override
    public void run(){

        //初始化
        try {
            handler.init();
        }catch (Throwable e){
            logger.error(e.getMessage() ,e);
        }

        //执行
        while (!toStop){
            running = false;
            idleTimes++;

            TriggerParam triggerParam = null;
            try {
                //为了避免线程阻塞，调用poll方法，会等待三秒如果队列种没有参数
                triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);
                if(triggerParam != null){
                    running = true;
                    idleTimes = 0;
                    triggerLogIdSet.remove(triggerParam.getLogId());

                    //日志文件名 "logPath/yyyy-MM-dd/9999.log"
                    String logFileName = XxlJobFileAppender.makeLogFileName(new Date(triggerParam.getLogDateTime()),triggerParam.getLogId());
                    XxlJobContext xxlJobContext = new XxlJobContext(
                            triggerParam.getJobId(),
                            triggerParam.getExecutorParams(),
                            logFileName,
                            triggerParam.getBroadcastIndex(),
                            triggerParam.getBroadcastTotal());

                    //初始化任务内容
                    XxlJobContext.setXxlJobContext(xxlJobContext);

                    //执行
                    XxlJobHelper.log("<br>----------- xxl-job job execute start -----------<br>----------- Param:" + xxlJobContext.getJobParam());

                    //超时处理
                    if(triggerParam.getExecutorTimeout() > 0){
                        //限制超时时间
                        Thread futureThread = null;
                        try {
                            FutureTask<Boolean> futureTask = new FutureTask<Boolean>(new Callable<Boolean>() {
                                @Override
                                public Boolean call() throws Exception {

                                    //初始化工作内容
                                    XxlJobContext.setXxlJobContext(xxlJobContext);

                                    handler.execute();
                                    return true;
                                }
                            });
                            futureThread = new Thread(futureTask);
                            futureThread.start();
                            Boolean tempResult = futureTask.get(triggerParam.getExecutorTimeout(), TimeUnit.SECONDS);
                        }catch (TimeoutException e){

                            XxlJobHelper.log("<br>----------- xxl-job job execute timeout");
                            XxlJobHelper.log(e);

                            // handle result
                            XxlJobHelper.handleTimeout("job execute timeout ");
                        }finally {
                            futureThread.interrupt();
                        }
                    }else {
                        //执行即可
                        handler.execute();
                    }

                    //有效的执行处理数据
                    // valid execute handle data
                    if (XxlJobContext.getXxlJobContext().getHandleCode() <= 0) {
                        XxlJobHelper.handleFail("job handle result lost.");
                    } else {
                        String tempHandleMsg = XxlJobContext.getXxlJobContext().getHandleMsg();
                        tempHandleMsg = (tempHandleMsg!=null&&tempHandleMsg.length()>50000)
                                ?tempHandleMsg.substring(0, 50000).concat("...")
                                :tempHandleMsg;
                        XxlJobContext.getXxlJobContext().setHandleMsg(tempHandleMsg);
                    }
                    XxlJobHelper.log("<br>----------- xxl-job job execute end(finish) -----------<br>----------- Result: handleCode="
                            + XxlJobContext.getXxlJobContext().getHandleCode()
                            + ", handleMsg = "
                            + XxlJobContext.getXxlJobContext().getHandleMsg()
                    );
                }else {
                    if(idleTimes > 30){
                        if(triggerQueue.size() == 0){
                            XxlJobExecutor.removeJobThread(jobId, "excutor idel times over limit.");
                        }
                    }
                }
            }catch (Throwable e){
                if (toStop) {
                    XxlJobHelper.log("<br>----------- JobThread toStop, stopReason:" + stopReason);
                }

                // handle result
                StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                String errorMsg = stringWriter.toString();

                XxlJobHelper.handleFail(errorMsg);

                XxlJobHelper.log("<br>----------- JobThread Exception:" + errorMsg + "<br>----------- xxl-job job execute end(error) -----------");
            }finally {
                //检查参数是否为空
                if(triggerParam != null){
                    //判断是否正常中止
                    if(!toStop){
                        // commonm
                        TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                                triggerParam.getLogId(),
                                triggerParam.getLogDateTime(),
                                XxlJobContext.getXxlJobContext().getHandleCode(),
                                XxlJobContext.getXxlJobContext().getHandleMsg() )
                        );
                    }else {
                        //是被异常中止的
                        TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                                triggerParam.getLogId(),
                                triggerParam.getLogDateTime(),
                                XxlJobContext.HANDLE_CODE_FAIL,
                                stopReason + " [job running, killed]" )
                        );
                    }
                }
            }
        }

        //这段代码用于处理等待执行的触发请求，即使它们没有实际执行，也会生成相应的回调信息，并将其传递给回调处理线程，以便进行回调处理
        while (triggerQueue != null && triggerQueue.size() > 0){
            TriggerParam triggerParam = triggerQueue.poll();
            if(triggerParam != null){
                //回调信息中写入线程中止
                TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                        triggerParam.getLogId(),
                        triggerParam.getLogDateTime(),
                        XxlJobContext.HANDLE_CODE_FAIL,
                        stopReason + " [job not executed, in the job queue, killed.]")
                );
            }
        }

        //释放资源
        try {
            handler.destroy();
        }catch (Throwable e){
            logger.error(e.getMessage() , e);
        }
        logger.info(">>>>>>>>>>> xxl-job JobThread stoped, hashCode:{}", Thread.currentThread());

    }
}
