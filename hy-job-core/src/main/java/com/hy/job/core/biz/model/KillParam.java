package com.hy.job.core.biz.model;

import java.io.Serializable;

/**
 * @Author: HY
 * @Date: 2023-10-16-21:29
 * @Description:删除参数
 */
public class KillParam implements Serializable {
    private static final long serialVersionUID = 42L;

    public KillParam() {
    }
    public KillParam(int jobId) {
        this.jobId = jobId;
    }

    private int jobId;


    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }
}
