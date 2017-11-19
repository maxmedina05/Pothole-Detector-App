package com.medmax.potholedetector.services;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.medmax.potholedetector.config.AppSettings;
import com.medmax.potholedetector.utilities.CSVHelper;
import com.medmax.potholedetector.utilities.DateTimeHelper;
import com.medmax.potholedetector.utilities.PotholeCSVContract;
import com.medmax.potholedetector.views.LoggerActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

/**
 * Created by Max Medina on 2017-10-18.
 */

public class LoggerService extends BaseSensorService {

    // Constants
    private final static String LOG_TAG = LoggerService.class.getSimpleName();

    // Variables
    private long mIdSeed = 0;
    private long mLogStartTime = 0;

    // Helpers
    private CSVHelper csvHelper;

    public LoggerService(Context context) {
        super(context);
        csvHelper = new CSVHelper();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initLogger();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLogger();
    }

    @Override
    protected void onAccelerometerSensorChanged(float[] values) {
        super.onAccelerometerSensorChanged(values);
        mTimestamp = (System.currentTimeMillis() - mLogStartTime) / 1000.0f;
        new LoggerService.LogTask(++mIdSeed, mTimestamp, mRawAccelerometerValues, lastLatitude, lastLongitude).execute();
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
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopLogger() {
        if (!mPreferenceManager.isDebuggerOn() && csvHelper.isOpen()) {
            try {
                csvHelper.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void logData(float timestamp, long logId, float[] data, float latitude, float longitude) throws IOException {
        String line = String.format(
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
                longitude);

//        Log.d(LOG_TAG, line);

        if (csvHelper.isOpen()) {
            csvHelper.write(line);
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
