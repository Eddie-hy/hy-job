package com.hy.job.core.biz.model;

import java.io.Serializable;

/**
 * @Author: HY
 * @Date: 2023-10-07-19:55
 * @Description:心跳次数计算
 */
public class IdleBeatParam implements Serializable {
    private static final long serialVersionUID = 42L;

    public IdleBeatParam() {
    }
    public IdleBeatParam(int jobId) {
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
