package com.medmax.potholedetector.views;

import android.hardware.SensorManager;
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
 * Created by Max Medina on 2017-08-23.
 */

public class VirtualOLoggerActivity extends BaseSensorActivity {

    // Constants
    public final static String LOG_TAG = VirtualOLoggerActivity.class.getSimpleName();
    private static final float ALPHA = 0.8f;

    // Variables
    protected boolean isLogging = false;
    private long mIdSeed = 0;
    private long mLogStartTime = 0;

    protected volatile float[] mGravityValues = new float[3];
    protected volatile float[] mLinearAccelerationValues = new float[3];
    protected volatile float[] mVirtualAccelerationValues = new float[3];

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

            // Isolate the force of mGravityValues with the low-pass filter.
            mGravityValues[0] = ALPHA * mGravityValues[0] + (1 - ALPHA) * values[0];
            mGravityValues[1] = ALPHA * mGravityValues[1] + (1 - ALPHA) * values[1];
            mGravityValues[2] = ALPHA * mGravityValues[2] + (1 - ALPHA) * values[2];

            // Remove the mGravityValues contribution with the high-pass filter.
            mLinearAccelerationValues[0] = values[0] - mGravityValues[0];
            mLinearAccelerationValues[1] = values[1] - mGravityValues[1];
            mLinearAccelerationValues[2] = values[2] - mGravityValues[2];

            float[] rotationMatrix = new float[9];

            if(SensorManager.getRotationMatrix(rotationMatrix, null, values, mRawMagneticValues)){
                float[] vrm = computeVirtualAcceleration(rotationMatrix, mLinearAccelerationValues);
                System.arraycopy(vrm, 0, mVirtualAccelerationValues, 0, vrm.length);
            }

            new LogTask(++mIdSeed, mTimestamp, mVirtualAccelerationValues).execute();
        }
    }

    private void initLogger() {
        mLogStartTime = System.currentTimeMillis();
        mIdSeed = 0;
        if (!mPreferenceManager.isDebuggerOn()) {
            String fileName = String.format("%s_%s.%s",
                    AppSettings.VO_POTHOLE_FILENAME,
                    DateTimeHelper.getCurrentDateTime("yyyy-MM-dd hh-mm-ss"),
                    AppSettings.CSV_EXTENSION_NAME
            );
            File exportDir = new File(Environment.getExternalStorageDirectory(), AppSettings.VO_LOGGER_DIRECTORY);
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

    private synchronized void logData(float timestamp, long logId, float[] data) throws IOException {
        if (csvHelper.isOpen()) {
            csvHelper.write(String.format(
                    Locale.US,
                    "%d,%s,%s,%.06f,%.6f,%.6f,%.6f",
                    logId,
                    DateTimeHelper.getCurrentDateTime("yyyy-MM-dd hh:mm:ss.SSS"),
                    mDeviceName,
                    timestamp,
                    data[0],
                    data[1],
                    data[2])
            );
        }
    }

    private float[] computeVirtualAcceleration(float[] rotationMatrix, float[] linearAcceleration) {
        float r[] = new float[3];
        float l[] = linearAcceleration;
        float m[][] = invertRotationMatrix(rotationMatrix);

        // multiply inverse rotation matrix with linear acceleration
        r[0] = l[0] * m[0][0] + l[1] * m[1][0] + l[2] * m[2][0];
        r[1] = l[0] * m[0][1] + l[1] * m[1][1] + l[2] * m[2][1];
        r[2] = l[0] * m[0][2] + l[1] * m[1][2] + l[2] * m[2][2];

        return r;
    }

    private float[][] invertRotationMatrix(float[] matrix) {
        float[][] result = new float[3][3];
        result[0][0] = matrix[0];
        result[0][1] = matrix[3];
        result[0][2] = matrix[6];

        result[1][0] = matrix[1];
        result[1][1] = matrix[4];
        result[1][2] = matrix[7];

        result[2][0] = matrix[2];
        result[2][1] = matrix[5];
        result[2][2] = matrix[8];

        return result;
    }

    private class LogTask extends AsyncTask<Void, Void, Integer> {
        private long logId;
        private float timeStamp;
        private float[] data;

        LogTask(long logId, float timeStamp, float[] data) {
            this.logId = logId;
            this.timeStamp = timeStamp;
            this.data = data;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                logData(timeStamp, logId, data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }

    }
}
