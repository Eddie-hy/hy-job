package com.hy.job.admin.service;

import com.hy.job.admin.core.model.XxlJobInfo;
import com.hy.job.core.biz.model.ReturnT;

import java.util.Date;
import java.util.Map;

/**
 * @Author: HY
 * @Date: 2023-10-04-21:56
 * @Description:页面服务类
 */
public interface XxlJobService {


    /**\
     *得到主页的调度信息
     * @return
     */
    public Map<String, Object> dashboardInfo();


    /**
     *展示主页图标
     * @param startDate
     * @param endDate
     * @return
     */
    ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate);

    /**
     * 任务管理页面的数据展示
     * @param start
     * @param length
     * @param jobGroup
     * @param triggerStatus
     * @param jobDesc
     * @param executorHandler
     * @param author
     * @return
     */
    Map<String, Object> pageList(int start, int length, int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author);

    /**
     * 任务管理页面增加任务
     * @param jobInfo
     * @return
     */
    ReturnT<String> add(XxlJobInfo jobInfo);

    /**
     * remove job
     * 	 *
     * @param id
     * @return
     */
    ReturnT<String> remove(int id);

    ReturnT<String> update(XxlJobInfo jobInfo);

    ReturnT<String> start(int id);

    ReturnT<String> stop(int id);
}
