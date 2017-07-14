package com.medmax.potholedetector.views;

import android.content.SharedPreferences;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.medmax.potholedetector.BaseSensorActivity;
import com.medmax.potholedetector.R;
import com.medmax.potholedetector.config.AppSettings;
import com.medmax.potholedetector.data.analyzer.PotholeDataFrame;
import com.medmax.potholedetector.data.analyzer.PotholeFinder;
import com.medmax.potholedetector.models.AccData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Max Medina on 2017-07-06.
 */

public class FinderActivity extends BaseSensorActivity {
    // Constants
    public final static String LOG_TAG = FinderActivity.class.getSimpleName();

    // Analyzer
    private PotholeFinder mFinder;
    private int mSelectedAlgorithm = 100;
    private PotholeDataFrame mDataFrame;

    private float stime = 0;
    private float ctime = 0;

    // Preferences
    float winSize = 0.5f;
    float smWinSize = 0.1f;
    float K = 3.0f;
    float std_thresh = 0.19f;

    // Debugger fields
    private BufferedReader mReader;
    private boolean isDebuggerOn = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFinder = new PotholeFinder();
        mDataFrame = new PotholeDataFrame();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String key = getResources().getString(R.string.pref_zthresh_key);

        float ztreshv = Float.parseFloat(sharedPrefs.getString(key, "1.4"));
        mFinder.setzThreshValue(ztreshv);

        key = getResources().getString(R.string.pref_algorithm_list_key);
        mSelectedAlgorithm = Integer.parseInt(sharedPrefs.getString(key, "100"));

        loadRobsAlgorithmParameters(sharedPrefs);

        // TODO: Remove log
        Log.d(LOG_TAG, String.format("Selected Algorithm: %d", mSelectedAlgorithm));
    }

    private void loadRobsAlgorithmParameters(SharedPreferences sharedPrefs) {
        String key;
        key = getResources().getString(R.string.pref_window_size);
        winSize = Float.parseFloat(sharedPrefs.getString(key, "1"));

        key = getResources().getString(R.string.pref_small_window_size);
        smWinSize = Float.parseFloat(sharedPrefs.getString(key, "0.1"));

        key = getResources().getString(R.string.pref_k_value);
        K = Integer.parseInt(sharedPrefs.getString(key, "3"));

        key = getResources().getString(R.string.pref_std_thresh);
        std_thresh = Float.parseFloat(sharedPrefs.getString(key, "0.19"));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(isDebuggerOn){
            loadDataFromCSV();
        }
    }

    private void loadDataFromCSV() {
        File downloadsDir = new File(Environment.getExternalStorageDirectory(), "Download");
        File file = new File(downloadsDir, "dataset_jeepeta.csv");
        try {
            mReader = new BufferedReader(new FileReader(file));
            mReader.readLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mReader != null) {
            try {
                mReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void myOnClick(View v) {
        if(mStartLogger) {
            stime = 0;
        }
    }

    @Override
    public void myOnSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float timestamp = mTimestamp;

        if(mStartLogger && isDebuggerOn) {
            String line = "";
            try {
                if((line = mReader.readLine()) != null) {
                    String[] row = line.split(",");
                    timestamp   = Float.parseFloat(row[2]);
                    x           = Float.parseFloat(row[4]);
                    y           = Float.parseFloat(row[5]);
                    z           = Float.parseFloat(row[6]);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(mStartLogger) {
             x /= AppSettings.GRAVITY_CONSTANT;
             y /= AppSettings.GRAVITY_CONSTANT;
             z /= AppSettings.GRAVITY_CONSTANT;

            switch (mSelectedAlgorithm) {
                case EnumAlgorithm.Z_THRESH_ALGORITHM:
                    doZThreshAlgorithm(z);
                    break;
                case EnumAlgorithm.ROBS_ALGORITHM:
                    doRobsAlgorithm(timestamp, x, z);
                    break;
            }
        }
    }


    private void doZThreshAlgorithm(float z) {
        mFinder.handleState(z);
        if(mFinder.isThereAPothole()){
            Log.d(LOG_TAG, "I Pothole was found!");
            sendToast("I Pothole was found!");
        }
    }

    private void doRobsAlgorithm(float timestamp, float x, float z) {
        mDataFrame.addRow(new AccData(x, 0, z, timestamp));
        ctime = timestamp;
//        Log.d(LOG_TAG, String.format("stime: %.4f| ctime: %.4f", stime, ctime));

        if(ctime <= winSize){
            return;
        }

        if((ctime - stime) >= smWinSize) {
            PotholeDataFrame win    = mDataFrame.query(ctime - winSize, ctime - smWinSize);
            PotholeDataFrame smWin  = mDataFrame.query(ctime - smWinSize, ctime);

            float wmean = (float) win.computeMean();
            float wstd = (float) win.computeStd();
            float wceil = wmean + K*wstd;

            float smw_max = (float) smWin.computeMax();
            float smw_std = (float) smWin.computeStd();

            if(smw_max > wceil && smw_std > std_thresh) {
                onDefectFound(win, smWin, stime, ctime);
            }

            stime = ctime;
        }
    }

    protected void onDefectFound(PotholeDataFrame win, PotholeDataFrame smWin, float stime, float ctime) {
        Log.d(LOG_TAG, String.format("Something was found between ti: %.4f and tf: %.4f", stime, ctime));
        sendToast("I Pothole was found!");
    }

    private static class EnumAlgorithm {
        static final int Z_THRESH_ALGORITHM = 100;
        static final int ROBS_ALGORITHM = 200;
    }
}
