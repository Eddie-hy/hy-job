package com.hy.job.admin.dao;

import com.hy.job.admin.core.model.XxlJobGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface XxlJobGroupDao {

    //找到全部的执行器
    public List<XxlJobGroup> findAll();

    //通过地址类型找到执行器
    public List<XxlJobGroup> findByAddressType(@Param("addressType") int addressType);

    //保存地址执行器
    public int save(XxlJobGroup xxlJobGroup);

    //更新执行器信息
    public int update(XxlJobGroup xxlJobGroup);

    //删除执行器信息
    public int remove(@Param("id") int id);

    //根据id返回执行器对象
    public XxlJobGroup load(@Param("id") int id);

    //返回满足模糊查询条件的结果
    public List<XxlJobGroup> pageList(@Param("offset") int offset,
                                      @Param("pagesize") int pagesize,
                                      @Param("appname") String appname,
                                      @Param("title") String title);

    //返回满足条件的个数
    public int pageListCount(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("appname") String appname,
                             @Param("title") String title);

}
