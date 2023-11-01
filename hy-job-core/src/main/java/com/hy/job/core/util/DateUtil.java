package com.hy.job.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: HY
 * @Date: 2023-10-05-10:54
 * @Description:关于时间日期的配置类
 */

public class DateUtil {

    private static Logger logger = LoggerFactory.getLogger(DateUtil.class);

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     *这段代码创建了一个线程本地变量，用于在多线程环境下存储和获取日期格式化对象，以确保线程安全性和避免线程间的竞争条件
     */
    private static final ThreadLocal<Map<String, DateFormat>> dateFormatThreadLocal = new ThreadLocal<Map<String, DateFormat>>();

    //
    public static DateFormat getDateFormat(String pattern){
        if(pattern == null || pattern.trim().length() == 0){
            throw new IllegalArgumentException("pattern cannot be empty.");
        }

        Map<String, DateFormat> dateFormatMap = dateFormatThreadLocal.get();
        if(dateFormatMap != null && dateFormatMap.containsKey(pattern)){
            return dateFormatMap.get(pattern);
        }

        synchronized (dateFormatThreadLocal){
            if(dateFormatMap == null){
                dateFormatMap = new HashMap<String, DateFormat>();
            }
            dateFormatMap.put(pattern ,new SimpleDateFormat(pattern));
            dateFormatThreadLocal.set(dateFormatMap);
        }

        return dateFormatMap.get(pattern);
    }


    /**
     * 将日期转变为特定格式的字符串  "yyyy-MM-dd"
     * @param date
     * @return
     */
    public static String formatDate(Date date){
        return format(date , DATE_FORMAT);
    }

    /**
     * 将日期转变为特定格式的字符串   "yyyy-MM-dd HH:mm:ss"
     * @param date
     * @return
     */
    public static String formatDateTime(Date date){
        return format(date , DATETIME_FORMAT);
    }



    /**
     * 将日期转变为特定格式的字符串
     * @param date
     * @param pattern
     * @return
     */
    public static String format(Date date , String pattern){
        return getDateFormat(pattern).format(date);
    }


    /**
     * 把日期字符串按照规则转换为Date对象  "yyyy-MM-dd HH:mm:s"
     * @param dateString
     * @return
     */
    public static Date parseDate(String dateString){
        return parse(dateString, DATE_FORMAT);
    }


    /**
     * 把日期字符串按照规则转换为Date对象  "yyyy-MM-dd HH:mm:ss"
     * @param dateString
     * @return
     */
    public static Date parseDateTime(String dateString){
        return parse(dateString, DATETIME_FORMAT);
    }

    /**
     * 把日期字符串转换为日期对象
     * @param dateString
     * @param pattern
     * @return
     */
    public static Date parse(String dateString , String pattern){
        try{
            Date date = getDateFormat(pattern).parse(dateString);
            return date;
        }catch (Exception e){
            logger.warn("parse date error, dateString = {}, pattern={}; errorMsg = {}", dateString, pattern, e.getMessage());
            return null;
        }
    }


    // ---------------------- add date ----------------------

    public static Date addYears(final Date date, final int amount) {
        return add(date, Calendar.YEAR, amount);
    }

    public static Date addMonths(final Date date, final int amount) {
        return add(date, Calendar.MONTH, amount);
    }

    public static Date addDays(final Date date, final int amount) {
        return add(date, Calendar.DAY_OF_MONTH, amount);
    }

    public static Date addHours(final Date date, final int amount) {
        return add(date, Calendar.HOUR_OF_DAY, amount);
    }

    public static Date addMinutes(final Date date, final int amount) {
        return add(date, Calendar.MINUTE, amount);
    }

    //对日期进行加操作
    private static Date add(final Date date, final int calendarField, final int amount) {
        if (date == null) {
            return null;
        }
        final Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(calendarField, amount);
        return c.getTime();
    }

}
