package com.hy.job.admin.core.thread;

import com.hy.job.admin.core.conf.XxlJobAdminConfig;
import com.hy.job.admin.core.trigger.TriggerTypeEnum;
import com.hy.job.admin.core.trigger.XxlJobTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: HY
 * @Date: 2023-10-09-15:18
 * @Description:线程池初始化与调用类
 */
public class JobTriggerPoolHelper {
    private static Logger logger = LoggerFactory.getLogger(JobTriggerPoolHelper.class);


    // ---------------------- trigger pool ----------------------

    // 创建快慢线程池
    private ThreadPoolExecutor fastTriggerPool = null;
    private ThreadPoolExecutor slowTriggerPool = null;

    /**
     * 初始化线程池
     */
    public void start(){
        //最大200线程，最多处理1000任务
        fastTriggerPool = new ThreadPoolExecutor(
                10,
                XxlJobAdminConfig.getAdminConfig().getTriggerPoolFastMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(1000),
                new ThreadFactory() {
                    @Override   //创建线程
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "xxl-job, admin JobTriggerPoolHelper-fastTriggerPool-" + r.hashCode());
                    }
                });

        //最大100线程，最多处理2000任务
        //一分钟内超时10次，则采用慢触发器执行
        slowTriggerPool = new ThreadPoolExecutor(
                10,
                XxlJobAdminConfig.getAdminConfig().getTriggerPoolSlowMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "xxl-job, admin JobTriggerPoolHelper-slowTriggerPool-" + r.hashCode());
                    }
                });
    }

    /**
     *用于停止线程池的执行。在这里，有两个线程池：fastTriggerPool 和 slowTriggerPool，
     * 分别用于处理不同类型的任务。当需要停止这两个线程池时，就可以调用 stop 方法。
     * shutdownNow()尝试停止所有正在执行的任务，并返回等待执行的任务列表。这样可以迅速停止线程池，但可能会导致一些任务被中断或未完成。
     *
     * 这个方法的目的是优雅地关闭线程池，以确保所有任务都被正确处理。在应用程序关闭或重启时，通常会调用这样的方法来清理资源并终止线程池的运行。
     */
    public void stop() {
        //triggerPool.shutdown();
        fastTriggerPool.shutdownNow();
        slowTriggerPool.shutdownNow();
        logger.info(">>>>>>>>> xxl-job trigger thread pool shutdown success.");
    }

    private volatile long minTim = System.currentTimeMillis()/60000;     // ms > min

    //超时Map,每分钟清空一次
    private volatile ConcurrentMap<Integer, AtomicInteger> jobTimeoutCountMap = new ConcurrentHashMap<>();

    /**
     * 根据触发器判断使用快线程池还是慢线程池
     */
    public void addTrigger(final int jobId,
                           final TriggerTypeEnum triggerType,
                           final int failRetryCount,
                           final String executorShardingParam,
                           final String executorParam,
                           final String addressList) {

        //默认使用快线程池，如果一分钟内超时十次，就使用慢线程池
        ThreadPoolExecutor triggerPool = fastTriggerPool;
        AtomicInteger jobTimeoutCount = jobTimeoutCountMap.get(jobId);
        if(jobTimeoutCount != null && jobTimeoutCount.get() > 10){    //一个任务一分钟超时十次换到慢线程池
            triggerPool = slowTriggerPool;
        }

        //执行
        triggerPool.execute(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();

                try {
                    //使用触发器
                    XxlJobTrigger.trigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);
                }catch (Exception e){
                    logger.error(e.getMessage(), e);
                }finally {
                    //检查任务时间超时，判断是否在同一分钟内
                    long minTim_now = System.currentTimeMillis()/60000;
                    if(minTim != minTim_now){
                        minTim = minTim_now;
                        jobTimeoutCountMap.clear();
                    }

                    //增加超时计数映射
                    long cost  = System.currentTimeMillis()-start;
                    if(cost > 500){    //超时阈值 500 毫秒
                        AtomicInteger timeoutCount = jobTimeoutCountMap.putIfAbsent(jobId,new AtomicInteger(1));
                        //如果之前这种任务超时就存在,增加1
                        if(timeoutCount != null){
                            timeoutCount.incrementAndGet();
                        }
                    }

                }

            }
        });
    }



    // ---------------------- helper ----------------------
    private static JobTriggerPoolHelper helper = new JobTriggerPoolHelper();

    public static void toStart(){
        helper.start();
    }

    public static void toStop(){
        helper.stop();
    }

    /**
     * @param jobId
     * @param triggerType
     * @param failRetryCount
     * 			>=0: use this param
     * 			<0: use param from job info config
     * @param executorShardingParam
     * @param executorParam
     *          null: use job param
     *          not null: cover job param
     */
    public static void trigger(int jobId, TriggerTypeEnum triggerType, int failRetryCount, String executorShardingParam, String executorParam, String addressList) {
        helper.addTrigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);
    }

}
