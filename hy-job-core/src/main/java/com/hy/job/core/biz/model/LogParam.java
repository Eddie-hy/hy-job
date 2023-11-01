package com.hy.job.core.biz.model;

import java.io.Serializable;

/**
 * @Author: HY
 * @Date: 2023-10-06-14:15
 * @Description:日志参数
 */
public class LogParam implements Serializable {
    private static final long serialVersionUID = 42L;

    public LogParam() {
    }
    public LogParam(long logDateTim, long logId, int fromLineNum) {
        this.logDateTim = logDateTim;
        this.logId = logId;
        this.fromLineNum = fromLineNum;
    }

    private long logDateTim;
    private long logId;
    private int fromLineNum;

    public long getLogDateTim() {
        return logDateTim;
    }

    public void setLogDateTim(long logDateTim) {
        this.logDateTim = logDateTim;
    }

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public int getFromLineNum() {
        return fromLineNum;
    }

    public void setFromLineNum(int fromLineNum) {
        this.fromLineNum = fromLineNum;
    }
}
