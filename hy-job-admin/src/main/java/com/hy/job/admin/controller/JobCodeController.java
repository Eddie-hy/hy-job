package com.hy.job.admin.controller;

import com.hy.job.admin.core.model.XxlJobInfo;
import com.hy.job.admin.core.model.XxlJobLogGlue;
import com.hy.job.admin.core.util.I18nUtil;
import com.hy.job.admin.dao.XxlJobInfoDao;
import com.hy.job.admin.dao.XxlJobLogGlueDao;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.glue.GlueTypeEnum;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * @Author: HY
 * @Date: 2023-10-12-22:35
 * @Description:Glue任务界面管理
 */
@Controller
@RequestMapping("/jobcode")
public class JobCodeController {

    @Resource
    private XxlJobInfoDao xxlJobInfoDao;

    @Resource
    private XxlJobLogGlueDao xxlJobLogGlueDao;

    @RequestMapping
    public String index(HttpServletRequest request, Model model, int jobId) {
        XxlJobInfo jobInfo = xxlJobInfoDao.loadById(jobId);
        List<XxlJobLogGlue> jobLogGlues = xxlJobLogGlueDao.findByJobId(jobId);

        if(jobInfo == null){
            throw new RuntimeException(I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        if (GlueTypeEnum.BEAN == GlueTypeEnum.match(jobInfo.getGlueType())) {
            throw new RuntimeException(I18nUtil.getString("jobinfo_glue_gluetype_unvalid"));
        }

        //判断是否具有权限
        JobInfoController.validPermission(request, jobInfo.getJobGroup());

        // Glue类型-字典
        model.addAttribute("GlueTypeEnum", GlueTypeEnum.values());

        model.addAttribute("jobInfo", jobInfo);
        model.addAttribute("jobLogGlues", jobLogGlues);
        return "jobcode/jobcode.index";
    }

    //保存Glue上的代码
    @RequestMapping("/save")
    @ResponseBody
    public ReturnT<String> save(Model model, int id, String glueSource, String glueRemark) {

        //判断命名是否有效
        if (glueRemark==null) {
            return new ReturnT<String>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_glue_remark")) );
        }
        if (glueRemark.length()<4 || glueRemark.length()>100) {
            return new ReturnT<String>(500, I18nUtil.getString("jobinfo_glue_remark_limit"));
        }

        XxlJobInfo exists_jobInfo = xxlJobInfoDao.loadById(id);
        if (exists_jobInfo == null) {
            return new ReturnT<String>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }

        //更新调度信息拓展表
        exists_jobInfo.setGlueSource(glueSource);
        exists_jobInfo.setGlueRemark(glueRemark);
        exists_jobInfo.setGlueUpdatetime(new Date());

        exists_jobInfo.setUpdateTime(new Date());
        xxlJobInfoDao.update(exists_jobInfo);

        //更新任务glue日志
        XxlJobLogGlue xxlJobLogGlue = new XxlJobLogGlue();
        xxlJobLogGlue.setJobId(exists_jobInfo.getId());
        xxlJobLogGlue.setGlueType(exists_jobInfo.getGlueType());
        xxlJobLogGlue.setGlueSource(glueSource);
        xxlJobLogGlue.setGlueRemark(glueRemark);

        xxlJobLogGlue.setAddTime(new Date());
        xxlJobLogGlue.setUpdateTime(new Date());
        xxlJobLogGlueDao.save(xxlJobLogGlue);

        //只保留最新的30条记录
        xxlJobLogGlueDao.removeOld(exists_jobInfo.getId(), 30);

        return ReturnT.SUCCESS;
    }

}