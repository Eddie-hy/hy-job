package com.hy.job.admin.core.thread;

import com.hy.job.admin.core.conf.XxlJobAdminConfig;
import com.hy.job.admin.core.cron.CronExpression;
import com.hy.job.admin.core.model.XxlJobInfo;
import com.hy.job.admin.core.scheduler.MisfireStrategyEnum;
import com.hy.job.admin.core.scheduler.ScheduleTypeEnum;
import com.hy.job.admin.core.trigger.TriggerTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: HY
 * @Date: 2023-10-08-22:23
 * @Description:任务调度器核心部分，把任务id加进环表中，然后调用线程运行
 */
public class JobScheduleHelper {

    private static Logger logger = LoggerFactory.getLogger(JobScheduleHelper.class);

    private static JobScheduleHelper instance = new JobScheduleHelper();
    public static JobScheduleHelper getInstance(){
        return instance;
    }

    public static final long PRE_READ_MS = 5000;    // pre read

    private Thread scheduleThread;
    private Thread ringThread;
    private volatile boolean scheduleThreadToStop = false;
    private volatile boolean ringThreadToStop = false;
    private volatile static Map<Integer, List<Integer>> ringData = new ConcurrentHashMap<>();


    public void start(){

        //协调线程
        scheduleThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    //下5秒之后执行一次，等待服务器启动，为了凑到整数秒，且时间不超过五秒
                    TimeUnit.MILLISECONDS.sleep(5000 - System.currentTimeMillis()%1000 );
                } catch (InterruptedException e) {
                    if (!scheduleThreadToStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
                logger.info(">>>>>>>>> init xxl-job admin scheduler success.");

                // pre-read count: treadpool-size * trigger-qps (each trigger cost 50ms, qps = 1000/50 = 20) 每个触发器花费50ms,每个线程单位时间内处理20任务,最多同时处理300*20=6000任务
                int preReadCount = (XxlJobAdminConfig.getAdminConfig().getTriggerPoolFastMax() + XxlJobAdminConfig.getAdminConfig().getTriggerPoolSlowMax()) * 20;

                while (!scheduleThreadToStop){

                    //扫描任务
                    long start = System.currentTimeMillis();

                    Connection conn = null;
                    Boolean connAutoCommit = null;
                    PreparedStatement preparedStatement = null;

                    boolean preReadSuc = true;
                    try {
                        //设置手动提交
                        conn = XxlJobAdminConfig.getAdminConfig().getDataSource().getConnection();
                        connAutoCommit = conn.getAutoCommit();
                        conn.setAutoCommit(false);

                        //获取任务调度锁表内数据信息,加写锁,获得排他锁
                        preparedStatement = conn.prepareStatement(  "select * from xxl_job_lock where lock_name = 'schedule_lock' for update" );
                        preparedStatement.execute();

                        //从任务表预读
                        long nowTime = System.currentTimeMillis(); // //获取当前时间后5秒,同时最多负载的分页数
                        //得到满足执行状态同时下次执行时间在执行范围内的所有任务
                        List<XxlJobInfo> scheduleList = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().scheduleJobQuery(nowTime + PRE_READ_MS, preReadCount);
                        if(scheduleList != null && scheduleList.size() > 0){

                            //放进时间环
                            for(XxlJobInfo jobInfo : scheduleList){

                                //跳过时间环
                                if(nowTime > jobInfo.getTriggerNextTime()+PRE_READ_MS){ //触发器过期时间>5s
                                    //trigger-expire > 5s：pass && make next-trigger-time
                                    logger.warn(">>>>>>>>>>> xxl-job, schedule misfire, jobId = " + jobInfo.getId());

                                    //调度过期匹配
                                    //- 忽略：调度过期后，忽略过期的任务，从当前时间开始重新计算下次触发时间；
                                    //- 立即执行一次：调度过期后，立即执行一次，并从当前时间开始重新计算下次触发时间；
                                    MisfireStrategyEnum misfireStrategyEnum = MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), MisfireStrategyEnum.DO_NOTHING);
                                    if(MisfireStrategyEnum.FIRE_ONCE_NOW == misfireStrategyEnum){
                                        //执行触发器
                                        JobTriggerPoolHelper.trigger(jobInfo.getId(), TriggerTypeEnum.MISFIRE, -1, null, null, null);
                                        logger.debug(">>>>>>>>>>> xxl-job, schedule push trigger : jobId = " + jobInfo.getId() );
                                    }

                                    //更新下次执行时间
                                    refreshNextValidTime(jobInfo, new Date());
                                } else if (nowTime > jobInfo.getTriggerNextTime()) {  //触发器过期时间<5s
                                    // 1、trigger  //执行触发器
                                    JobTriggerPoolHelper.trigger(jobInfo.getId(), TriggerTypeEnum.CRON, -1, null, null, null);
                                    logger.debug(">>>>>>>>>>> xxl-job, schedule push trigger : jobId = " + jobInfo.getId() );

                                    //2.更新下次执行时间
                                    refreshNextValidTime(jobInfo, new Date());

                                    //下次触发时间在当前时间往后5秒范围内
                                    if(jobInfo.getTriggerStatus() == 1 && nowTime + PRE_READ_MS > jobInfo.getTriggerNextTime()){

                                        //获取下次执行时间
                                        int ringSecond = (int)((jobInfo.getTriggerNextTime()/1000)%60);

                                        //放进时间环
                                        pushTimeRing(ringSecond, jobInfo.getId());

                                        //更新下次执行时间
                                        refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));

                                    }
                                }else {
                                    //未来五秒以内执行的所有任务添加到ringData
                                    //得到放上环的时间
                                    int ringSecond = (int)((jobInfo.getTriggerNextTime()/1000)%60);

                                    //把时间戳和任务id放进环里
                                    pushTimeRing(ringSecond, jobInfo.getId());

                                    //更新下次触发时间
                                    refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));

                                }
                            }

                            //更新执行时间和上次执行时间到数据库
                            for(XxlJobInfo jobInfo:scheduleList){
                                XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().scheduleUpdate(jobInfo);
                            }

                        }else {
                            preReadSuc = false;
                        }

                        // tx stop


                    }catch (Exception e){
                        if (!scheduleThreadToStop) {
                            logger.error(">>>>>>>>>>> xxl-job, JobScheduleHelper#scheduleThread error:{}", e);
                        }
                    }finally {

                        //事务提交
                        if (conn != null) {
                            try {
                                conn.commit();
                            } catch (SQLException e) {
                                if (!scheduleThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                            try {
                                conn.setAutoCommit(connAutoCommit);
                            } catch (SQLException e) {
                                if (!scheduleThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                            try {
                                conn.close();
                            } catch (SQLException e) {
                                if (!scheduleThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        }

                        //关闭数据库执行连接
                        if (null != preparedStatement) {
                            try {
                                preparedStatement.close();
                            } catch (SQLException e) {
                                if (!scheduleThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        }
                    }
                    long cost = System.currentTimeMillis()-start;


                    //等待时间
                    if (cost < 1000) {  // scan-overtime, not wait
                        try {
                            // pre-read period: success > scan each second; fail > skip this period;
                            //若执行成功,下一秒继续执行。执行失败或没查询出数据则5秒执行一次。
                            TimeUnit.MILLISECONDS.sleep((preReadSuc?1000:PRE_READ_MS) - System.currentTimeMillis()%1000);
                        } catch (InterruptedException e) {
                            if (!scheduleThreadToStop) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }
                }
                logger.info(">>>>>>>>>>> xxl-job, JobScheduleHelper#scheduleThread stop");
            }
        });
        scheduleThread.setDaemon(true);
        scheduleThread.setName("xxl-job, admin JobScheduleHelper#scheduleThread");
        scheduleThread.start();


        //环线程
        ringThread = new Thread(new Runnable() {

            @Override
            public void run() {

                while (!ringThreadToStop){

                    //单个秒数
                    try{
                        //凑到整秒
                        TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
                    }catch (InterruptedException e){
                        if (!ringThreadToStop) {
                            logger.error(e.getMessage(), e);
                        }
                    }

                    try {
                        //第二秒
                        List<Integer> ringItemData = new ArrayList<>();
                        int nowSecond = Calendar.getInstance().get(Calendar.SECOND); // 避免处理耗时太长，跨过刻度，向前校验一个刻度
                        for (int i = 0; i < 2; i++) {
                            //假设现在为1秒，那么执行任务之后，5秒之后的任务分别会添加到23456下标位置
                            // i=1：（1+60-1）%60=0
                            // i=2：（1+60-2）%60=59
                            List<Integer> tmpData = ringData.remove( (nowSecond+60-i)%60 );
                            if (tmpData != null) {
                                ringItemData.addAll(tmpData);
                            }
                        }

                        //时间环触发
                        logger.debug(">>>>>>>>>>> xxl-job, time-ring beat : " + nowSecond + " = " + Arrays.asList(ringItemData) );
                        if(ringItemData.size() > 0){
                            //执行触发器
                            for(int jobId: ringItemData){
                                //执行触发器
                                JobTriggerPoolHelper.trigger(jobId, TriggerTypeEnum.CRON,-1,null,null,null);
                            }
                            //清理
                            ringItemData.clear();
                        }
                    }catch (Exception e){
                        if (!ringThreadToStop) {
                            logger.error(">>>>>>>>>>> xxl-job, JobScheduleHelper#ringThread error:{}", e);
                        }
                    }
                }
                logger.info(">>>>>>>>>>> xxl-job, JobScheduleHelper#ringThread stop");
            }
        });
        ringThread.setDaemon(true);
        ringThread.setName("xxl-job, admin JobScheduleHelper#ringThread");
        ringThread.start();
    }

    public void toStop(){

        //停止任务表
        scheduleThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);
        }catch (InterruptedException e){
            logger.error(e.getMessage(), e);
        }

        //判断线程是否中止，并发送中断信号
        if (scheduleThread.getState() != Thread.State.TERMINATED){
            // interrupt and wait
            scheduleThread.interrupt();
            try {
                scheduleThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }

        //判断环里是否存在数据
        boolean hasRingData = false;
        if (!ringData.isEmpty()) {
            for (int second : ringData.keySet()) {
                List<Integer> tmpData = ringData.get(second);
                if (tmpData!=null && tmpData.size()>0) {
                    hasRingData = true;
                    break;
                }
            }
        }

        //如果存在数据
        if(hasRingData){
            try {
            TimeUnit.SECONDS.sleep(8);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }

        //停止环数据线程
        ringThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        if (ringThread.getState() != Thread.State.TERMINATED){
            // interrupt and wait
            ringThread.interrupt();
            try {
                ringThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }

        logger.info(">>>>>>>>>>> xxl-job, JobScheduleHelper stop");
    }

    //把任务id放在执行列表中
    private void pushTimeRing(int ringSecond, int jobId){
       //放进异步环
        List<Integer> ringItemData = ringData.get(ringSecond);
        if(ringItemData == null){
            ringItemData = new ArrayList<Integer>();
            ringData.put(ringSecond, ringItemData);
        }
        ringItemData.add(jobId);

        logger.debug(">>>>>>>>>>> xxl-job, schedule push time-ring : " + ringSecond + " = " + Arrays.asList(ringItemData) );
    }


    private void refreshNextValidTime(XxlJobInfo jobInfo, Date fromTime) throws Exception {
        Date nextValidTime = generateNextValidTime(jobInfo, fromTime);
        if(nextValidTime != null){
            jobInfo.setTriggerLastTime(jobInfo.getTriggerNextTime());
            jobInfo.setTriggerNextTime(nextValidTime.getTime());
        }else {
            jobInfo.setTriggerStatus(0);
            jobInfo.setTriggerLastTime(0);
            jobInfo.setTriggerNextTime(0);
            logger.warn(">>>>>>>>>>> xxl-job, refreshNextValidTime fail for job: jobId={}, scheduleType={}, scheduleConf={}",
                    jobInfo.getId(), jobInfo.getScheduleType(), jobInfo.getScheduleConf());
        }
    }

    // ---------------------- tools ----------------------
    //判断任务调度策略并返回不同时间
    public static Date generateNextValidTime(XxlJobInfo jobInfo, Date fromTime) throws Exception {
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        if (ScheduleTypeEnum.CRON == scheduleTypeEnum) {
            Date nextValidTime = new CronExpression(jobInfo.getScheduleConf()).getNextValidTimeAfter(fromTime);
            return nextValidTime;
        } else if (ScheduleTypeEnum.FIX_RATE == scheduleTypeEnum /*|| ScheduleTypeEnum.FIX_DELAY == scheduleTypeEnum*/) {
            return new Date(fromTime.getTime() + Integer.valueOf(jobInfo.getScheduleConf())*1000 );
        }
        return null;
    }


}
