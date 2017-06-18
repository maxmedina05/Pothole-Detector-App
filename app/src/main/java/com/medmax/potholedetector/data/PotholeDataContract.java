package com.medmax.potholedetector.data;

import android.provider.BaseColumns;

/**
 * Created by maxme on 2017-06-11.
 */

public class PotholeDataContract {

    private PotholeDataContract() {
    }

    public static class PossiblePothole implements BaseColumns {
        public static final String TABLE_NAME = "possible_holes";
        public static final String DATE_CREATED = "date";
        public static final String DEVICE_NAME = "device";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String TYPE = "type";
    }


}
