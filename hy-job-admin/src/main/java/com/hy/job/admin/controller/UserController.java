package com.hy.job.admin.controller;

import com.hy.job.admin.controller.annotation.PermissionLimit;
import com.hy.job.admin.core.model.XxlJobGroup;
import com.hy.job.admin.core.model.XxlJobUser;
import com.hy.job.admin.core.util.I18nUtil;
import com.hy.job.admin.dao.XxlJobGroupDao;
import com.hy.job.admin.dao.XxlJobUserDao;
import com.hy.job.admin.service.LoginService;
import com.hy.job.core.biz.model.ReturnT;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: HY
 * @Date: 2023-10-05-15:50
 * @Description:用户信息管理页
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Resource
    private XxlJobUserDao xxlJobUserDao;

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;


    //展示用户页面
    @RequestMapping
    @PermissionLimit(adminuser = true)
    public String index(Model model){

        //执行器列表
        List<XxlJobGroup> groupList =xxlJobGroupDao.findAll();
        model.addAttribute("groupList" , groupList);

        return "user/user.index";
    }


    //展示用户列表
    @RequestMapping("/pageList")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        String username, int role) {

        //用户列表与用户数量
        List<XxlJobUser> list = xxlJobUserDao.pageList(start, length, username, role);
        int list_count = xxlJobUserDao.pageListCount(start, length, username, role);

        //清洗密码
        if(list != null && list.size() > 0){
            for(XxlJobUser item : list){
                item.setPassword(null);
            }
        }

        //封装结果
        Map<String , Object> maps = new HashMap<String , Object>();
        maps.put("recordsTotal", list_count);		// 总记录数
        maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
        maps.put("data", list);  					// 分页列表

        return maps;
    }

    //增加用户
    @RequestMapping("/add")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> add(XxlJobUser xxlJobUser) {

        //判断用户名是否有效
        if (!StringUtils.hasText(xxlJobUser.getUsername())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_please_input")+I18nUtil.getString("user_username") );
        }
        xxlJobUser.setUsername(xxlJobUser.getUsername().trim());
        if (!(xxlJobUser.getUsername().length()>=4 && xxlJobUser.getUsername().length()<=20)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
        }

        //判断密码是否有效
        if (!StringUtils.hasText(xxlJobUser.getPassword())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_please_input")+I18nUtil.getString("user_password") );
        }
        xxlJobUser.setPassword(xxlJobUser.getPassword().trim());
        if (!(xxlJobUser.getPassword().length()>=4 && xxlJobUser.getPassword().length()<=20)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
        }

        //对密码进行md5加密
        xxlJobUser.setPassword(DigestUtils.md5DigestAsHex(xxlJobUser.getPassword().getBytes()));

        //检查用户名是否重复
        XxlJobUser existUser = xxlJobUserDao.loadByUserName(xxlJobUser.getUsername());
        if(existUser != null){
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("user_username_repeat") );
        }

        //把新建对象写进数据库
        xxlJobUserDao.save(xxlJobUser);

       return  new ReturnT("用户增加成功！");
    }


    //更新用户信息
    @RequestMapping("/update")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> update(HttpServletRequest request, XxlJobUser xxlJobUser) {

        //判断修改的是否是目前登录的用户
        XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        if(loginUser.getUsername().equals(xxlJobUser.getUsername())){
            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("user_update_loginuser_limit"));
        }

        //判断密码是否有效
        if(StringUtils.hasText(xxlJobUser.getPassword())){
            xxlJobUser.setPassword(xxlJobUser.getPassword().trim());
            if(!(xxlJobUser.getPassword().length()>=4 && xxlJobUser.getPassword().length()<=20)){
                return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
            }
            //对密码进行md5加密
            xxlJobUser.setPassword(DigestUtils.md5DigestAsHex(xxlJobUser.getPassword().getBytes()));
        }else{
            xxlJobUser.setPassword(null);
        }

        //写进数据库
        xxlJobUserDao.update(xxlJobUser);
        return new ReturnT<>("更新用户信息成功！");
    }

    //删除用户
    @RequestMapping("/remove")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> remove(HttpServletRequest request, int id) {

        //判断修改的是否是目前登录的用户
        XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        if(loginUser.getId() == id){
            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("user_update_loginuser_limit"));
        }

        //删除数据库用户
        xxlJobUserDao.delete(id);
       return new ReturnT<>("删除信息成功！");
    }


    //更新当前用户密码
    @RequestMapping("/updatePwd")
    @ResponseBody
    public ReturnT<String> updatePwd(HttpServletRequest request, String password){

        //判断密码是否在有效范围
        if(password == null || password.trim().length() == 0){
            return new ReturnT<>(ReturnT.FAIL.getCode(),"密码不可为空！");
        }
        password = password.trim();
        if (!(password.length()>=4 && password.length()<=20)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
        }

        //对密码进行md5编码
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

        //根据cookie查找是哪个用户
        XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);

        //写入数据库
        XxlJobUser existUser = xxlJobUserDao.loadByUserName(loginUser.getUsername());
        existUser.setPassword(md5Password);
        xxlJobUserDao.update(existUser);

       return new ReturnT<>("更新密码成功！");
    }



}
