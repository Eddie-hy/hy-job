package com.hy.job.core.glue.impl;

import com.hy.job.core.executor.impl.XxlJobSpringExecutor;
import com.hy.job.core.glue.GlueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotationUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @Author: HY
 * @Date: 2023-10-24-10:51
 * @Description:用于构建spring glue连接工厂
 */
public class SpringGlueFactory extends GlueFactory {

    private static Logger logger = LoggerFactory.getLogger(SpringGlueFactory.class);

    /**
     * 实现 Spring 依赖注入的功能，以确保对象实例能够访问所需的依赖服务
     * @param instance
     */
    @Override
    public void injectService(Object instance){
        if(instance == null){
            return;
        }

        if(XxlJobSpringExecutor.getApplicationContext() == null){
            return;
        }

        //获取所有的声明字段
        Field[] fields = instance.getClass().getDeclaredFields();
        for(Field field : fields){
            if(Modifier.isStatic(field.getModifiers())){
                continue;
            }

            Object fieldBean = null;

            if(AnnotationUtils.getAnnotation(field, Resource.class) != null){
                try {
                    Resource resource  = AnnotationUtils.getAnnotation(field, Resource.class);
                    if(resource.name() != null && resource.name().length() > 0){
                        fieldBean = XxlJobSpringExecutor.getApplicationContext().getBean(resource.name());
                    }else {
                        fieldBean = XxlJobSpringExecutor.getApplicationContext().getBean(field.getName());
                    }
                }catch (Exception e){
                }
                if(fieldBean == null){
                    fieldBean = XxlJobSpringExecutor.getApplicationContext().getBean(field.getType());
                }
            }else if (AnnotationUtils.getAnnotation(field, Autowired.class) != null){
                Qualifier qualifier = AnnotationUtils.getAnnotation(field, Qualifier.class);
                if(qualifier != null && qualifier.value() != null && qualifier.value().length() > 0){
                    fieldBean = XxlJobSpringExecutor.getApplicationContext().getBean(qualifier.value());
                }else {
                    fieldBean = XxlJobSpringExecutor.getApplicationContext().getBean(field.getType());
                }
            }

            if(fieldBean != null){
                field.setAccessible(true);
                try {
                    field.set(instance, fieldBean);
                }catch (IllegalArgumentException e){
                    logger.error(e.getMessage() , e);
                }catch (IllegalAccessException e){
                    logger.error(e.getMessage() , e);
                }
            }
        }

        }
}
