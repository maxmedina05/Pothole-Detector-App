package com.medmax.potholedetector.utilities;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by maxme on 2017-05-28.
 */

public class TimeHelper {

    public static String getCurrentDateTime(String pattern) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String currentTime = simpleDateFormat.format(Calendar.getInstance().getTime());
        return currentTime;
    }
}
