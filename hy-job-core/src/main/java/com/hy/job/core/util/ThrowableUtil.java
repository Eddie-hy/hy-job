package com.hy.job.core.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @Author: HY
 * @Date: 2023-10-12-14:15
 * @Description:异常信息抛出类
 */
public class ThrowableUtil {
    /**
     * 把错误类型转换为字符串
     */
    public static String toString(Throwable e){
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String errorMsg = stringWriter.toString();
        return errorMsg;
    }
}
