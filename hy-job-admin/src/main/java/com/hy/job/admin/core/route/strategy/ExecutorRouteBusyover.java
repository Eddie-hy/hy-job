package com.hy.job.admin.core.route.strategy;

import com.hy.job.admin.core.route.ExecutorRouter;
import com.hy.job.admin.core.scheduler.XxlJobScheduler;
import com.hy.job.admin.core.util.I18nUtil;
import com.hy.job.core.biz.ExecutorBiz;
import com.hy.job.core.biz.model.IdleBeatParam;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.biz.model.TriggerParam;

import java.util.List;

/**
 * @Author: HY
 * @Date: 2023-10-07-19:34
 * @Description:繁忙转移策略
 */
public class ExecutorRouteBusyover extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        StringBuffer idleBeatResultSB = new StringBuffer();
        for(String address : addressList){
            //心跳检测
            ReturnT<String> idleBeatResult = null;
            try{
                ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
                idleBeatResult = executorBiz.idleBeat((new IdleBeatParam(triggerParam.getJobId())));
            }catch (Exception e){
                logger.error(e.getMessage(), e);
                idleBeatResult = new ReturnT<String>(ReturnT.FAIL_CODE, ""+e );
            }
            idleBeatResultSB.append( (idleBeatResultSB.length()>0)?"<br><br>":"")
                    .append(I18nUtil.getString("jobconf_idleBeat") + "：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(idleBeatResult.getCode())
                    .append("<br>msg：").append(idleBeatResult.getMsg());

            //心跳测试成功，有空闲的执行器
            if(idleBeatResult.getCode() == ReturnT.SUCCESS_CODE){
                idleBeatResult.setMsg(idleBeatResultSB.toString());
                idleBeatResult.setContent(address);
                return idleBeatResult;
            }
        }
        return new ReturnT<String>(ReturnT.FAIL_CODE, idleBeatResultSB.toString());
    }
}
