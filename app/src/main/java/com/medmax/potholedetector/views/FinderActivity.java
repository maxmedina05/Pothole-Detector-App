package com.medmax.potholedetector.views;

import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.medmax.potholedetector.BaseSensorActivity;
import com.medmax.potholedetector.config.AppSettings;
import com.medmax.potholedetector.data.analyzer.PotholeDataFrame;
import com.medmax.potholedetector.data.analyzer.PotholeFinder;
import com.medmax.potholedetector.models.AccData;
import com.medmax.potholedetector.models.Defect;
import com.medmax.potholedetector.utilities.MathHelper;

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
    private PotholeDataFrame mDataFrame;

    private boolean defectFound = false;
    private float stime = 0;
    private float ctime = 0;

    // Debugger fields
    private BufferedReader mReader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFinder = new PotholeFinder();
        mDataFrame = new PotholeDataFrame();

        mFinder.setzThreshValue(z_thresh);
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
        File file = new File(downloadsDir, AppSettings.MOCK_DATA_FILENAME);
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
                    timestamp   = Float.parseFloat(row[3]);
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

            switch (selectedAlgorithm) {
                case EnumAlgorithm.Z_THRESH_ALGORITHM:
                    doZThreshAlgorithm(z);
                    break;
                case EnumAlgorithm.ROBS_ALGORITHM:
                    doRobsAlgorithm(timestamp, x, y, z);
                    break;
                case EnumAlgorithm.STDX_ALGORITHM:
                    doStdXAlgorithm(timestamp, x, y, z);
                    break;
            }
        }
    }

    private void doZThreshAlgorithm(float z) {
        mFinder.handleState(z);
        if(mFinder.isThereAPothole()){
            Log.d(LOG_TAG, defectFoundMsg);
            sendToast(defectFoundMsg);
        }
    }

    private void doRobsAlgorithm(float timestamp, float x, float y, float z) {
        mDataFrame.addRow(new AccData(x, y, z, timestamp));
        ctime = timestamp;

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

            if(smw_max > wceil && smw_std > z_std_thresh) {
                onDefectFound(win, win, smWin, stime, ctime, lastLongitude, lastLatitude);
            }

            stime = ctime;
        }
    }

    private void doStdXAlgorithm(float timestamp, float x, float y, float z) {
        mDataFrame.addRow(new AccData(x, y, z, timestamp));
        ctime = timestamp;
        float deltaTime = ctime - stime;

        // wait for cooldown delay
        if(defectFound) {
            if(deltaTime <= coolDownTime) {
                return;
            } else {
                defectFound = false;
                stime = ctime;
            }
        }

        // don't start working until it has enough data
        if(ctime <= winSize){
            return;
        }

        if(deltaTime >= smWinSize) {
            PotholeDataFrame oneWin = mDataFrame.query(ctime-winSize, ctime);
            PotholeDataFrame win    = oneWin.query(ctime - winSize, ctime - smWinSize);
            PotholeDataFrame smWin  = oneWin.query(ctime - smWinSize, ctime);

            float one_x_mean = (float) oneWin.computeMean(Defect.Axis.AXIS_X);
            float one_x_std = (float) oneWin.computeStd(Defect.Axis.AXIS_X, one_x_mean);

            float sm_z_mean = (float) smWin.computeMean(Defect.Axis.AXIS_Z);
            float sm_z_std = (float) smWin.computeStd(Defect.Axis.AXIS_Z, sm_z_mean);

            one_x_std = (float) MathHelper.round(one_x_std, AppSettings.NDIGITS);
            sm_z_std = (float) MathHelper.round(sm_z_std, AppSettings.NDIGITS);

            if(one_x_std < x_std_thresh || sm_z_std < z_std_thresh) {
                stime = ctime;
                return;
            }

            float mean = (float) win.computeMean(Defect.Axis.AXIS_Z);
            float std = (float) win.computeStd(Defect.Axis.AXIS_Z, mean);
            // dynamic thresh
            float thresh = (float) MathHelper.round(mean + (K*std), AppSettings.NDIGITS);
            float z_max = (float) MathHelper.round(smWin.computeMax(), AppSettings.NDIGITS);

            if(z_max >= thresh) {
                defectFound = true;
                onDefectFound(oneWin, win, smWin, stime, ctime, lastLongitude, lastLatitude);
            }
            stime = ctime;
        }
    }

    protected void onDefectFound(PotholeDataFrame oneWin, PotholeDataFrame win, PotholeDataFrame smWin, float stime, float ctime, float longitude, float latitude) {
        Log.d(LOG_TAG, String.format("A defect was found between ti: %.4f and tf: %.4f", stime, ctime));
        sendToast(defectFoundMsg);
    }

    public static class EnumAlgorithm {
        public static final int Z_THRESH_ALGORITHM = 100;
        static final int ROBS_ALGORITHM = 200;
        static final int STDX_ALGORITHM = 250;
    }
}
