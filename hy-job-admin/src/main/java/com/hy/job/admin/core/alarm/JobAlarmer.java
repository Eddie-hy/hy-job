package com.hy.job.admin.core.alarm;

import com.hy.job.admin.core.model.XxlJobInfo;
import com.hy.job.admin.core.model.XxlJobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: HY
 * @Date: 2023-10-10-9:47
 * @Description:告警类,遍历容器中的告警对象，然后查看告警对象是否有用
 */
@Component
public class JobAlarmer implements ApplicationContextAware, InitializingBean {

    private static Logger logger  = LoggerFactory.getLogger(JobAlarmer.class);

    private ApplicationContext applicationContext;

    private List<JobAlarm> jobAlarmList;

    @Override
    public void afterPropertiesSet() throws Exception {
        //从Spring容器中得到所有类型为 JobAlarm 的 Bean
        Map<String , JobAlarm> serviceBeanMap = applicationContext.getBeansOfType(JobAlarm.class);
        if(serviceBeanMap != null && serviceBeanMap.size() > 0){
            jobAlarmList = new ArrayList<JobAlarm>(serviceBeanMap.values());
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 这段代码用于触发作业执行时的告警操作，遍历所有的告警器执行告警任务，只要有一个失败，就返回 false，否则返回 true。
     */
    public boolean alarm(XxlJobInfo info, XxlJobLog jobLog){

        boolean result = false;
        if(jobAlarmList != null && jobAlarmList.size() > 0){
            result = true;
            for(JobAlarm alarm: jobAlarmList){
                boolean resultItem = false;
                try {
                    resultItem = alarm.doAlarm(info, jobLog);
                }catch (Exception e){
                    logger.error(e.getMessage(),e);
                }
                if(!resultItem){
                    result = false;
                }
            }
        }
        return result;
    }

}