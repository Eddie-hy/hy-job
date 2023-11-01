package com.hy.job.admin.controller;

import com.hy.job.admin.core.complete.XxlJobCompleter;
import com.hy.job.admin.core.exception.XxlJobException;
import com.hy.job.admin.core.model.XxlJobGroup;
import com.hy.job.admin.core.model.XxlJobInfo;
import com.hy.job.admin.core.model.XxlJobLog;
import com.hy.job.admin.core.scheduler.XxlJobScheduler;
import com.hy.job.admin.core.util.I18nUtil;
import com.hy.job.admin.dao.XxlJobGroupDao;
import com.hy.job.admin.dao.XxlJobInfoDao;
import com.hy.job.admin.dao.XxlJobLogDao;
import com.hy.job.core.biz.ExecutorBiz;
import com.hy.job.core.biz.model.KillParam;
import com.hy.job.core.biz.model.LogParam;
import com.hy.job.core.biz.model.LogResult;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: HY
 * @Date: 2023-10-05-20:31
 * @Description:日志处理
 */
@Controller
@RequestMapping("/joblog")
public class JobLogController {

    private static Logger logger = LoggerFactory.getLogger(JobLogController.class);

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;

    @Resource
    public XxlJobInfoDao xxlJobInfoDao;

    @Resource
    public XxlJobLogDao xxlJobLogDao;


    @RequestMapping
    public String index(HttpServletRequest request, Model model, @RequestParam(required = false, defaultValue = "0") Integer jobId) {

        //执行器列表
        List<XxlJobGroup> jobGroupList_all = xxlJobGroupDao.findAll();

        //查找执行器
        List<XxlJobGroup> jobGroupList = JobInfoController.filterJobGroupByRole(request, jobGroupList_all);
        if(jobGroupList == null && jobGroupList.size() == 0){
            throw new XxlJobException(I18nUtil.getString("jobgroup_empty"));
        }

        model.addAttribute("JobGroupList", jobGroupList);

        // 任务
        if (jobId > 0) {
            XxlJobInfo jobInfo = xxlJobInfoDao.loadById(jobId);
            if (jobInfo == null) {
                throw new RuntimeException(I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_unvalid"));
            }

            model.addAttribute("jobInfo", jobInfo);

            // 判断是否是admin或者是否具有这个执行器的权限
            JobInfoController.validPermission(request, jobInfo.getJobGroup());
        }
        return "joblog/joblog.index";
    }

    //根据执行器编号查询日志
    @RequestMapping("/getJobsByGroup")
    @ResponseBody
    public ReturnT<List<XxlJobInfo>> getJobsByGroup(int jobGroup){
       List<XxlJobInfo> list = xxlJobInfoDao.getJobsByGroup(jobGroup);
       return new ReturnT<List<XxlJobInfo>>(list);
    }

    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(HttpServletRequest request,
                                        @RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        int jobGroup, int jobId, int logStatus, String filterTime) {

        //判断是否有权限
        JobInfoController.validPermission(request, jobGroup); // 仅管理员支持查询全部；普通用户仅支持查询有权限的 jobGroup

        //解析参数 把传递的时间范围拆分为两个时间参数
        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if(filterTime != null && filterTime.trim().length() > 0) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                triggerTimeStart = DateUtil.parseDateTime(temp[0]);
                triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
            }
        }

        //查询参数信息
        List<XxlJobLog> list = xxlJobLogDao.pageList(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
        int list_count = xxlJobLogDao.pageListCount(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);

        //封装参数
        Map<String , Object> maps = new HashMap<String , Object>();
        maps.put("recordsTotal", list_count);		// 总记录数
        maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
        maps.put("data", list);  					// 分页列表

        return maps;
        }


        //日志详细页面
    @RequestMapping("/logDetailPage")
    public String logDetailPage(int id, Model model){

        ReturnT<String> logStatue = ReturnT.SUCCESS;
        XxlJobLog jobLog = xxlJobLogDao.load(id);

        if(jobLog == null){
            throw new RuntimeException(I18nUtil.getString("joblog_logid_unvalid"));
        }

        model.addAttribute("triggerCode", jobLog.getTriggerCode());
        model.addAttribute("handleCode", jobLog.getHandleCode());
        model.addAttribute("logId", jobLog.getId());
        return "joblog/joblog.detail";
    }

