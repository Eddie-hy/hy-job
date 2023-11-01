package com.hy.job.core.biz.model;

import java.io.Serializable;

/**
 * @Author: HY
 * @Date: 2023-10-12-21:15
 * @Description:此类的主要目的是在任务处理完成后，将相关信息传递给回调方法，以便记录任务处理的结果和状态。
 */
public class HandleCallbackParam implements Serializable {
    private static final long serialVersionUID = 42L;

    private long logId;
    private long logDateTim;

    private int handleCode;
    private String handleMsg;

    public HandleCallbackParam(){}
    public HandleCallbackParam(long logId, long logDateTim, int handleCode, String handleMsg) {
        this.logId = logId;
        this.logDateTim = logDateTim;
        this.handleCode = handleCode;
        this.handleMsg = handleMsg;
    }

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public long getLogDateTim() {
        return logDateTim;
    }

    public void setLogDateTim(long logDateTim) {
        this.logDateTim = logDateTim;
    }

    public int getHandleCode() {
        return handleCode;
    }

    public void setHandleCode(int handleCode) {
        this.handleCode = handleCode;
    }

    public String getHandleMsg() {
        return handleMsg;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }

    @Override
    public String toString() {
        return "HandleCallbackParam{" +
                "logId=" + logId +
                ", logDateTim=" + logDateTim +
                ", handleCode=" + handleCode +
                ", handleMsg='" + handleMsg + '\'' +
                '}';
    }
}
