package com.hy.job.admin.controller;


import com.hy.job.admin.controller.annotation.PermissionLimit;
import com.hy.job.admin.service.LoginService;
import com.hy.job.admin.service.XxlJobService;
import com.hy.job.core.biz.model.ReturnT;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Controller
public class IndexController {

    @Resource
    private XxlJobService xxlJobService;

    @Resource
    private LoginService loginService;

    //主页的展示
    @RequestMapping("/")
    public String index(Model model){

        Map<String, Object> dashboardMap = xxlJobService.dashboardInfo();
        model.addAllAttributes(dashboardMap);

        return "index";
    }

    @RequestMapping("/chartInfo")
    @ResponseBody
    public ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate){
        ReturnT<Map<String, Object>> chartInfo = xxlJobService.chartInfo(startDate , endDate);
        return chartInfo;
    }


    //登录页面
    @RequestMapping("/toLogin")
    @PermissionLimit(limit = false)
    public ModelAndView toLogin(HttpServletRequest request,HttpServletResponse response,ModelAndView modelAndView){
        if(loginService.ifLogin(request, response) != null){
            //重定向到根目录
            modelAndView.setView(new RedirectView("/",true,false));
            return modelAndView;
        }
        return new ModelAndView("login");
    }

    //登录页面发送判断登录逻辑
    @RequestMapping(value="login", method= RequestMethod.POST)
    @ResponseBody
    @PermissionLimit(limit=false)
    public ReturnT<String> loginDo(HttpServletRequest request, HttpServletResponse response, String userName, String password, String ifRemember){

        //ifRemember参数是用户自行选择是否要记住账号密码
        boolean ifRem = (ifRemember!=null && ifRemember.trim().length()>0 && "on".equals(ifRemember))?true:false;
        return loginService.login(request, response, userName, password, ifRem);
    }

    //退出操作
    @RequestMapping(value="logout", method=RequestMethod.POST)
    @ResponseBody
    @PermissionLimit(limit=false)
    public ReturnT<String> logout(HttpServletRequest request, HttpServletResponse response){
        return loginService.logout(request, response);
    }



    @RequestMapping("/help")
    public String help(){
        return "help";
    }


    //规范日期格式
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }

}
