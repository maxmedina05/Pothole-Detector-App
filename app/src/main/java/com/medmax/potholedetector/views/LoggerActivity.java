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

    // Variables
    private int mIdSeed = 0;

    // Helpers
    CSVHelper csvHelper;

    // Multi threading
    private ThreadManager mThreadManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        csvHelper = new CSVHelper();
        mThreadManager = ThreadManager.getsInstance();
    }

    @Override
    protected void onPause() {
        stopLogging();
        super.onPause();
    }

    @Override
    public void myOnClick(View v) {
        if (!csvHelper.isOpen()) {
            initLogging();
        } else {
            stopLogging();
        }
    }

    @Override
    public void myOnSensorChanged(SensorEvent event) {
        if (csvHelper.isOpen()) {

            mThreadManager.addCallable(new Callable() {
                @Override
                public Object call() throws Exception {
                    logChanges(mTimestamp);
                    return null;
                }
            });
        }
    }

    private void initLogging() {
        mIdSeed = 0;
        String fileName = String.format("%s_%s.%s",
                AppSettings.POTHOLE_FILENAME,
                DateTimeHelper.getCurrentDateTime("yyyy-MM-dd hh-mm-ss"),
                AppSettings.CSV_EXTENSION_NAME
        );

        File exportDir = new File(Environment.getExternalStorageDirectory(), AppSettings.POTHOLE_DIRECTORY);
        try {
            csvHelper.open(exportDir, fileName, true);
            // TODO: Remove logger
            Log.d(LOG_TAG, "Start Logger");
        } catch (FileNotFoundException e) {
            sendToast("File was not found!");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopLogging() {
        mTimestamp = 0;
        if (csvHelper.isOpen()) {
            try {
                sendToast(String.format("file %s was created", csvHelper.getcurrentFileName()));
                csvHelper.close();
                // TODO: Remove logger
                Log.d(LOG_TAG, "Stop Logger");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void logChanges(float timestamp) throws IOException {
        // check again to see if the file is still open
        if (csvHelper.isOpen()) {
            if (mIdSeed == 0) {
                csvHelper.setHeader(PotholeCSVContract.PotholeCSV.getHeaders());
            }

            csvHelper.write(String.format(
                    Locale.US,
                    "%d,%s,%s,%.06f,%.6f,%.6f,%.6f",
                    ++mIdSeed,
                    DateTimeHelper.getCurrentDateTime("yyyy-MM-dd hh:mm:ss.SSS"),
                    mDeviceName,
                    timestamp,
                    acc_values[0],
                    acc_values[1],
                    acc_values[2])
            );
        }
    }
}
