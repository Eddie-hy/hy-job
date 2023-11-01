package com.hy.job.core.handler;

/**
 * @Author: HY
 * @Date: 2023-10-07-20:13
 * @Description:处理类的抽象类
 */
public abstract class IJobHandler {


    /**
     *当执行器接收到调度请求时，将调用这个方法来执行作业
     * @throws Exception
     */
    public abstract void execute()throws Exception;

    /**
     *提供了一个机会来执行一些初始化操作，例如加载配置或准备资源
     * @throws Exception
     */
    public void init()throws Exception{

    }

    /**
     *它提供了一个机会来执行一些清理操作，例如释放资源或关闭连接
     * @throws Exception
     */
    public void destroy()throws Exception{

    }
}
