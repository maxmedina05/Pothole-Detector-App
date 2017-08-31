package com.medmax.potholedetector.views;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

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
    private float stime = 0;
    private float ctime = 0;

    // Debugger fields
    private BufferedReader mReader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDataFrame = new PotholeDataFrame();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isDebuggerOn) {
            loadDataFromCSV();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mReader != null) {
            try {
                mReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onAccelerometerSensorChanged(float[] values) {
        float[] nvalues = values;

        if (isLogging && isDebuggerOn) {
            String line = "";
            nvalues = new float[3];
            try {
                if ((line = mReader.readLine()) != null) {
                    String[] row = line.split(",");
                    nvalues[0] = Float.parseFloat(row[4]);
                    nvalues[1] = Float.parseFloat(row[5]);
                    nvalues[2] = Float.parseFloat(row[6]);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onAccelerometerSensorChanged(nvalues);
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

//    @Override
    protected void onVirtualAccelChanged(float[] accel) {

        mDataFrame.addRow(new AccData(accel[0], accel[1], accel[2], accel[3]));
        ctime = accel[3];
        float deltaTime = ctime - stime;

        // wait for cooldown delay
        if (defectFound) {
            if (deltaTime <= coolDownTime) {
                return;
            } else {
                defectFound = false;
            }
        }

        // don't start working until it has enough data
        if (ctime > winSize) {
            new FinderTask(stime, ctime).execute(mDataFrame.clone());
        }
        stime = ctime;
    }

    private void PdAlgorithm(float timestamp, float x, float y, float z) {
        mDataFrame.addRow(new AccData(x, y, z, timestamp));
        ctime = timestamp;
        float deltaTime = ctime - stime;

        // wait for cooldown delay
        if (defectFound) {
            if (deltaTime <= coolDownTime) {
                return;
            } else {
                defectFound = false;
                stime = ctime;
            }
        }

        // don't start working until it has enough data
        if (ctime <= winSize) {
            return;
        }

        if (deltaTime >= smWinSize) {
            PotholeDataFrame oneWin = mDataFrame.query(ctime - winSize, ctime);
            PotholeDataFrame win = oneWin.query(ctime - winSize, ctime - smWinSize);
            PotholeDataFrame smWin = oneWin.query(ctime - smWinSize, ctime);

            float one_x_mean = (float) oneWin.computeMean(Defect.Axis.AXIS_X);
            float one_x_std = (float) oneWin.computeStd(Defect.Axis.AXIS_X, one_x_mean);

            float sm_z_mean = (float) smWin.computeMean(Defect.Axis.AXIS_Z);
            float sm_z_std = (float) smWin.computeStd(Defect.Axis.AXIS_Z, sm_z_mean);

            one_x_std = (float) MathHelper.round(one_x_std, AppSettings.NDIGITS);
            sm_z_std = (float) MathHelper.round(sm_z_std, AppSettings.NDIGITS);

            if (one_x_std < x_std_thresh || sm_z_std < z_std_thresh) {
                stime = ctime;
                return;
            }

            float mean = (float) win.computeMean(Defect.Axis.AXIS_Z);
            float std = (float) win.computeStd(Defect.Axis.AXIS_Z, mean);
            // dynamic thresh
            float thresh = (float) MathHelper.round(mean + (K * std), AppSettings.NDIGITS);
            float z_max = (float) MathHelper.round(smWin.computeMax(), AppSettings.NDIGITS);

            if (z_max >= thresh) {
                defectFound = true;
                // TODO: add gps coor
//                onDefectFound(oneWin, win, smWin, stime, ctime, 0, 0);
                onDefectFound(stime, ctime, 0, 0);
            }
            stime = ctime;
        }
    }

    protected void onDefectFound(float stime, float ctime, float longitude, float latitude) {
        Log.d(LOG_TAG, String.format("A defect was found between ti: %.4f and tf: %.4f", stime, ctime));
        sendToast(defectFoundMsg);
    }

    private class DefectFound {
        private float startTime;
        private float currentTime;
        private boolean defectFound;

        public DefectFound(float startTime, float currentTime, boolean defectFound) {
            this.startTime = startTime;
            this.currentTime = currentTime;
            this.defectFound = defectFound;
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
    }

    private class FinderTask extends AsyncTask<PotholeDataFrame, Integer, DefectFound> {

        private float startTime;
        private float currentTime;

        public FinderTask(float startTime, float currentTime) {
            this.startTime = startTime;
            this.currentTime = currentTime;
        }

        @Override
        protected DefectFound doInBackground(PotholeDataFrame... params) {
            DefectFound defectFound = new DefectFound(startTime, currentTime, false);

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

            if (one_x_std < x_std_thresh || sm_z_std < z_std_thresh) {
                return defectFound;
            }

            float mean = (float) win.computeMean(Defect.Axis.AXIS_Z);
            float std = (float) win.computeStd(Defect.Axis.AXIS_Z, mean);
            // dynamic thresh
            float thresh = (float) MathHelper.round(mean + (K * std), AppSettings.NDIGITS);
            float z_max = (float) MathHelper.round(smWin.computeMax(), AppSettings.NDIGITS);

            if (z_max >= thresh) {
                defectFound.setDefectFound(true);
            }

            return defectFound;
        }

        @Override
        protected void onPostExecute(DefectFound df) {
            if(df.wasDefectFound()) {
                onDefectFound(df.getStartTime(), df.getCurrentTime(), 0, 0);
            }
        }
    }
}
