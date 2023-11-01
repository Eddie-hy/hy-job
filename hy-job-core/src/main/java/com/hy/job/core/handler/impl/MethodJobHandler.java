package com.hy.job.core.handler.impl;

import com.hy.job.core.handler.IJobHandler;

import java.lang.reflect.Method;

/**
 * @Author: HY
 * @Date: 2023-10-23-21:49
 * @Description:注解方法调用处理
 */
public class MethodJobHandler extends IJobHandler {

    private final Object target;

    private final Method method;

    private Method initMethod;

    private Method destroyMethod;


    public MethodJobHandler(Object target, Method method, Method initMethod, Method destroyMethod) {
        this.target = target;
        this.method = method;
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }


    /**
     * 执行方法体中的函数，并传递参数
     * @throws Exception
     */
    @Override
    public void execute() throws Exception {
        //这行代码的作用是获得 method 方法的参数类型数组，即该方法的参数类型列表
        Class<?>[] paramTypes = method.getParameterTypes();
        if(paramTypes.length > 0){
            method.invoke(target, new Object[paramTypes.length]);     //方法参数不能是原始参数
        }else {
            method.invoke(target);
        }
    }

    @Override
    public void init() throws Exception{
        if(initMethod != null){
            initMethod.invoke(target);
        }
    }

    @Override
    public void destroy() throws  Exception{
        if(destroyMethod != null){
            destroyMethod.invoke(target);
        }
    }

    @Override
    public String toString() {
        return super.toString()+"["+ target.getClass() + "#" + method.getName() +"]";
    }
}
