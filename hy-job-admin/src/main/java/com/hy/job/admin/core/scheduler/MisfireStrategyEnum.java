package com.hy.job.admin.core.scheduler;

import com.hy.job.admin.core.util.I18nUtil;

/**
 * @Author: HY
 * @Date: 2023-10-07-22:10
 * @Description:调度过期策略
 */
public enum MisfireStrategyEnum {

    /**
     * 什么都不做
     */
    DO_NOTHING(I18nUtil.getString("misfire_strategy_do_nothing")),

    /**
     * 立即再执行一次
     */
    FIRE_ONCE_NOW(I18nUtil.getString("misfire_strategy_fire_once_now"));

    private String title;

    MisfireStrategyEnum(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public static MisfireStrategyEnum match(String name, MisfireStrategyEnum defaultItem){
        for (MisfireStrategyEnum item: MisfireStrategyEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return defaultItem;
    }
}
