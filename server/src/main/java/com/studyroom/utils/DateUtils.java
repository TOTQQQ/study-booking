package com.studyroom.utils;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;

public class DateUtils {  // 类名改成 DateUtils
    
    public static String getToday() {
        return DateUtil.today();
    }
    
    public static long parseToTimestamp(String date, String time) {
        DateTime dateTime = DateUtil.parse(date + " " + time, "yyyy-MM-dd HH:mm:ss");
        return dateTime.getTime();
    }
    
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
    
    public static boolean isTimeout(String date, String startTime, int timeoutMinutes) {
        long start = parseToTimestamp(date, startTime);
        long now = getCurrentTimestamp();
        return now > start + (timeoutMinutes * 60 * 1000L);
    }
    
    public static long daysBetween(String date1, String date2) {
        DateTime d1 = DateUtil.parse(date1);
        DateTime d2 = DateUtil.parse(date2);
        return Math.abs(DateUtil.betweenDay(d1, d2, false));
    }
}