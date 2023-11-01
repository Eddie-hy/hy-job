package com.hy.job.executor.service.jobhandler;

import com.hy.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @Author: HY
 * @Date: 2023-10-26-21:22
 * @Description:运行实例
 */
@Component    //放入IOC容器中
public class SampleHyJob {


    private static Logger logger = LoggerFactory.getLogger(SampleHyJob.class);

    @XxlJob("test01")
    public void testApp1(){
        logger.info("我是你爹一号执行成功！执行时间是："+new Date());
        System.out.println("请别乱认爹！");
    }
}
