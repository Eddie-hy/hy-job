package com.hy.job.admin.dao;

import com.hy.job.admin.core.model.XxlJobRegistry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface XxlJobRegistryDao {

    //寻找注册信息过期的任务
    public List<Integer> findDead(@Param("timeout") int timeout,
                                  @Param("nowTime") Date nowTime);

    //删除在列表中的注册信息
    public int removeDead(@Param("ids") List<Integer> ids);


    //获取未过期的任务注册信息
    public List<XxlJobRegistry> findAll(@Param("timeout") int timeout,
                                        @Param("nowTime") Date nowTime);

    //更新注册信息
    public int registryUpdate(@Param("registryGroup") String registryGroup,
                              @Param("registryKey") String registryKey,
                              @Param("registryValue") String registryValue,
                              @Param("updateTime") Date updateTime);


    //保存增加的注册信息
    public int registrySave(@Param("registryGroup") String registryGroup,
                            @Param("registryKey") String registryKey,
                            @Param("registryValue") String registryValue,
                            @Param("updateTime") Date updateTime);


    //删除注册信息
    public int registryDelete(@Param("registryGroup") String registryGroup,
                              @Param("registryKey") String registryKey,
                              @Param("registryValue") String registryValue);


}
