package com.hy.job.core.biz.model;

import java.io.Serializable;

/**
 * @Author: HY
 * @Date: 2023-10-06-11:12
 * @Description:这个类的主要作用是将从日志中提取的一部分内容封装成一个对象，以便在程序中传递、显示或进一步处理
 */
public class LogResult implements Serializable {
    private static final long serialVersionUID = 42L;

    public LogResult() {
    }
    public LogResult(int fromLineNum, int toLineNum, String logContent, boolean isEnd) {
        this.fromLineNum = fromLineNum;
        this.toLineNum = toLineNum;
        this.logContent = logContent;
        this.isEnd = isEnd;
    }

    private int fromLineNum;
    private int toLineNum;
    private String logContent;
    private boolean isEnd;

    public int getFromLineNum() {
        return fromLineNum;
    }

    public void setFromLineNum(int fromLineNum) {
        this.fromLineNum = fromLineNum;
    }

    public int getToLineNum() {
        return toLineNum;
    }

    public void setToLineNum(int toLineNum) {
        this.toLineNum = toLineNum;
    }

    public String getLogContent() {
        return logContent;
    }

    public void setLogContent(String logContent) {
        this.logContent = logContent;
    }

    public boolean isEnd() {
        return isEnd;
    }

    public void setEnd(boolean end) {
        isEnd = end;
    }
}
