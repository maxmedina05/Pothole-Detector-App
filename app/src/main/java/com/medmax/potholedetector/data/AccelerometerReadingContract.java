package com.medmax.potholedetector.data;

import android.provider.BaseColumns;

/**
 * Created by Max Medina on 2017-05-20.
 */

public final class AccelerometerReadingContract {

    private AccelerometerReadingContract() {
    }

    public static class AccelerometerReading implements BaseColumns {
        public static String TABLE_NAME = "accelerometer_reading";
        public static String COLUMN_NAME_TIMESPAN = "timespan";
        public static String COLUMN_NAME_DEVICE_MODEL = "accelerometer_reading";
        public static String COLUMN_NAME_ACC_X_AXIS = "x_axis";
        public static String COLUMN_NAME_ACC_Y_AXIS = "y_axis";
        public static String COLUMN_NAME_ACC_Z_AXIS = "z_axis";

    }
}
