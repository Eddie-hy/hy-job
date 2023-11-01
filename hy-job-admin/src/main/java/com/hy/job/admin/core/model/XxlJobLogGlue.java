package com.hy.job.admin.core.model;

import lombok.Data;

import java.util.Date;

@Data
public class XxlJobLogGlue {

    private int id;
    private int jobId;				// 任务主键ID
    private String glueType;		// GLUE类型	#com.xxl.job.core.glue.GlueTypeEnum
    private String glueSource;
    private String glueRemark;
    private Date addTime;
    private Date updateTime;
}
