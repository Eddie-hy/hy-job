package com.hy.job.admin.controller;

import com.hy.job.admin.core.exception.XxlJobException;
import com.hy.job.admin.core.model.XxlJobGroup;
import com.hy.job.admin.core.model.XxlJobInfo;
import com.hy.job.admin.core.model.XxlJobUser;
import com.hy.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.hy.job.admin.core.scheduler.MisfireStrategyEnum;
import com.hy.job.admin.core.scheduler.ScheduleTypeEnum;
import com.hy.job.admin.core.thread.JobScheduleHelper;
import com.hy.job.admin.core.thread.JobTriggerPoolHelper;
import com.hy.job.admin.core.trigger.TriggerTypeEnum;
import com.hy.job.admin.core.util.I18nUtil;
import com.hy.job.admin.dao.XxlJobGroupDao;
import com.hy.job.admin.service.LoginService;
import com.hy.job.admin.service.XxlJobService;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.enums.ExecutorBlockStrategyEnum;
import com.hy.job.core.glue.GlueTypeEnum;
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
import java.util.*;

/**
 * @Author: HY
 * @Date: 2023-10-05-21:11
 * @Description:
 */
@Controller
@RequestMapping("/jobinfo")
public class JobInfoController {

    private static Logger logger = LoggerFactory.getLogger(JobInfoController.class);

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;

    @Resource
    private XxlJobService xxlJobService;

    @RequestMapping
    public String index(HttpServletRequest request, Model model, @RequestParam(required = false, defaultValue = "-1") int jobGroup) {

        // 枚举-字典
        model.addAttribute("ExecutorRouteStrategyEnum", ExecutorRouteStrategyEnum.values());	    // 路由策略-列表
        model.addAttribute("GlueTypeEnum", GlueTypeEnum.values());    //Glue类型-字典  判断与之连接的是那种脚本
        model.addAttribute("ExecutorBlockStrategyEnum", ExecutorBlockStrategyEnum.values());	    // 阻塞处理策略-字典
        model.addAttribute("ScheduleTypeEnum", ScheduleTypeEnum.values());	    				// 调度时间策略类型
        model.addAttribute("MisfireStrategyEnum", MisfireStrategyEnum.values());	    			// 调度过期策略


        //得到所有执行器列表
        List<XxlJobGroup> jobGroupList_all = xxlJobGroupDao.findAll();

        //过滤执行器
        List<XxlJobGroup> jobGroupList = filterJobGroupByRole(request, jobGroupList_all);
        if(jobGroupList == null || jobGroupList.size() == 0){
            throw new XxlJobException(I18nUtil.getString("jobgroup_empty"));
        }

        //把数据加进视图中
        model.addAttribute("JobGroupList", jobGroupList);
        model.addAttribute("jobGroup", jobGroup);

        return "jobinfo/jobinfo.index";
    }


    /**
     * 查找用户可以使用的执行器
     * @param request
     * @param jobGroupList_all
     * @return
     */
    public static List<XxlJobGroup> filterJobGroupByRole(HttpServletRequest request, List<XxlJobGroup> jobGroupList_all){

        List<XxlJobGroup> jobGroupList =new ArrayList<>();

        if(jobGroupList_all != null && jobGroupList_all.size() > 0){
            XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
            if(loginUser.getRole() == 1){  //是管理员
                jobGroupList = jobGroupList_all;
            }else{
                List<String> groupIdStrs = new ArrayList<>();
                if(loginUser.getPermission() != null && loginUser.getPermission().trim().length() > 0){
                    groupIdStrs = Arrays.asList(loginUser.getPermission().trim().split(","));
                }
                //如果执行器表中有这个执行器就加入集合中
                for(XxlJobGroup groupItem : jobGroupList_all){
                    if(groupIdStrs.contains(String.valueOf(groupItem.getId()))){
                        jobGroupList.add(groupItem);
                    }
                }
            }

        }
        return jobGroupList;
    }

    /**
     * 判断用户是否是管理员或者是否具有权限
     * @param request
     * @param jobGroup
     */
    public static void validPermission(HttpServletRequest request, int jobGroup) {
        XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        if (!loginUser.validPermission(jobGroup)) {
            throw new RuntimeException(I18nUtil.getString("system_permission_limit") + "[username="+ loginUser.getUsername() +"]");
        }
    }

    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author) {

        return xxlJobService.pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
    }

    @RequestMapping("/add")
    @ResponseBody
    public ReturnT<String> add(XxlJobInfo jobInfo) {
        return xxlJobService.add(jobInfo);
    }

    @RequestMapping("/update")
    @ResponseBody
    public ReturnT<String> update(XxlJobInfo jobInfo) {
        return xxlJobService.update(jobInfo);
    }

    //删除任务信息
    @RequestMapping("/remove")
    @ResponseBody
    public ReturnT<String> remove(int id) {
        return xxlJobService.remove(id);
    }

    @RequestMapping("/start")
    @ResponseBody
    public ReturnT<String> start(int id) {
        return xxlJobService.start(id);
    }

    @RequestMapping("/stop")
    @ResponseBody
    public ReturnT<String> pause(int id) {
        return xxlJobService.stop(id);
    }

    //只执行一次
    @RequestMapping("/trigger")
    @ResponseBody
    //@PermissionLimit(limit = false)
    public ReturnT<String> triggerJob(int id, String executorParam, String addressList) {
        if(executorParam == null){
            executorParam = "";
        }
        JobTriggerPoolHelper.trigger(id, TriggerTypeEnum.MANUAL, -1, null, executorParam, addressList);
        return ReturnT.SUCCESS;
    }

    /**
     * 得到下五次的触发时间
     */
    @RequestMapping("/nextTriggerTime")
    @ResponseBody
    public ReturnT<List<String>> nextTriggerTime(String scheduleType, String scheduleConf) {

        XxlJobInfo paramXxlJobInfo = new XxlJobInfo();
        paramXxlJobInfo.setScheduleType(scheduleType);
        paramXxlJobInfo.setScheduleConf(scheduleConf);

        List<String> result = new ArrayList<>();
        try{
            Date lastTime = new Date();
            for(int i = 0;i < 5; i++){
                lastTime = JobScheduleHelper.generateNextValidTime(paramXxlJobInfo, lastTime);
                if(lastTime != null){
                    result.add(DateUtil.formatDateTime(lastTime));
                }else{
                    break;
                }
            }
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            return new ReturnT<List<String>>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) + e.getMessage());
        }
        return new ReturnT<List<String>>(result);
    }


}
