package com.medmax.potholedetector.utilities;

/**
 * Created by maxme on 2017-07-08.
 */

public class PotholeCSVContract {
    public static class PotholeCSV {
        // Code  Date   Timestamp	DeviceName	X-Axis	Y-Axis	Z-Axis
        public static final String COLUMN_CODE = "Code";
        public static final String COLUMN_DATE = "Date";
        public static final String COLUMN_Timestamp = "Timestamp";
        public static final String COLUMN_DeviceName = "X-Axis";
        public static final String COLUMN_Y_Axis = "Y-Axis";
        public static final String COLUMN_Z_Axis = "Z-Axis";
//        public static final String COLUMN_LONGITUDE = "Longitude";
//        public static final String COLUMN_LATITUDE = "Latitude";

        public static String[] getHeaders() {
            return new String[] {
                    "Code",
                    "Date",
                    "DeviceName",
                    "Timestamp",
                    "X-Axis",
                    "Y-Axis",
                    "Z-Axis"
            };
        }
    }
}
