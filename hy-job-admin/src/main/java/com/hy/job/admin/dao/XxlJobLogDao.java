package com.hy.job.admin.dao;

import com.hy.job.admin.core.model.XxlJobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface XxlJobLogDao {

    // exist jobId not use jobGroup, not exist use jobGroup

    //列出在时间范围内还满足条件的日志
    public List<XxlJobLog> pageList(@Param("offset") int offset,
                                    @Param("pagesize") int pagesize,
                                    @Param("jobGroup") int jobGroup,
                                    @Param("jobId") int jobId,
                                    @Param("triggerTimeStart") Date triggerTimeStart,
                                    @Param("triggerTimeEnd") Date triggerTimeEnd,
                                    @Param("logStatus") int logStatus);


    //统计满足条件的日志条数
    public int pageListCount(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("jobGroup") int jobGroup,
                             @Param("jobId") int jobId,
                             @Param("triggerTimeStart") Date triggerTimeStart,
                             @Param("triggerTimeEnd") Date triggerTimeEnd,
                             @Param("logStatus") int logStatus);


    //根据id查找日志并封装到日志对象中
    public XxlJobLog load(@Param("id") long id);

    //保存日志信息
    public long save(XxlJobLog xxlJobLog);

    //更新触发部分信息
    public int updateTriggerInfo(XxlJobLog xxlJobLog);

    //更新手动部分信息
    public int updateHandleInfo(XxlJobLog xxlJobLog);

    //根据id删除日志信息
    public int delete(@Param("jobId") int jobId);

    public Map<String, Object> findLogReport(@Param("from") Date from,
                                             @Param("to") Date to);

    //选择一批满足特定条件的日志记录的ID，并排除了最近的一些记录
    public List<Long> findClearLogIds(@Param("jobGroup") int jobGroup,
                                      @Param("jobId") int jobId,
                                      @Param("clearBeforeTime") Date clearBeforeTime,
                                      @Param("clearBeforeNum") int clearBeforeNum,
                                      @Param("pagesize") int pagesize);

    //删除列表中的日志
    public int clearLog(@Param("logIds") List<Long> logIds);

    //查找失败日志
    public List<Long> findFailJobLogIds(@Param("pagesize") int pagesize);

    //更新警告状态
    public int updateAlarmStatus(@Param("logId") long logId,
                                 @Param("oldAlarmStatus") int oldAlarmStatus,
                                 @Param("newAlarmStatus") int newAlarmStatus);

    //多表联合查询，查询未成功执行或未处理的作业记录
    public List<Long> findLostJobIds(@Param("losedTime") Date losedTime);



}
