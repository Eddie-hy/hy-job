package com.hy.job.admin.core.thread;

import com.hy.job.admin.core.conf.XxlJobAdminConfig;
import com.hy.job.admin.core.model.XxlJobInfo;
import com.hy.job.admin.core.model.XxlJobLog;
import com.hy.job.admin.core.trigger.TriggerTypeEnum;
import com.hy.job.admin.core.util.I18nUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author: HY
 * @Date: 2023-10-14-13:35
 * @Description:监控失败日志重新与运行失败任务
 */
public class JobFailMonitorHelper {
    private static Logger logger = LoggerFactory.getLogger(JobFailMonitorHelper.class);

    private static JobFailMonitorHelper instance = new JobFailMonitorHelper();

    public static JobFailMonitorHelper getInstance(){
        return instance;
    }

    // ---------------------- monitor ----------------------
    private Thread monitorThread;

    private volatile boolean toStop = false;

    public void start(){
        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {

                //监视器
                while (!toStop){
                    try {
                        //将注册表中appname对应的多条ip地址，整成appname-> ips(IP1,IP2,IP3)格式存储xxl_job_group表中，只针对自动注册。
                        List<Long> failLogIds = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().findFailJobLogIds(1000);
                        if(failLogIds != null && !failLogIds.isEmpty()){//1:执行触发器成功,返回值失败.2:触发器失败
                            for(long failLogId : failLogIds){

                                //加锁，乐观修改alarm_status=-1  以数据库的键值为锁
                                int lockRet = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateAlarmStatus(failLogId, 0, -1);
                                if (lockRet < 1) {
                                    continue;
                                }
                                //得到日志信息和任务信息
                                XxlJobLog log = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().load(failLogId);
                                XxlJobInfo info = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(log.getJobId());

                                //失败重试监视器
                                if (log.getExecutorFailRetryCount() > 0) {//若可重试次数>0,则再次执行触发器
                                    JobTriggerPoolHelper.trigger(log.getJobId(), TriggerTypeEnum.RETRY, (log.getExecutorFailRetryCount()-1), log.getExecutorShardingParam(), log.getExecutorParam(), null);
                                    String retryMsg = "<br><br><span style=\"color:#F39C12;\" > >>>>>>>>>>>"+ I18nUtil.getString("jobconf_trigger_type_retry") +"<<<<<<<<<<< </span><br>";
                                    log.setTriggerMsg(log.getTriggerMsg() + retryMsg);
                                    XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateTriggerInfo(log);
                                }

                                //fail alarm monitor 失败警告监视器
                                int newAlarmStatus = 0;		// 告警状态：0-默认、-1=锁定状态、1-无需告警、2-告警成功、3-告警失败
                                if(info != null){
                                    //判断发送邮件是否成功
                                    boolean alarmResult = XxlJobAdminConfig.getAdminConfig().getJobAlarmer().alarm(info,log);
                                    newAlarmStatus = alarmResult?2:3;
                                }else {//没设置报警邮箱，则更改状态为不需要告警
                                    newAlarmStatus = 1;
                                }
                                //释放锁
                                XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateAlarmStatus(failLogId, -1, newAlarmStatus);
                            }
                        }
                    }catch (Exception e){
                        if (!toStop) {
                            logger.error(">>>>>>>>>>> xxl-job, job fail monitor thread error:{}", e);
                        }
                    }
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
                logger.info(">>>>>>>>>>> xxl-job, job fail monitor thread stop");
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.setName("xxl-job, admin JobFailMonitorHelper");
        monitorThread.start();
    }

    public void ToStop(){
        toStop = true;
        //中断然后等待
        monitorThread.interrupt();
        try {
            monitorThread.join();
        }catch (InterruptedException e){
            logger.error(e.getMessage(), e);
        }
    }

}
