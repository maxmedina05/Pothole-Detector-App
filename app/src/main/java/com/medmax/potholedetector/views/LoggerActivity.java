package com.medmax.potholedetector.views;

import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.medmax.potholedetector.BaseSensorActivity;
import com.medmax.potholedetector.config.AppSettings;
import com.medmax.potholedetector.threads.ThreadManager;
import com.medmax.potholedetector.utilities.CSVHelper;
import com.medmax.potholedetector.utilities.DateTimeHelper;
import com.medmax.potholedetector.utilities.PotholeCSVContract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Created by Max Medina on 2017-07-06.
 */

public class LoggerActivity extends BaseSensorActivity {

    // Constants
    public final static String LOG_TAG = LoggerActivity.class.getSimpleName();

    @Override
    public void myOnClick(View v) {

    }

    @Override
    public void myOnSensorChanged(SensorEvent event) {

    }
}