    //加载日志信息
    @RequestMapping("/logDetailCat")
    @ResponseBody
    public ReturnT<LogResult> logDetailCat(long logId, int fromLineNum){

        try{
            XxlJobLog jobLog = xxlJobLogDao.load(logId);
            if (jobLog == null) {
                return new ReturnT<LogResult>(ReturnT.FAIL_CODE, I18nUtil.getString("joblog_logid_unvalid"));
            }

            //根据地址绑定执行器
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(jobLog.getExecutorAddress());
            ReturnT<LogResult> logResult = executorBiz.log(new LogParam(jobLog.getTriggerTime().getTime(), logId, fromLineNum));

            //判断日志是否存在
            if(logResult.getContent() != null && logResult.getContent().getFromLineNum() > logResult.getContent().getToLineNum()){
                if(jobLog.getHandleCode() > 0){
                    logResult.getContent().setEnd(true);
                }
            }
            return logResult;
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            return new ReturnT<LogResult>(ReturnT.FAIL_CODE, e.getMessage());
        }
    }

    @RequestMapping("/logKill")
    @ResponseBody
    public ReturnT<String> logKill(int id){
        // base check
        XxlJobLog log = xxlJobLogDao.load(id);
        XxlJobInfo jobInfo = xxlJobInfoDao.loadById(log.getJobId());
        if (jobInfo==null) {
            return new ReturnT<String>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        if (ReturnT.SUCCESS_CODE != log.getTriggerCode()) {
            return new ReturnT<String>(500, I18nUtil.getString("joblog_kill_log_limit"));
        }

        // request of kill
        ReturnT<String> runResult = null;
        try {
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(log.getExecutorAddress());
            runResult = executorBiz.kill(new KillParam(jobInfo.getId()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            runResult = new ReturnT<String>(500, e.getMessage());
        }

        if (ReturnT.SUCCESS_CODE == runResult.getCode()) {
            log.setHandleCode(ReturnT.FAIL_CODE);
            log.setHandleMsg( I18nUtil.getString("joblog_kill_log_byman")+":" + (runResult.getMsg()!=null?runResult.getMsg():""));
            log.setHandleTime(new Date());
            XxlJobCompleter.updateHandleInfoAndFinish(log);
            return new ReturnT<String>(runResult.getMsg());
        } else {
            return new ReturnT<String>(500, runResult.getMsg());
        }
    }



    //清楚指定日志信息
    @RequestMapping("/clearLog")
    @ResponseBody
    public ReturnT<String> clearLog(int jobGroup, int jobId, int type){

        Date clearBeforeTime = null;
        int clearBeforeNum = 0;
        if (type == 1) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -1);	// 清理一个月之前日志数据
        } else if (type == 2) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -3);	// 清理三个月之前日志数据
        } else if (type == 3) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -6);	// 清理六个月之前日志数据
        } else if (type == 4) {
            clearBeforeTime = DateUtil.addYears(new Date(), -1);	// 清理一年之前日志数据
        } else if (type == 5) {
            clearBeforeNum = 1000;		// 清理一千条以前日志数据
        } else if (type == 6) {
            clearBeforeNum = 10000;		// 清理一万条以前日志数据
        } else if (type == 7) {
            clearBeforeNum = 30000;		// 清理三万条以前日志数据
        } else if (type == 8) {
            clearBeforeNum = 100000;	// 清理十万条以前日志数据
        } else if (type == 9) {
            clearBeforeNum = 0;			// 清理所有日志数据
        } else {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("joblog_clean_type_unvalid"));
        }

        List<Long> logIds = null;
        do{
            logIds = xxlJobLogDao.findClearLogIds(jobGroup, jobId, clearBeforeTime, clearBeforeNum, 1000);
        }while (logIds != null && logIds.size() >0);


        return new ReturnT<>("日志删除成功！");
    }

}
