package com.hy.job.core.handler.annotation;

import java.lang.annotation.*;

/**
 * @Author: HY
 * @Date: 2023-10-16-22:21
 * @Description:给xxljob注解类
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface XxlJob {

    /**
     * jobhandler name
     */
    String value();

    /**
     * init handler, invoked when JobThread init
     */
    String init() default "";

    /**
     * destroy handler, invoked when JobThread destroy
     */
    String destroy() default "";
}
