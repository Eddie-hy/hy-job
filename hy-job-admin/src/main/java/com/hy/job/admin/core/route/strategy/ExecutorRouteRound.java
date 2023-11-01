package com.hy.job.admin.core.route.strategy;

import com.hy.job.admin.core.route.ExecutorRouter;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.biz.model.TriggerParam;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: HY
 * @Date: 2023-10-07-10:00
 * @Description:轮询策略
 * 用于将任务按照轮询方式分配给执行器节点执行。这种策略适用于均衡地分配任务，确保每个执行器都有机会执行任务。
 */
public class ExecutorRouteRound extends ExecutorRouter {

    private static ConcurrentMap<Integer, AtomicInteger> routeCountEachJob =new ConcurrentHashMap<>();

    private static long CACHE_VALID_TIME = 0;

    private static int count(int jobId){
        //清理缓存信息并重新设置时间为一天后   记录系统毫秒时间当作标准判断单位
        if(System.currentTimeMillis() > CACHE_VALID_TIME){
            routeCountEachJob.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000*60*60*24;
        }

        AtomicInteger count = routeCountEachJob.get(jobId);
        if(count == null || count.get() > 1000000){
            //初始化时候主动随机一次  在100以内随机选取
            count = new AtomicInteger(new Random().nextInt(100));
        }else{
            count.addAndGet(1);
        }
        //绑定任务id与随机数
        routeCountEachJob.put(jobId, count);
        return count.get();
    }


    /**
     * 轮询策略，使得每次可以在上一次执行任务加1
     * @param triggerParam
     * @param addressList
     * @return
     */
    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        String address = addressList.get(count(triggerParam.getJobId())%addressList.size());
        return new ReturnT<String>(address);
    }
}
