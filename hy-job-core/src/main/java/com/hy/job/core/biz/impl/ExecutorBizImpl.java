package com.hy.job.core.biz.impl;

import com.hy.job.core.biz.ExecutorBiz;
import com.hy.job.core.biz.model.*;
import com.hy.job.core.enums.ExecutorBlockStrategyEnum;
import com.hy.job.core.executor.XxlJobExecutor;
import com.hy.job.core.glue.GlueFactory;
import com.hy.job.core.glue.GlueTypeEnum;
import com.hy.job.core.handler.IJobHandler;
import com.hy.job.core.handler.impl.GlueJobHandler;
import com.hy.job.core.handler.impl.ScriptJobHandler;
import com.hy.job.core.log.XxlJobFileAppender;
import com.hy.job.core.thread.JobThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * @Author: HY
 * @Date: 2023-10-06-11:25
 * @Description:把执行参数放进队列，等待执行
 */
public class ExecutorBizImpl implements ExecutorBiz {

    private static Logger logger = LoggerFactory.getLogger(ExecutorBizImpl.class);

    @Override
    public ReturnT<String> beat() {
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam) {

        //判断标志，判断这个编号的执行器是否在执行
        boolean isRunningOrHasQueue  = false;
        JobThread jobThread = XxlJobExecutor.loadJobThread(idleBeatParam.getJobId());

        //如果执行器在执行
        if(jobThread != null && jobThread.isRunningOrHasQueue()){
            isRunningOrHasQueue = true;
        }

        //如果正在执行
        if(isRunningOrHasQueue){
            return new ReturnT<String>(ReturnT.FAIL_CODE, "job thread is running or has trigger queue.");
        }
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        //加载任务信息和任务处理器
        JobThread jobThread = XxlJobExecutor.loadJobThread(triggerParam.getJobId());
        IJobHandler jobHandler = jobThread != null?jobThread.getHandler():null;
        String removeOldReason = null;

        //判断任务信息和任务处理器是否有效
        //判断glue模式下使用的语言
        GlueTypeEnum glueTypeEnum = GlueTypeEnum.match(triggerParam.getGlueType());
        if(GlueTypeEnum.BEAN == glueTypeEnum){

            //新建任务处理器
            IJobHandler newJobHandler = XxlJobExecutor.loadJobHandler(triggerParam.getExecutorHandler());

            //如果老的任务处理器不等于新的
            if(jobThread != null && jobHandler != newJobHandler ){
                //解放老的处理器
                removeOldReason = "change jobhandler or glue type, and terminate the old job thread.";

                jobThread = null;
                jobHandler = null;
            }

            //让新的处理器有效
            if(jobHandler == null){
                jobHandler = newJobHandler;
                if(jobHandler == null){
                    return new ReturnT<String>(ReturnT.FAIL_CODE, "job handler [" + triggerParam.getExecutorHandler() + "] not found.");
                }
            }

        } else if (GlueTypeEnum.GLUE_GROOVY == glueTypeEnum) {

            //判断老的任务处理器是否还有效
            if (jobThread != null &&
                    !(jobThread.getHandler() instanceof GlueJobHandler
                            && ((GlueJobHandler) jobThread.getHandler()).getGlueUpdatetime()==triggerParam.getGlueUpdatetime() )) {
                // change handler or gluesource updated, need kill old thread
                removeOldReason = "change job source or glue type, and terminate the old job thread.";

                jobThread = null;
                jobHandler = null;
            }

            //判断处理器是否有效
            if(jobHandler == null){
                try {
                    IJobHandler originJobHandler = GlueFactory.getInstance().loadNewInstance(triggerParam.getGlueSource());
                    jobHandler = new GlueJobHandler(originJobHandler, triggerParam.getGlueUpdatetime());
                }catch (Exception e){
                    logger.error(e.getMessage(), e);
                    return new ReturnT<String>(ReturnT.FAIL_CODE, e.getMessage());
                }
            }
        } else if (glueTypeEnum != null && glueTypeEnum.isScript()) {

            //判断老的任务处理器是否有效
            if (jobThread != null &&
                    !(jobThread.getHandler() instanceof ScriptJobHandler
                            && ((ScriptJobHandler) jobThread.getHandler()).getGlueUpdatetime()==triggerParam.getGlueUpdatetime() )) {
                // change script or gluesource updated, need kill old thread
                removeOldReason = "change job source or glue type, and terminate the old job thread.";

                jobThread = null;
                jobHandler = null;
            }

            //判断处理器是否有效
            if(jobHandler == null){
                jobHandler = new ScriptJobHandler(triggerParam.getJobId(), triggerParam.getGlueUpdatetime(), triggerParam.getGlueSource(), GlueTypeEnum.match(triggerParam.getGlueType()));
            }
        }else {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "glueType[" + triggerParam.getGlueType() + "] is not valid.");
        }

        //处理阻塞策略
        if(jobThread != null){
            ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(triggerParam.getExecutorBlockStrategy(), null);
            if(ExecutorBlockStrategyEnum.DISCARD_LATER == blockStrategy){
                //当正在跑的时候丢弃
                if (jobThread.isRunningOrHasQueue()) {
                    return new ReturnT<String>(ReturnT.FAIL_CODE, "block strategy effect："+ExecutorBlockStrategyEnum.DISCARD_LATER.getTitle());
                }
            } else if (ExecutorBlockStrategyEnum.COVER_EARLY == blockStrategy) {
                //杀死正在跑的任务线程
                if (jobThread.isRunningOrHasQueue()) {
                    removeOldReason = "block strategy effect：" + ExecutorBlockStrategyEnum.COVER_EARLY.getTitle();

                    jobThread = null;
                }
            }else {
                // just queue trigger
            }
        }

        //替换线程（新的或者是无效的）
        if(jobThread == null){
            jobThread = XxlJobExecutor.registJobThread(triggerParam.getJobId(), jobHandler, removeOldReason);
        }

        //把数据推送到队列
        ReturnT<String> pushResult = jobThread.pushTriggerQueue(triggerParam);
        return pushResult;
    }

    @Override
    public ReturnT<String> kill(KillParam killParam) {
        //杀死处理线程，或者创建新的一个
        JobThread jobThread =  XxlJobExecutor.loadJobThread(killParam.getJobId());
        if(jobThread != null){
            XxlJobExecutor.removeJobThread(killParam.getJobId(),"scheduling center kill job.");
            return ReturnT.SUCCESS;
        }

        return new ReturnT<String>(ReturnT.SUCCESS_CODE, "job thread already killed.");
    }

    //获取日志文件的内容
    @Override
    public ReturnT<LogResult> log(LogParam logParam) {

        //日志文件名：logPath/yyyy-MM-dd/9999.log 得到日志文件名
        String logFileName = XxlJobFileAppender.makeLogFileName(new Date(logParam.getLogDateTim()),logParam.getLogId());

        //在指定路径读取日志文件
        LogResult logResult = XxlJobFileAppender.readLog(logFileName,logParam.getFromLineNum());
        return new ReturnT<LogResult>(logResult);
    }

}
