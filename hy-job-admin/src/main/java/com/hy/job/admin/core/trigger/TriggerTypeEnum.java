package com.hy.job.admin.core.trigger;

import com.hy.job.admin.core.util.I18nUtil;

/**
 * @Author: HY
 * @Date: 2023-10-09-15:14
 * @Description:触发器类型的枚举类
 */
public enum TriggerTypeEnum {
    //MANUAL：手动触发，表示任务是由用户手动触发的。
    MANUAL(I18nUtil.getString("jobconf_trigger_type_manual")),
    //CRON：基于 Cron 表达式的触发，表示任务是按照 Cron 表达式的规定时间触发的。
    CRON(I18nUtil.getString("jobconf_trigger_type_cron")),
    //RETRY：重试触发，表示任务是由于执行失败而进行的重试触发。
    RETRY(I18nUtil.getString("jobconf_trigger_type_retry")),
    //PARENT：父任务触发，表示任务是由其他任务作为父任务触发的。
    PARENT(I18nUtil.getString("jobconf_trigger_type_parent")),
    //API：API 触发，表示任务是通过 API 调用触发的。
    API(I18nUtil.getString("jobconf_trigger_type_api")),
    //MISFIRE：错过触发，表示任务是由于错过了之前的触发时间而进行的触发。
    MISFIRE(I18nUtil.getString("jobconf_trigger_type_misfire"));

    private TriggerTypeEnum(String title){
        this.title = title;
    }
    private String title;
    public String getTitle() {
        return title;
    }
}
