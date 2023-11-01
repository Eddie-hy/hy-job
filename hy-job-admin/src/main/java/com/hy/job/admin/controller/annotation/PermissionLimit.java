package com.hy.job.admin.controller.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author: HY
 * @Date: 2023-10-03-15:38
 * @Description:权限限制
 */
@Target(ElementType.METHOD)     //代表只作用于方法
@Retention(RetentionPolicy.RUNTIME)   //代表在编译时保留
public @interface PermissionLimit {

    /**
     * 登录拦截 (默认拦截)
     */
    boolean limit() default true;

    /**
     * 要求管理员权限
     *
     * @return
     */
    boolean adminuser() default false;
}
