package com.hy.job.admin.core.route.strategy;

import com.hy.job.admin.core.route.ExecutorRouter;
import com.hy.job.admin.core.scheduler.XxlJobScheduler;
import com.hy.job.admin.core.util.I18nUtil;
import com.hy.job.core.biz.ExecutorBiz;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.biz.model.TriggerParam;

import java.util.List;

/**
 * @Author: HY
 * @Date: 2023-10-07-16:46
 * @Description:故障转移，寻找有用的执行器
 */
public class ExecutorRouteFailover extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {

        StringBuffer beatResultSB = new StringBuffer();

        for(String address : addressList){
            //心跳测试（检测服务是否正在进行)
            ReturnT<String> beatResult = null;
            try{
                ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
                beatResult = executorBiz.beat();
            }catch (Exception e){
                logger.error(e.getMessage(), e);
                beatResult = new ReturnT<String>(ReturnT.FAIL_CODE, ""+e );
            }

            beatResultSB.append( (beatResultSB.length()>0)?"<br><br>":"")
                    .append(I18nUtil.getString("jobconf_beat") + "：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(beatResult.getCode())
                    .append("<br>msg：").append(beatResult.getMsg());

            //如果心跳测试成功  也就是分到了空的执行器
            if(beatResult.getCode() == ReturnT.SUCCESS_CODE){

                beatResult.setMsg(beatResultSB.toString());
                beatResult.setContent(address);
                return beatResult;
            }
        }
        return new ReturnT<String>(ReturnT.FAIL_CODE,beatResultSB.toString());
    }

}
