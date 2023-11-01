package com.hy.job.admin.dao;

import com.hy.job.admin.core.model.XxlJobLogReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface XxlJobLogReportDao {

    //保存汇报表
    public int save(XxlJobLogReport xxlJobLogReport);

    //更新汇报表
    public int update(XxlJobLogReport xxlJobLogReport);

    //根据某个时间段的日志
    public List<XxlJobLogReport> queryLogReport(@Param("triggerDayFrom") Date triggerDayFrom,
                                                @Param("triggerDayTo") Date triggerDayTo);

    //获取运行统计信息的总和，包括运行次数、成功次数和失败次数
    public XxlJobLogReport queryLogReportTotal();


}
