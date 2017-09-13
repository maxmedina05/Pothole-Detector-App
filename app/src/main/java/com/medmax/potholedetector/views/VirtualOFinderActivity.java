package com.medmax.potholedetector.views;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.medmax.potholedetector.config.AppSettings;
import com.medmax.potholedetector.data.analyzer.PotholeDataFrame;
import com.medmax.potholedetector.models.AccData;
import com.medmax.potholedetector.models.Defect;
import com.medmax.potholedetector.utilities.MathHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Max Medina on 2017-08-26.
 */

public class VirtualOFinderActivity extends VirtualOLoggerActivity {

    // Analyzer
    private PotholeDataFrame mDataFrame;

    private boolean defectFound = false;
    private float finderStartTime = 0;
    private float finderCurrentTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataFrame = new PotholeDataFrame();
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        if(isLogging) {
            finderCurrentTime = 0;
        }
    }

    @Override
    protected void onAccelerometerSensorChanged(float[] values) {
        super.onAccelerometerSensorChanged(values);

        float x = mVirtualAccelerationValues[0];
        float y = mVirtualAccelerationValues[1];
        float z = mVirtualAccelerationValues[2];
        float timestamp = mTimestamp;

        if(isLogging) {
            pdAlgorithm(timestamp, x, y, z);
        }
    }

    private void pdAlgorithm(float timestamp, float x, float y, float z) {
        mDataFrame.addRow(new AccData(x, y, z, timestamp));
        finderCurrentTime   = timestamp;
        float deltaTime     = finderCurrentTime - finderStartTime;
        float winSize       = mPreferenceManager.getWinSize();
        float smWinSize     = mPreferenceManager.getSmWinSize();

        // wait for cooldown delay
        if (defectFound) {
            if (deltaTime <= mPreferenceManager.getCoolDownTime()) {
                return;
            } else {
                defectFound = false;
            }
        }

        // don't start working until it has enough data
        if (finderCurrentTime <= winSize) {
            return;
        }

        if (deltaTime >= smWinSize) {
            new VirtualOFinderActivity.FinderTask(finderStartTime, finderCurrentTime, lastLatitude, lastLongitude).execute(mDataFrame.clone());
            finderStartTime = finderCurrentTime;
        }
    }

    protected void onDefectFound(PotholeDataFrame oneWin, PotholeDataFrame win, PotholeDataFrame smWin, float stime, float ctime, float longitude, float latitude) {
        Log.d(LOG_TAG, String.format("A defect was found between ti: %.4f and tf: %.4f", stime, ctime));
        sendToast(defectFoundMsg);
    }

    protected void onDefectFound(float startTime, float currentTime, float longitude, float latitude) {
        Log.d(LOG_TAG, String.format("A defect was found between ti: %.4f and tf: %.4f", startTime, currentTime));
        sendToast(defectFoundMsg);
    }

    private class FinderObject {
        private float startTime;
        private float currentTime;
        private float latitude;
        private float longitude;
        private boolean defectFound;

        public FinderObject(float startTime, float currentTime, float latitude, float longitude, boolean defectFound) {
            this.startTime = startTime;
            this.currentTime = currentTime;
            this.defectFound = defectFound;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public float getStartTime() {
            return startTime;
        }

        public float getCurrentTime() {
            return currentTime;
        }

        public boolean wasDefectFound() {
            return defectFound;
        }

        public void setDefectFound(boolean defectFound) {
            this.defectFound = defectFound;
        }

        public float getLatitude() {
            return latitude;
        }

        public void setLatitude(float latitude) {
            this.latitude = latitude;
        }

        public float getLongitude() {
            return longitude;
        }

        public void setLongitude(float longitude) {
            this.longitude = longitude;
        }
    }

    private class FinderTask extends AsyncTask<PotholeDataFrame, Integer, VirtualOFinderActivity.FinderObject> {
        private float startTime;
        private float currentTime;
        private float latitude;
        private float longitude;

        public FinderTask(float startTime, float currentTime, float lastLatitude, float lastLongitude) {
            this.startTime = startTime;
            this.currentTime = currentTime;
            this.latitude = lastLatitude;
            this.longitude = lastLongitude;
        }

        @Override
        protected VirtualOFinderActivity.FinderObject doInBackground(PotholeDataFrame... params) {
            VirtualOFinderActivity.FinderObject finderObject = new VirtualOFinderActivity.FinderObject(startTime, currentTime, latitude, longitude, false);
            float winSize = mPreferenceManager.getWinSize();
            float smWinSize = mPreferenceManager.getSmWinSize();

            PotholeDataFrame oneWin = params[0].query(currentTime - winSize, currentTime);
            PotholeDataFrame win = oneWin.query(currentTime - winSize, currentTime - smWinSize);
            PotholeDataFrame smWin = oneWin.query(currentTime - smWinSize, currentTime);

            float one_x_mean = (float) oneWin.computeMean(Defect.Axis.AXIS_X);
            float one_x_std = (float) oneWin.computeStd(Defect.Axis.AXIS_X, one_x_mean);

            float sm_z_mean = (float) smWin.computeMean(Defect.Axis.AXIS_Z);
            float sm_z_std = (float) smWin.computeStd(Defect.Axis.AXIS_Z, sm_z_mean);

            one_x_std = (float) MathHelper.round(one_x_std, AppSettings.NDIGITS);
            sm_z_std = (float) MathHelper.round(sm_z_std, AppSettings.NDIGITS);

            one_x_std = (float) MathHelper.round(one_x_std, AppSettings.NDIGITS);
            sm_z_std = (float) MathHelper.round(sm_z_std, AppSettings.NDIGITS);

            if(one_x_std < mPreferenceManager.getxStdThresh() || sm_z_std < mPreferenceManager.getzStdThresh()) {
                return finderObject;
            }

            float mean = (float) win.computeMean(Defect.Axis.AXIS_Z);
            float std = (float) win.computeStd(Defect.Axis.AXIS_Z, mean);
            // dynamic thresh
            float thresh = (float) MathHelper.round(mean + (mPreferenceManager.getK() * std), AppSettings.NDIGITS);
            float z_max = (float) MathHelper.round(smWin.computeMax(), AppSettings.NDIGITS);

            if (z_max >= thresh) {
                defectFound = true;
                finderObject.setDefectFound(true);
            }

            return finderObject;
        }

        @Override
        protected void onPostExecute(VirtualOFinderActivity.FinderObject df) {
            if(df.wasDefectFound()) {
                onDefectFound(df.getStartTime(), df.getCurrentTime(), df.getLongitude(), df.getLatitude());
                Log.d(LOG_TAG, String.format("A defect was found between ti: %.4f and tf: %.4f", df.getStartTime(), df.getCurrentTime()));
            }
        }
    }
}
