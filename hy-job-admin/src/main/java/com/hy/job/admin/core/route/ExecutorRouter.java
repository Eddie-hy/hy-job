package com.hy.job.admin.core.route;

import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.biz.model.TriggerParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @Author: HY
 * @Date: 2023-10-07-9:43
 * @Description:执行器路由父类
 */
public abstract class ExecutorRouter {

    protected static Logger logger = LoggerFactory.getLogger(ExecutorRouter.class);

    /**
     * route address
     *
     * @param addressList
     * @return  ReturnT.content=address
     */
    public abstract ReturnT<String> route(TriggerParam triggerParam, List<String> addressList);

}
