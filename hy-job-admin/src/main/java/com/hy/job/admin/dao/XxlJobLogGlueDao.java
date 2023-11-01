package com.hy.job.admin.dao;

import com.hy.job.admin.core.model.XxlJobLogGlue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface XxlJobLogGlueDao {

    //保存脚本模型
    public int save(XxlJobLogGlue xxlJobLogGlue);

    //根据id查询脚本模型
    public List<XxlJobLogGlue> findByJobId(@Param("jobId") int jobId);

    //删除等于某个id的旧记录
    public int removeOld(@Param("jobId") int jobId, @Param("limit") int limit);

    //删除等于某个id的所有记录
    public int deleteByJobId(@Param("jobId") int jobId);
}
