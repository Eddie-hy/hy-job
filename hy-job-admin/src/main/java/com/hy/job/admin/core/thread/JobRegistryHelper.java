package com.hy.job.admin.core.thread;

import com.hy.job.admin.core.conf.XxlJobAdminConfig;
import com.hy.job.admin.core.model.XxlJobGroup;
import com.hy.job.admin.core.model.XxlJobRegistry;
import com.hy.job.core.biz.model.RegistryParam;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.enums.RegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.*;

/**
 * @Author: HY
 * @Date: 2023-10-13-21:38
 * @Description:注册表信息管理
 */
public class JobRegistryHelper {

    private static Logger logger = LoggerFactory.getLogger(JobRegistryHelper.class);

    private static JobRegistryHelper instance = new JobRegistryHelper();

    public static JobRegistryHelper getInstance(){
        return instance;
    }

    private ThreadPoolExecutor registryOrRemoveThreadPool = null;

    //监控线程
    private Thread registryMonitorThread;

    //判断是否需要进行注册表信息维护
    private volatile  boolean toStop = false;

    /*
	1.初始化注册或者删除线程池，主要负责客户端注册或者销毁到xxl_job_registry表
    2.剔除超时注册机器
    3.更新xxl_job_group执行器地址列表
	*/
    public void start(){

        //初始化注册或者删除线程池
        registryOrRemoveThreadPool = new ThreadPoolExecutor(
                2,
                10,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "xxl-job, admin JobRegistryMonitorHelper-registryOrRemoveThreadPool-" + r.hashCode());
                    }
                },

                //注意:这里的拒绝策略就是再次执行...^_^'''
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        r.run();
                        logger.warn(">>>>>>>>>>> xxl-job, registry or remove too fast, match threadpool rejected handler(run now).");
                    }
                });


        //30秒执行一次,维护注册表信息， 判断在线超时时间90s
        registryMonitorThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (!toStop){
                    try {

                        //查询自动注册的数据
                        //这里如果没添加自动注册的数据，则不会进入该方法，然后删除register表中超时注册数据。
                        List<XxlJobGroup> groupList = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().findByAddressType(0);
                        if(groupList != null && !groupList.isEmpty()){
                            // 1):从注册表中删除超时90s的机器,不分是否自动注册
                            List<Integer> ids = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().findDead(RegistryConfig.DEAD_TIMEOUT, new Date());
                            if(ids != null && ids.size() > 0){
                                XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().removeDead(ids);
                            }
                        }

                        // 获取所有在线机器,注册表: 见"xxl_job_registry"表, "执行器" 在进行任务注册时将会周期性维护一条注册记录，
                        // 即机器地址和AppName的绑定关系; "调度中心" 从而可以动态感知每个AppName在线的机器列表;
                        HashMap<String, List<String>> appAddressMap = new HashMap<String, List<String>>();
                        //不分是否自动注册
                        List<XxlJobRegistry> list = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().findAll(RegistryConfig.DEAD_TIMEOUT, new Date());
                        if(list != null){
                            for(XxlJobRegistry item : list){
                                //将注册类型为EXECUTOR的XxlJobRegistry集合改装成appname=>设置触发器的ip地址
                                if(RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())){
                                    //AppName: 每个执行器机器集群的唯一标示, 任务注册以 "执行器" 为最小粒度进行注册; 每个任务通过其绑定的执行器可感知对应的执行器机器列表;
                                    String appname = item.getRegistryKey();
                                    List<String> registryList = appAddressMap.get(appname);
                                    if(registryList == null){
                                        registryList = new ArrayList<String>();
                                    }

                                    if(!registryList.contains(item.getRegistryValue())){
                                        registryList.add(item.getRegistryValue());
                                    }
                                    appAddressMap.put(appname, registryList);
                                }
                            }
                        }

                        //更新xxl_job_group执行器地址列表
                        for(XxlJobGroup group:groupList){
                            List<String> registryList = appAddressMap.get(group.getAppname());
                            String addressListStr = null;   //将所有配置触发器的ip地址，使用,拼接
                            if(registryList != null && !registryList.isEmpty()){
                                //排序
                                Collections.sort(registryList);
                                StringBuilder addressListSB = new StringBuilder();
                                for(String item:registryList){
                                    addressListSB.append(",");
                                }
                                addressListStr = addressListSB.toString();
                                addressListStr = addressListStr.substring(0,addressListStr.length()-1);
                            }
                            group.setAddressList(addressListStr);  //更新了触发器的地址
                            group.setUpdateTime(new Date());  //更新修改时间

                            //将注册表中appname对应的多条ip地址，整成appname-> ips(IP1,IP2,IP3)格式存储xxl_job_group表中，只针对自动注册。
                            XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().update(group);
                        }

                    }catch (Exception e){
                        if (!toStop) {
                            logger.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:{}", e);
                        }
                    }
                    try {
                        //让当前线程休息
                        TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                    }catch (InterruptedException e){
                        if (!toStop) {
                            logger.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:{}", e);
                        }
                    }
                }
                logger.info(">>>>>>>>>>> xxl-job, job registry monitor thread stop");
            }
        });
        registryMonitorThread.setDaemon(true);    //设置为后台线程
        registryMonitorThread.setName("xxl-job, admin JobRegistryMonitorHelper-registryMonitorThread");   //设置名字
        registryMonitorThread.start();    //开始线程
    }

    public void toStop(){
        toStop = true;

        //关闭线程池
        registryOrRemoveThreadPool.shutdownNow();

        //中断监控线程
        registryMonitorThread.interrupt();

        try {
            registryMonitorThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    // ---------------------- helper ----------------------

    //信息注册
    public ReturnT<String> registry(RegistryParam registryParam){

        //判断数据是否有效
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }

        //异步执行
        registryOrRemoveThreadPool.execute(new Runnable() {

            @Override
            public void run() {
               int ret = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().registryUpdate(registryParam.getRegistryGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue(), new Date())  ;
               //如果更新失败
               if(ret < 1){
                   XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().registrySave(registryParam.getRegistryGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue(), new Date());

                   // fresh
                   freshGroupRegistryInfo(registryParam);
               }
            }
        });
        return ReturnT.SUCCESS;
    }

    public ReturnT<String> registryRemove(RegistryParam registryParam){

        //判断是否有效
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }

        //异步执行
        registryOrRemoveThreadPool.execute(new Runnable() {

            @Override
            public void run() {
                int ret =     XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().registryDelete(registryParam.getRegistryGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue());

                if(ret > 0){
                    // fresh
                    freshGroupRegistryInfo(registryParam);
                }
            }
        });
        return ReturnT.SUCCESS;
    }


    private void freshGroupRegistryInfo(RegistryParam registryParam){
        // Under consideration, prevent affecting core tables
    }
}
