package com.hy.job.admin.controller.interceptor;

import com.hy.job.admin.controller.annotation.PermissionLimit;
import com.hy.job.admin.core.model.XxlJobUser;
import com.hy.job.admin.core.util.I18nUtil;
import com.hy.job.admin.service.LoginService;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author: HY
 * @Date: 2023-10-04-15:59
 * @Description:权限拦截
 */

@Component
public class PermissionInterceptor implements AsyncHandlerInterceptor {

    @Resource
    private LoginService loginService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)throws Exception{

        //为了确保在拦截器中只对真正的控制器方法进行特定的操作，而不会影响其他非控制器方法的请求处理(就是controller层的路径方法)
        if(!(handler instanceof HandlerMethod)){
            return true;      //可以执行下一个拦截器
        }

        //判断是否登录  还有就是是否具有管理员权限
        boolean needLogin = true;
        boolean needAdminuser = false;
        HandlerMethod method = (HandlerMethod) handler;
        PermissionLimit permission = method.getMethodAnnotation(PermissionLimit.class);

        //通过注解去得到权限
        if(permission != null){
            needLogin = permission.limit();
            needAdminuser = permission.adminuser();
        }

        //检查用户是否登录，没有就跳转登录页面
        if(needLogin){
            XxlJobUser loginUser = loginService.ifLogin(request,response);

            if(loginUser == null){
                response.setStatus(302);
                response.setHeader("location",request.getContextPath()+"/toLogin");
                return false;
            }

            if(needAdminuser && loginUser.getRole() !=1){
                throw new RuntimeException(I18nUtil.getString("system_permission_limit"));
            }

            request.setAttribute(LoginService.LOGIN_IDENTITY_KEY, loginUser);
        }

        return true;   //进入下一个拦截器
    }

}
