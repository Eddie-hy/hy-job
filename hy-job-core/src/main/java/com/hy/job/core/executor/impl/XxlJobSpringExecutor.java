package com.hy.job.core.executor.impl;

import com.hy.job.core.executor.XxlJobExecutor;
import com.hy.job.core.glue.GlueFactory;
import com.hy.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @Author: HY
 * @Date: 2023-10-23-22:06
 * @Description:关于Spring的执行器类
 */
public class XxlJobSpringExecutor extends XxlJobExecutor implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(XxlJobSpringExecutor.class);

    /**
     * 这段代码是在 Spring 单例 Bean 实例化之后执行的初始化操作，主要是为了初始化任务处理器和启动任务执行器。
     */
    @Override
    public void afterSingletonsInstantiated() {

        //初始化注册方法
        initJobHandlerMethodRepository(applicationContext);

        //允许在不重启应用程序的情况下进行作业代码的更改和更新
        //刷新作业源代码
        GlueFactory.refreshInstance(1);

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
     * 从 Spring 应用上下文中扫描并注册带有 @XxlJob 注解的方法作为任务处理器，为后续的任务调度提供支持
     * @param applicationContext
     */
    private void initJobHandlerMethodRepository(ApplicationContext applicationContext) {
        if(applicationContext == null){
            return;
        }

        //初始化方法的job handler
        //得到Bean的名字
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);

        for(String beanDefinitionName :beanDefinitionNames){

            //得到bean
            Object bean = null;
            Lazy onBean = applicationContext.findAnnotationOnBean(beanDefinitionName, Lazy.class);
            //如果包含lazy注解跳过
            if (onBean!=null){
                logger.debug("xxl-job annotation scan, skip @Lazy Bean:{}", beanDefinitionName);
                continue;
            }else {
                bean = applicationContext.getBean(beanDefinitionName);
            }

            //寻找xxljob注解的方法
            Map<Method, XxlJob> annotatedMethods = null;
            try {
                //寻找哪些方法用了xxljob注解
                annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(),
                        new MethodIntrospector.MetadataLookup<XxlJob>() {
                            @Override
                            public XxlJob inspect(Method method) {
                                return AnnotatedElementUtils.findMergedAnnotation(method, XxlJob.class);
                            }
                        });
            }catch (Throwable ex){
                logger.error("xxl-job method-jobhandler resolve error for bean[" + beanDefinitionName + "].", ex);
            }
            if(annotatedMethods == null || annotatedMethods.isEmpty()){
                continue;
            }

            //遍历方法，然后注册方法
            for (Map.Entry<Method, XxlJob> methodXxlJobEntry : annotatedMethods.entrySet()) {
                Method executeMethod = methodXxlJobEntry.getKey();
                XxlJob xxlJob = methodXxlJobEntry.getValue();
                // regist
                registJobHandler(xxlJob, bean, executeMethod);
            }
        }
    }

    // ---------------------- applicationContext ----------------------
    private static ApplicationContext applicationContext;

    /**
     * 保存和提供 Spring 应用上下文的引用，以便在类内部或其他地方使用 Spring 容器的功能和资源
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        XxlJobSpringExecutor.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext(){
        return applicationContext;
    }
}
