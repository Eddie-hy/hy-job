package com.hy.job.admin.controller.resolver;

import com.hy.job.admin.core.exception.XxlJobException;
import com.hy.job.admin.core.util.JacksonUtil;
import com.hy.job.core.biz.model.ReturnT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Author: HY
 * @Date: 2023-10-09-14:25
 * @Description:全局异常处理器，根据情况返回相应的错误页面或错误JSON响应
 */
public class WebExceptionResolver implements HandlerExceptionResolver {

    private static transient Logger logger = LoggerFactory.getLogger(WebExceptionResolver.class);

    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response, Object handler, Exception ex) {

        //判断是否XxlJobException异常,不是就记录错误日志
        if(!(ex instanceof XxlJobException)){
            logger.error("WebExceptionResolver:{}", ex);
        }

        //判断所返回的数据是否是json数据
        boolean isJson = false;
        if(handler instanceof HandlerMethod){
            HandlerMethod method = (HandlerMethod) handler;
            ResponseBody responseBody = method.getMethodAnnotation(ResponseBody.class);
            if(responseBody != null){
                isJson = true;
            }
        }

        //报错转换
        ReturnT<String> errorResult = new ReturnT<String>(ReturnT.FAIL_CODE, ex.toString().replaceAll("\n", "<br/>"));

        //报错反馈
        ModelAndView mv = new ModelAndView();
        if(isJson){
            try {
                //它会将异常信息包装成一个 ReturnT 对象，并将该对象转换成JSON格式响应给客户端
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().print(JacksonUtil.writeValueAsString(errorResult));
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
            return mv;
        }else{
            //将异常信息存储到一个名为 "exceptionMsg" 的Model属性中，然后指定一个错误页面（"/common/common.exception"）作为视图返回给客户端。
            mv.addObject("exceptionMsg", errorResult.getMsg());
            mv.setViewName("/common/common.exception");
            return mv;
        }
    }
}
