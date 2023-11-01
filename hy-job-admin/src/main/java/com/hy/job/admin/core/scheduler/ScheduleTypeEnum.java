package com.hy.job.admin.core.scheduler;

import com.hy.job.admin.core.util.I18nUtil;

/**
 * @Author: HY
 * @Date: 2023-10-07-22:06
 * @Description:调度的时间类型
 */
public enum ScheduleTypeEnum {

    //没有指定调度类型
    NONE(I18nUtil.getString("schedule_type_none")),

    /**
     * 根据 cron  调度
     */
    CRON(I18nUtil.getString("schedule_type_cron")),

    /**
     * 表示按照固定的速率（以秒为单位）进行调度
     */
    FIX_RATE(I18nUtil.getString("schedule_type_fix_rate")),

    /**
     * 表示按照固定的延迟时间（以秒为单位），在上一次调度结束后再次进行调度
     */
    /*FIX_DELAY(I18nUtil.getString("schedule_type_fix_delay"))*/;

    private String title;

    ScheduleTypeEnum(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public static ScheduleTypeEnum match(String name, ScheduleTypeEnum defaultItem){
        for (ScheduleTypeEnum item: ScheduleTypeEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return defaultItem;
    }

}
