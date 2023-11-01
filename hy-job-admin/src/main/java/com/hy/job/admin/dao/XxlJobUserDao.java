package com.hy.job.admin.dao;

import com.hy.job.admin.core.model.XxlJobUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface XxlJobUserDao {

    //查询数据库对应的用户信息
    public List<XxlJobUser> pageList(@Param("offset") int offset,
                                     @Param("pagesize") int pagesize,
                                     @Param("username") String username,
                                     @Param("role") int role);

    //查询满足条件的用户数量
    public int pageListCount(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("username") String username,
                             @Param("role") int role);


    //只通过用户名查询
    public XxlJobUser loadByUserName(@Param("username") String username);

    //保存用户进数据库
    public int save(XxlJobUser xxlJobUser);

    //根据id更改用户信息
    public int update(XxlJobUser xxlJobUser);

    //根据id删除用户
    public int delete(@Param("id") int id);

}
