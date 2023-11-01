package com.hy.job.admin.core.route.strategy;

import com.hy.job.admin.core.route.ExecutorRouter;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.biz.model.TriggerParam;

import java.util.List;
import java.util.Random;

/**
 * @Author: HY
 * @Date: 2023-10-07-14:04
 * @Description:随机策略
 */
public class ExecutorRouteRandom extends ExecutorRouter {

    //定义一个随机变量
    private static Random localRandom = new Random();

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        //从地址长度范围内选择一个随机数
        String address = addressList.get(localRandom.nextInt(addressList.size()));
        return new ReturnT<String>(address);
    }
}
