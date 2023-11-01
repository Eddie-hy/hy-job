package com.hy.job.admin.controller.interceptor;

import com.hy.job.admin.core.util.FtlUtil;
import com.hy.job.admin.core.util.I18nUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/**
 * @Author: HY
 * @Date: 2023-10-04-14:33
 * @Description: 拦截器
 */
@Component
public class CookieInterceptor implements AsyncHandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView)throws Exception{

        //判断视图和cookie不为空
        if(modelAndView != null && request.getCookies() != null && request.getCookies().length > 0){
            HashMap<String, Cookie> cookieMap = new HashMap<String,Cookie>();
            for(Cookie ck : request.getCookies()){
                cookieMap.put(ck.getName(), ck);
            }
            modelAndView.addObject("cookieMap",cookieMap);
        }

        //添加国际化方法到视图对象中
        if(modelAndView != null){
            modelAndView.addObject("I18nUtil", FtlUtil.generateStaticModel(I18nUtil.class.getName()));
        }
    }
}
