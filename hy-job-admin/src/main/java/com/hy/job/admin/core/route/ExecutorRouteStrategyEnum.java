package com.hy.job.admin.core.route;

import com.hy.job.admin.core.route.strategy.*;
import com.hy.job.admin.core.util.I18nUtil;

/**
 * @Author: HY
 * @Date: 2023-10-07-9:40
 * @Description:路由枚举策略
 * 这个枚举类的目的是提供一组可选的执行器路由策略，以便在任务调度时选择适当的策略来路由任务执行器。根据不同的业务需求，可以选择不同的策略来分配任务给执行器。
 */
public enum ExecutorRouteStrategyEnum {

    FIRST(I18nUtil.getString("jobconf_route_first"), new ExecutorRouteFirst()),    //首选策略
    LAST(I18nUtil.getString("jobconf_route_last"), new ExecutorRouteLast()),     //最后策略
    ROUND(I18nUtil.getString("jobconf_route_round"), new ExecutorRouteRound()),    //轮询策略
    RANDOM(I18nUtil.getString("jobconf_route_random"), new ExecutorRouteRandom()),   //随机策略
    CONSISTENT_HASH(I18nUtil.getString("jobconf_route_consistenthash"), new ExecutorRouteConsistentHash()),   //一致性哈希策略
    LEAST_FREQUENTLY_USED(I18nUtil.getString("jobconf_route_lfu"), new ExecutorRouteLFU()),    //最不经常使用策略(通过使用次数来鉴别)
    LEAST_RECENTLY_USED(I18nUtil.getString("jobconf_route_lru"), new ExecutorRouteLRU()),     //最近最少使用策略（通过构建链表来鉴别）
    FAILOVER(I18nUtil.getString("jobconf_route_failover"), new ExecutorRouteFailover()),      //故障转移策略
    BUSYOVER(I18nUtil.getString("jobconf_route_busyover"), new ExecutorRouteBusyover()),      //繁忙转移策略
    SHARDING_BROADCAST(I18nUtil.getString("jobconf_route_shard"), null);        //分片广播策略

    ExecutorRouteStrategyEnum(String title, ExecutorRouter router) {
        this.title = title;
        this.router = router;
    }

    private String title;
    private ExecutorRouter router;

    public String getTitle() {
        return title;
    }
    public ExecutorRouter getRouter() {
        return router;
    }

    public static ExecutorRouteStrategyEnum match(String name, ExecutorRouteStrategyEnum defaultItem){
        if (name != null) {
            for (ExecutorRouteStrategyEnum item: ExecutorRouteStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }
}
