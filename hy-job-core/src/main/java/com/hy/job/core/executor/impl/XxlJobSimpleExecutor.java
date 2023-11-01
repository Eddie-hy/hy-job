package com.hy.job.core.executor.impl;

import com.hy.job.core.executor.XxlJobExecutor;
import com.hy.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: HY
 * @Date: 2023-10-23-22:06
 * @Description:简单类型的执行器配置  (无框架的)
 */
public class XxlJobSimpleExecutor extends XxlJobExecutor {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobSimpleExecutor.class);

    private List<Object> xxlJobBeanList = new ArrayList<>();

    public List<Object> getXxlJobBeanList() {return xxlJobBeanList;}

    public void setXxlJobBeanList(List<Object> xxlJobBeanList){this.xxlJobBeanList = xxlJobBeanList;}

    @Override
    public void start(){

        //初始化JobHandler的方法仓库
        initJobHandlerMethodRepository(xxlJobBeanList);

        try {
            super.start();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy(){
        super.destroy();
    }


    /**
     * 动态地初始化任务处理器方法的仓库，以便后续可以执行这些任务
     * @param xxlJobBeanList
     */
    private void initJobHandlerMethodRepository(List<Object> xxlJobBeanList){
        if(xxlJobBeanList == null || xxlJobBeanList.size() == 0){
            return;
        }

        //初始化任务的处理方法
        for(Object bean : xxlJobBeanList){
            //得到所有的声明方法
            Method[] methods = bean.getClass().getDeclaredMethods();
            if(methods.length == 0){
                continue;
            }

            for(Method executeMethod : methods){
                XxlJob xxlJob = executeMethod.getAnnotation(XxlJob.class);

                //注册
                registJobHandler(xxlJob, bean, executeMethod);
            }
        }
    }

}
