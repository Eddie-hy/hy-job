package com.hy.job.admin.core.route.strategy;

import com.hy.job.admin.core.route.ExecutorRouter;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.biz.model.TriggerParam;

import java.util.List;

/**
 * @Author: HY
 * @Date: 2023-10-07-9:47
 * @Description:首选策略
 */
public class ExecutorRouteFirst extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new ReturnT<String>(addressList.get(0));
    }
}
