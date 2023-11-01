package com.hy.job.admin.core.scheduler;

import com.hy.job.admin.core.conf.XxlJobAdminConfig;
import com.hy.job.admin.core.thread.*;
import com.hy.job.admin.core.util.I18nUtil;
import com.hy.job.core.biz.ExecutorBiz;
import com.hy.job.core.biz.client.ExecutorBizClient;
import com.hy.job.core.enums.ExecutorBlockStrategyEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: HY
 * @Date: 2023-10-06-11:29
 * @Description:xxl-job任务调度器
 */
public class XxlJobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(XxlJobScheduler.class);

    //初始化xxljob调度器
    public void init() throws Exception{
        //init i18n  初始国际化的功能或者配置
        initI18n();

        //初始化触发器线程池
        JobTriggerPoolHelper.toStart();

        //30秒执行一次,维护注册表信息， 判断在线超时时间90s
        JobRegistryHelper.getInstance().start();

        //运行事变监视器,主要失败发送邮箱,重试触发器
        JobFailMonitorHelper.getInstance().start();

        //将丢失主机信息调度日志更改状态
        JobCompleteHelper.getInstance().start();

        //统计一些失败成功报表
        JobLogReportHelper.getInstance().start();

        //执行调度器
        JobScheduleHelper.getInstance().start();

        logger.info(">>>>>>>>> init xxl-job admin success.");
    }

    public void destroy()throws Exception{

        //停止调度器
        JobScheduleHelper.getInstance().toStop();

        //log文件报告结束
        JobLogReportHelper.getInstance().toStop();

        //停止丢失监控
        JobCompleteHelper.getInstance().toStop();

        //停止失败监控
        JobFailMonitorHelper.getInstance().ToStop();

        //停止注册信息
        JobRegistryHelper.getInstance().toStop();

        //停止初始化线程池
        JobTriggerPoolHelper.toStop();
    }


    // ---------------------- I18n ----------------------
    //根据id得到配置文件中得国际化配置，把不同的任务策略重命名
    private void initI18n(){
        for(ExecutorBlockStrategyEnum item : ExecutorBlockStrategyEnum.values()){
            item.setTitle(I18nUtil.getString("jobconf_block_".concat(item.name())));
        }
    }


    // ---------------------- executor-client ----------------------
    private static ConcurrentMap<String , ExecutorBiz> executorBizRepository = new ConcurrentHashMap<String, ExecutorBiz>();

    //根据地址绑定执行器
    public static ExecutorBiz getExecutorBiz(String address) throws Exception {

        //判断地址是否有效
        if(address == null && address.trim().length() > 0){
            return null;
        }

        //从本地缓存中加载执行器
        address = address.trim();
        ExecutorBiz executorBiz = executorBizRepository.get(address);
        if(executorBiz != null){
            return executorBiz;
        }

        //如果本地没有，就重新创一个
        executorBiz = new ExecutorBizClient(address, XxlJobAdminConfig.getAdminConfig().getAccessToken());
        executorBizRepository.put(address, executorBiz);


        return executorBiz;
    }
}
