package com.hy.job.admin.core.alarm.impl;

import com.hy.job.admin.core.alarm.JobAlarm;
import com.hy.job.admin.core.conf.XxlJobAdminConfig;
import com.hy.job.admin.core.model.XxlJobGroup;
import com.hy.job.admin.core.model.XxlJobInfo;
import com.hy.job.admin.core.model.XxlJobLog;
import com.hy.job.admin.core.util.I18nUtil;
import com.hy.job.core.biz.model.ReturnT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @Author: HY
 * @Date: 2023-10-10-9:55
 * @Description:邮件发送实现类
 */
public class EmailJobAlarm implements JobAlarm {

    private static Logger logger = LoggerFactory.getLogger(EmailJobAlarm.class);


    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {
        //邮件成功发送标识
        boolean alarmResult = true;

        //发送监控告警邮件
        if(info != null && info.getAlarmEmail() != null && info.getAlarmEmail().trim().length() > 0){

            //告警内容
            String alarmContent = "Alarm Job LogId=" + jobLog.getId();
            if (jobLog.getTriggerCode() != ReturnT.SUCCESS_CODE) {
                alarmContent += "<br>TriggerMsg=<br>" + jobLog.getTriggerMsg();
            }
            if (jobLog.getHandleCode()>0 && jobLog.getHandleCode() != ReturnT.SUCCESS_CODE) {
                alarmContent += "<br>HandleCode=" + jobLog.getHandleMsg();
            }

            //邮件头
            XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().load(Integer.valueOf(info.getJobGroup()));
            String personal = I18nUtil.getString("admin_name_full");
            String title = I18nUtil.getString("jobconf_monitor");
            String content = MessageFormat.format(loadEmailJobAlarmTemplate(),
                    group!=null?group.getTitle():"null",
                    info.getId(),
                    info.getJobDesc(),
                    alarmContent);

            //通过逗号分隔符把多个邮件分开
            Set<String> emailSet = new HashSet<String>(Arrays.asList(info.getAlarmEmail().split(",")));

            //给每个邮件地址发送邮件
            for(String email : emailSet){

                //发送邮件
                try {
                    //构建邮件内容
                    MimeMessage mimeMessage = XxlJobAdminConfig.getAdminConfig().getMailSender().createMimeMessage();
                    //简化设置邮件的各种属性，例如发件人、收件人、主题和内容等
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,true);
                    //设置发件人地址和发件人姓名
                    helper.setFrom(XxlJobAdminConfig.getAdminConfig().getEmailFrom());
                    //设置邮件得收件人地址
                    helper.setTo(email);
                    //设置标题
                    helper.setSubject(title);
                    //设置内容
                    helper.setText(content, true);

                    //发送邮件
                    XxlJobAdminConfig.getAdminConfig().getMailSender().send(mimeMessage);
                }catch (Exception e){
                    logger.error(">>>>>>>>>>> xxl-job, job fail alarm email send error, JobLogId:{}", jobLog.getId(), e);
                    alarmResult = false;
                }
            }
        }
        return alarmResult;
    }

    /**
     * 邮件返回模板
     * @return
     */
    private static final String loadEmailJobAlarmTemplate(){
        String mailBodyTemplate = "<h5>" + I18nUtil.getString("jobconf_monitor_detail") + "：</span>" +
                "<table border=\"1\" cellpadding=\"3\" style=\"border-collapse:collapse; width:80%;\" >\n" +
                "   <thead style=\"font-weight: bold;color: #ffffff;background-color: #ff8c00;\" >" +
                "      <tr>\n" +
                "         <td width=\"20%\" >"+ I18nUtil.getString("jobinfo_field_jobgroup") +"</td>\n" +
                "         <td width=\"10%\" >"+ I18nUtil.getString("jobinfo_field_id") +"</td>\n" +
                "         <td width=\"20%\" >"+ I18nUtil.getString("jobinfo_field_jobdesc") +"</td>\n" +
                "         <td width=\"10%\" >"+ I18nUtil.getString("jobconf_monitor_alarm_title") +"</td>\n" +
                "         <td width=\"40%\" >"+ I18nUtil.getString("jobconf_monitor_alarm_content") +"</td>\n" +
                "      </tr>\n" +
                "   </thead>\n" +
                "   <tbody>\n" +
                "      <tr>\n" +
                "         <td>{0}</td>\n" +
                "         <td>{1}</td>\n" +
                "         <td>{2}</td>\n" +
                "         <td>"+ I18nUtil.getString("jobconf_monitor_alarm_type") +"</td>\n" +
                "         <td>{3}</td>\n" +
                "      </tr>\n" +
                "   </tbody>\n" +
                "</table>";

        return mailBodyTemplate;
    }
}
