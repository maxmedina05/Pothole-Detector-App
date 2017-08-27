package com.medmax.potholedetector.config;

/**
 * Created by Max Medina on 2017-07-07.
 */

public class AppSettings {
    public static final String LOGGER_DIRECTORY = "pothole_exports";
    public static final String DEFECTS_DIRECTORY = "defects_exports";
    public static final String POTHOLE_FILENAME = "pothole_log";
    public static final String CSV_EXTENSION_NAME = "csv";
    public static final String POTHOLE_LOG_THREAD = "pothole_log_thread";
    public static final String DEFECTS_FILENAME = "defects_export.csv";
    public static final String MOCK_DATA_FILENAME = "mock-dataset.csv";

    public static final float GRAVITY_CONSTANT = 9.80f;
    public static final float COOLDOWN_TIME = 1.0f;
    public static final int NDIGITS = 5;

    public static final float SPEED_CONSTANT = 3.6f;
}

