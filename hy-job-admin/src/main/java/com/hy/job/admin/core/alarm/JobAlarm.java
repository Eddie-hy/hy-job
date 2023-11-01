package com.hy.job.admin.core.alarm;

import com.hy.job.admin.core.model.XxlJobInfo;
import com.hy.job.admin.core.model.XxlJobLog;

/**
 * @Author: HY
 * @Date: 2023-10-10-9:51
 * @Description:
 */
public interface JobAlarm {

    /**
     * 任务警告
     * @param info
     * @param jobLog
     * @return
     */
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog);
}
