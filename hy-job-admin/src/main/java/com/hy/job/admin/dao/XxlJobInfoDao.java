package com.hy.job.admin.dao;

import com.hy.job.admin.core.model.XxlJobInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface XxlJobInfoDao {

    //选择满足条件的集合
    public List<XxlJobInfo> pageList(@Param("offset") int offset,
                                     @Param("pagesize") int pagesize,
                                     @Param("jobGroup") int jobGroup,
                                     @Param("triggerStatus") int triggerStatus,
                                     @Param("jobDesc") String jobDesc,
                                     @Param("executorHandler") String executorHandler,
                                     @Param("author") String author);

    //统计满足条件的数量
    public int pageListCount(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("jobGroup") int jobGroup,
                             @Param("triggerStatus") int triggerStatus,
                             @Param("jobDesc") String jobDesc,
                             @Param("executorHandler") String executorHandler,
                             @Param("author") String author);

    //保存任务表信息
    public int save(XxlJobInfo info);

    //根据id把信息注入任务表对象
    public XxlJobInfo loadById(@Param("id") int id);

    //更新任务表信息
    public int update(XxlJobInfo xxlJobInfo);

    //根据id删除任务表信息
    public int delete(@Param("id") long id);

    //通过任务表分组查询任务表信息
    public List<XxlJobInfo> getJobsByGroup(@Param("jobGroup") int jobGroup);

    //统计所有任务表的数量
    public int findAllCount();

    //查询调度状态为已调度且下次触发时间小于等于给定值的任务信息列
    public List<XxlJobInfo> scheduleJobQuery(@Param("maxNextTime") long maxNextTime, @Param("pagesize") int pagesize );

    //更新任务表信息
    public int scheduleUpdate(XxlJobInfo xxlJobInfo);

}
