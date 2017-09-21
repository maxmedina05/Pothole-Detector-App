package com.medmax.potholedetector.views;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.View;

import com.medmax.potholedetector.BaseSensorActivity;
import com.medmax.potholedetector.config.AppSettings;
import com.medmax.potholedetector.utilities.CSVHelper;
import com.medmax.potholedetector.utilities.DateTimeHelper;
import com.medmax.potholedetector.utilities.PotholeCSVContract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

/**
 * Created by Max Medina on 2017-07-06.
 */

public class LoggerActivity extends BaseSensorActivity {

    // Constants
    public final static String LOG_TAG = LoggerActivity.class.getSimpleName();

    // Variables
    protected boolean isLogging = false;
    private long mIdSeed = 0;
    private long mLogStartTime = 0;

    // Helpers
    private CSVHelper csvHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        csvHelper = new CSVHelper();
    }

    @Override
    public void onClick(View v) {
        isLogging = !isLogging;
        if (isLogging) {
            initLogger();
        } else {
            stopLogger();
        }
    }

    @Override
    protected void onAccelerometerSensorChanged(float[] values) {
        super.onAccelerometerSensorChanged(values);

        if(isLogging) {
            mTimestamp = (System.currentTimeMillis() - mLogStartTime) / 1000.0f;

            new LogTask(++mIdSeed, mTimestamp, mRawAccelerometerValues, lastLatitude, lastLongitude).execute();
        }
    }

    private void initLogger() {
        mLogStartTime = System.currentTimeMillis();
        mIdSeed = 0;
        if (!mPreferenceManager.isDebuggerOn()) {
            String fileName = String.format("%s_%s.%s",
                    AppSettings.POTHOLE_FILENAME,
                    DateTimeHelper.getCurrentDateTime("yyyy-MM-dd hh-mm-ss"),
                    AppSettings.CSV_EXTENSION_NAME
            );

            File exportDir = new File(Environment.getExternalStorageDirectory(), AppSettings.LOGGER_DIRECTORY);
            try {
                csvHelper.open(exportDir, fileName, true);
                csvHelper.setHeader(PotholeCSVContract.PotholeCSV.getHeaders());
            } catch (FileNotFoundException e) {
                sendToast("File was not found!");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopLogger() {
        if (!mPreferenceManager.isDebuggerOn() && csvHelper.isOpen()) {
            try {
                sendToast(String.format("file %s was created", csvHelper.getCurrentFileName()));
                csvHelper.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void logData(float timestamp, long logId, float[] data, float latitude, float longitude) throws IOException {
        if (csvHelper.isOpen()) {
            csvHelper.write(String.format(
                    Locale.US,
                    "%d,%s,%s,%.06f,%.6f,%.6f,%.6f,%f,%f",
                    logId,
                    DateTimeHelper.getCurrentDateTime("yyyy-MM-dd hh:mm:ss.SSS"),
                    mDeviceName,
                    timestamp,
                    data[0],
                    data[1],
                    data[2],
                    latitude,
                    longitude)
            );
        }
    }

    private class LogTask extends AsyncTask<Void, Void, Integer> {
        private long logId;
        private float timeStamp;
        private float latitude;
        private float longitude;

        private float[] data;

        LogTask(long logId, float timeStamp, float[] data, float latitude, float longitude) {
            this.logId = logId;
            this.timeStamp = timeStamp;
            this.data = data;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                logData(timeStamp, logId, data, latitude, longitude);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }

    }
}
