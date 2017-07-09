package com.medmax.potholedetector.utilities;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Max Medina on 2017-05-28.
 */

public class DateTimeHelper {

    public static String getCurrentDateTime(String pattern) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String currentTime = simpleDateFormat.format(Calendar.getInstance().getTime());
        return currentTime;
    }

    public static String getFormatDate(int year, int month, int dayOfMonth, String pattern) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, dayOfMonth);

        String currentTime = simpleDateFormat.format(cal.getTime());

        return currentTime;
    }

    public static String getFormatDate(long timestamp, String pattern) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);

        return simpleDateFormat.format(cal.getTime());
    }


}