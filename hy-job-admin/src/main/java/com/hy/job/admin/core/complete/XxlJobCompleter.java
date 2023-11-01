package com.hy.job.admin.core.complete;

import com.hy.job.admin.core.conf.XxlJobAdminConfig;
import com.hy.job.admin.core.model.XxlJobInfo;
import com.hy.job.admin.core.model.XxlJobLog;
import com.hy.job.admin.core.thread.JobTriggerPoolHelper;
import com.hy.job.admin.core.trigger.TriggerTypeEnum;
import com.hy.job.admin.core.util.I18nUtil;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.context.XxlJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

/**
 * @Author: HY
 * @Date: 2023-10-14-15:18
 * @Description:查看任务是否有子任务，并完成子任务的任务
 */
public class XxlJobCompleter {
    private static Logger logger = LoggerFactory.getLogger(XxlJobCompleter.class);

    public static int updateHandleInfoAndFinish(XxlJobLog xxlJobLog){

        // finish 若父任务正常结束，则终止子任务,以及设置Childmsg
        finishJob(xxlJobLog);


        //text最大64kb 避免长度过长 截断超过长度限制字符
        if(xxlJobLog.getHandleMsg().length() > 15000){
            xxlJobLog.setHandleMsg(xxlJobLog.getHandleMsg().substring(0, 15000));
        }

        // fresh handle 更新超时joblog
        return XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateHandleInfo(xxlJobLog);
    }

    private static void finishJob(XxlJobLog xxlJobLog) {

        //处理子任务
        String triggerChildMsg = null;
        if(XxlJobContext.HANDLE_CODE_SUCCESS == xxlJobLog.getHandleCode()){
            XxlJobInfo xxlJobInfo = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(xxlJobLog.getJobId());

            //处理子任务
            if (xxlJobInfo!=null && xxlJobInfo.getChildJobId()!=null && xxlJobInfo.getChildJobId().trim().length()>0) {
                triggerChildMsg = "<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>"+ I18nUtil.getString("jobconf_trigger_child_run") +"<<<<<<<<<<< </span><br>";

                String[] childJobIds = xxlJobInfo.getChildJobId().split(",");
                for (int i = 0; i < childJobIds.length; i++) {
                    int childJobId = (childJobIds[i]!=null && childJobIds[i].trim().length()>0 && isNumeric(childJobIds[i]))?Integer.valueOf(childJobIds[i]):-1;
                    if (childJobId > 0) {

                        JobTriggerPoolHelper.trigger(childJobId, TriggerTypeEnum.PARENT, -1, null, null, null);
                        ReturnT<String> triggerChildResult = ReturnT.SUCCESS;

                        // add msg
                        triggerChildMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg1"),
                                (i+1),
                                childJobIds.length,
                                childJobIds[i],
                                (triggerChildResult.getCode()==ReturnT.SUCCESS_CODE?I18nUtil.getString("system_success"):I18nUtil.getString("system_fail")),
                                triggerChildResult.getMsg());
                    } else {
                        triggerChildMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg2"),
                                (i+1),
                                childJobIds.length,
                                childJobIds[i]);
                    }
                }
            }
        }
        if(triggerChildMsg != null){
            xxlJobLog.setHandleMsg(xxlJobLog.getHandleMsg() + triggerChildMsg);
        }

        // 2、fix_delay trigger next
        // on the way
    }


    private static boolean isNumeric(String str){
        try {
            int result = Integer.valueOf(str);
            return true;
        }catch (NumberFormatException e){
            return false;
        }
    }
}
