package com.medmax.potholedetector.utilities;

/**
 * Created by maxme on 2017-07-12.
 */

public class DefectCSVContract {
    public static class DefectCSV {

        public static final String COLUMN_DEFECT_ID = "defect_id";
        public static final String COLUMN_X_MEAN = "x_mean";
        public static final String COLUMN_X_STD = "x_std";
        public static final String COLUMN_Y_MEAN = "y_mean";
        public static final String COLUMN_Y_STD = "y_std";
        public static final String COLUMN_Z_MEAN = "z_mean";
        public static final String COLUMN_Z_STD = "z_std";

        public static final String COLUMN_SMALL_X_MEAN = "sm_x_mean";
        public static final String COLUMN_SMALL_X_STD = "sm_x_std";
        public static final String COLUMN_SMALL_Y_MEAN = "sm_y_mean";
        public static final String COLUMN_SMALL_Y_STD = "sm_y_std";
        public static final String COLUMN_SMALL_Z_MEAN = "sm_z_mean";
        public static final String COLUMN_SMALL_Z_STD = "sm_z_std";
        public static final String COLUMN_CLASS_TYPE = "class";

        public static String[] getHeaders(){
            return new String[] {
                    "defect_id",

                    "one_x_mean",
                    "one_x_std",
                    "one_y_mean",
                    "one_y_std",
                    "one_z_mean",
                    "one_z_std",

                    "x_mean",
                    "x_std",
                    "y_mean",
                    "y_std",
                    "z_mean",
                    "z_std",

                    "sm_x_mean",
                    "sm_x_std",
                    "sm_y_mean",
                    "sm_y_std",
                    "sm_z_mean",
                    "sm_z_std",
                    "sm_z_max",

                    "start_time",
                    "end_time",

                    "latitude",
                    "longitude",

                    "class"
            };
        }
    }
}
