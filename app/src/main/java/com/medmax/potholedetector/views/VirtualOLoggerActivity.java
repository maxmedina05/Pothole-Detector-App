package com.medmax.potholedetector.views;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.medmax.potholedetector.R;
import com.medmax.potholedetector.config.AppSettings;
import com.medmax.potholedetector.utilities.CSVHelper;
import com.medmax.potholedetector.utilities.DateTimeHelper;
import com.medmax.potholedetector.utilities.FrequencyCalculator;
import com.medmax.potholedetector.utilities.PotholeCSVContract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

/**
 * Created by Max Medina on 2017-08-23.
 */

public class VirtualOLoggerActivity extends Activity implements View.OnClickListener, SensorEventListener {

    // Constants
    public final static String LOG_TAG = VirtualOLoggerActivity.class.getSimpleName();
    private static final int SAMPLING_RATE = SensorManager.SENSOR_DELAY_FASTEST;
    private static final int UPDATE_UI_DELAY = 100;
    private static final float ALPHA = 0.8f;

    // Variables
    protected float mTimestamp = 0;
    private long mLogStartTime = 0;
    protected boolean isLogging = false;

    private FrequencyCalculator fqCal;

    protected volatile float[] mAccel = new float[3];
    protected volatile float[] mMagnet = new float[3];
    protected volatile float[] mGravity = new float[3];
    protected volatile float[] mLinearAcceleration = new float[3];
    protected volatile float[] virtualAccel = new float[3];

    protected int mIdSeed = 0;
    protected String mDeviceName = "";
    protected String defectFoundMsg = "";

    // Helpers
    protected CSVHelper csvHelper;

    // Debugger
    protected boolean isDebuggerOn = false;

    // Preferences
    protected float winSize = 1.0f;
    protected float smWinSize = 0.1f;
    protected float K = 3.0f;
    protected float z_std_thresh = 0.19f;
    protected float x_std_thresh = 0.10f;
    protected float coolDownTime = AppSettings.COOLDOWN_TIME;

    // Sensor properties
    protected SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;

    // Multi thread
    private Handler mHandler;
    private Runnable mRunnable;

    // UI Components
    protected TextView tvTimestamp;
    protected TextView tvFrequency;
    protected TextView tvAxisX;
    protected TextView tvAxisY;
    protected TextView tvAxisZ;
    protected ToggleButton btnLog;

    // Chart Variables
    private GraphView mGraph;
    private LineGraphSeries<DataPoint> seriesX;
    private LineGraphSeries<DataPoint> seriesY;
    private LineGraphSeries<DataPoint> seriesZ;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        defectFoundMsg = getString(R.string.defect_found);
        loadPrefParameters();
        setUILayout();
        setupUIComponents();
        setupSensors();
        setupUIUpdateThread();
        csvHelper = new CSVHelper();
        mDeviceName = Build.MANUFACTURER + " " + Build.MODEL;
        fqCal = new FrequencyCalculator();
    }

    @Override
    protected void onResume() {
        super.onResume();

        fqCal.reset();
        mSensorManager.registerListener(this, mAccelerometerSensor, SAMPLING_RATE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SAMPLING_RATE);

        setupGraph();
    }

    @Override
    protected void onPause() {
        super.onPause();
        btnLog.setChecked(false);
        mSensorManager.unregisterListener(this);
        isLogging = false;
        stopLogger();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                onAccelerometerSensorChanged(event.values);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                onMagneticFieldSensorChanged(event);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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

    protected void onAccelerometerSensorChanged(float[] values) {
        fqCal.calculateFrequency();
//        Log.d(LOG_TAG, String.format("VirtualLogger - X: %.4f | Y: %.4f | Z %.4f", values[0], values[1], values[2]));
        System.arraycopy(values, 0, mAccel, 0, values.length);

        // Isolate the force of mGravity with the low-pass filter.
        mGravity[0] = ALPHA * mGravity[0] + (1 - ALPHA) * values[0];
        mGravity[1] = ALPHA * mGravity[1] + (1 - ALPHA) * values[1];
        mGravity[2] = ALPHA * mGravity[2] + (1 - ALPHA) * values[2];

        // Remove the mGravity contribution with the high-pass filter.
        mLinearAcceleration[0] = values[0] - mGravity[0];
        mLinearAcceleration[1] = values[1] - mGravity[1];
        mLinearAcceleration[2] = values[2] - mGravity[2];

        if (isLogging) {
            float[] rotationMatrix = new float[9];
            if(SensorManager.getRotationMatrix(rotationMatrix, null, values, mMagnet)){
                mTimestamp = (System.currentTimeMillis() - mLogStartTime) / 1000.0f;
                float[] vrm = computeAccelVirtualOrientation(rotationMatrix, mLinearAcceleration);
                System.arraycopy(vrm, 0, virtualAccel, 0, vrm.length);
//                new LogTask(++mIdSeed, mTimestamp, values, mLinearAcceleration, mMagnet, rotationMatrix).execute();
            }
        }
    }

    protected void onMagneticFieldSensorChanged(SensorEvent event) {
        System.arraycopy(event.values, 0, mMagnet, 0, 3);
    }

    protected void updateUI() {
        tvAxisX.setText(String.format(Locale.US, "x: %.4f", virtualAccel[0]));
        tvAxisY.setText(String.format(Locale.US, "y: %.4f", virtualAccel[1]));
        tvAxisZ.setText(String.format(Locale.US, "z: %.4f", virtualAccel[2]));

        tvFrequency.setText(String.format(Locale.US, "%.1f hz", fqCal.getFqHz()));
        tvTimestamp.setText(String.format(Locale.US, "timestamp: %.3f s", mTimestamp));

        seriesX.appendData(new DataPoint(mTimestamp, (double) virtualAccel[0]), true, 400);
        seriesY.appendData(new DataPoint(mTimestamp, (double) virtualAccel[1]), true, 400);
        seriesZ.appendData(new DataPoint(mTimestamp, (double) virtualAccel[2]), true, 400);
    }

    protected void sendToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void loadPrefParameters() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        String key = getResources().getString(R.string.pref_window_size);
        winSize = Float.parseFloat(sharedPrefs.getString(key, "1"));

        key = getResources().getString(R.string.pref_small_window_size);
        smWinSize = Float.parseFloat(sharedPrefs.getString(key, "0.1"));

        key = getResources().getString(R.string.pref_k_value);
        K = Integer.parseInt(sharedPrefs.getString(key, "3"));

        key = getResources().getString(R.string.pref_std_thresh);
        z_std_thresh = Float.parseFloat(sharedPrefs.getString(key, "0.19"));

        key = getResources().getString(R.string.pref_x_std_thresh);
        x_std_thresh = Float.parseFloat(sharedPrefs.getString(key, "0.10"));

        key = getResources().getString(R.string.pref_debugger_mode);
        isDebuggerOn = sharedPrefs.getBoolean(key, false);

        key = getResources().getString(R.string.pref_cooldown_time);
        coolDownTime = Float.parseFloat(sharedPrefs.getString(key, String.valueOf(AppSettings.COOLDOWN_TIME)));
    }

    protected void setUILayout() {
        setContentView(R.layout.activity_virtual_orientation);
    }

    private void setupUIComponents() {
        tvTimestamp = (TextView) findViewById(R.id.tv_timestamp);
        tvFrequency = (TextView) findViewById(R.id.tv_frequency);
        tvAxisX = (TextView) findViewById(R.id.tv_x_axis);
        tvAxisY = (TextView) findViewById(R.id.tv_y_axis);
        tvAxisZ = (TextView) findViewById(R.id.tv_z_axis);
        btnLog = (ToggleButton) findViewById(R.id.btn_log);
        btnLog.setOnClickListener(this);

        mGraph = (GraphView) findViewById(R.id.graph);
    }

    private void setupUIUpdateThread() {
        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRunnable = this;
                updateUI();
                mHandler.postDelayed(mRunnable, UPDATE_UI_DELAY);
            }
        }, UPDATE_UI_DELAY);
    }

    private void setupSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void initLogger() {
        mLogStartTime = System.currentTimeMillis();
        mIdSeed = 0;
        if (!isDebuggerOn) {
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

        setupGraph();
    }

    private void stopLogger() {
        if (!isDebuggerOn && csvHelper.isOpen()) {
            try {
                sendToast(String.format("file %s was created", csvHelper.getCurrentFileName()));
                csvHelper.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mGraph.removeAllSeries();
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

    private void setupGraph() {
        seriesX = new LineGraphSeries<>();
        seriesY = new LineGraphSeries<>();
        seriesZ = new LineGraphSeries<>();

        seriesX.setColor(Color.BLUE);
        seriesY.setColor(Color.RED);
        seriesZ.setColor(Color.GREEN);

        mGraph.addSeries(seriesX);
        mGraph.addSeries(seriesY);
        mGraph.addSeries(seriesZ);

        mGraph.getViewport().setXAxisBoundsManual(true);
        mGraph.getViewport().setYAxisBoundsManual(true);
        mGraph.getViewport().setMinX(0);
        mGraph.getViewport().setMaxX(10);

        mGraph.getViewport().setMinY(-2);
        mGraph.getViewport().setMaxY(2);
    }

    protected void onVirtualAccelChanged(float[] virtualAccel) {

    }

    private float[] computeAccelVirtualOrientation(float[] rotationMatrix, float[] linearAcceleration) {
        float r[] = new float[3];
        float l[] = linearAcceleration;
        float m[][] = invertRotationMatrix(rotationMatrix);

        // multiply inverse rotation matrix with linear acceleration
        r[0] = l[0] * m[0][0] + l[1] * m[1][0] + l[2] * m[2][0];
        r[1] = l[0] * m[0][1] + l[1] * m[1][1] + l[2] * m[2][1];
        r[2] = l[0] * m[0][2] + l[1] * m[1][2] + l[2] * m[2][2];

        return r;
    }

    //    Invert this matrix
    //            /  M[ 0]   M[ 1]   M[ 2]  \
    //            |  M[ 3]   M[ 4]   M[ 5]  |
    //            \  M[ 6]   M[ 7]   M[ 8]  /
    private float[][] invertRotationMatrix(float[] matrix) {
        float[][] result = new float[3][3];
        // first build matrix
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

//    private class LogTask extends AsyncTask<Object, Object, float[]> {
//        private long logId;
//        private float timeStamp;
//
//        private float[] data;
//        private float[] linearAcceleration;
//        private float[] magnet;
//        private float[] rotationMatrix;
//
//        public LogTask(long logId, float timeStamp, float[] data, float[] linearAcceleration, float[] magnet, float[] rotationMatrix) {
//            this.logId = logId;
//            this.timeStamp = timeStamp;
//            this.data = data;
//            this.linearAcceleration = linearAcceleration;
//            this.magnet = magnet;
//            this.rotationMatrix = rotationMatrix;
//        }
//
//        @Override
//        protected float[] doInBackground(Object... params) {
//            float[] fixedAccel = computeAccelVirtualOrientation(rotationMatrix);
//            try {
//                logData(timeStamp, logId, fixedAccel);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            float[] accel = new float[4];
////            accel[0] = virtualAccel[0];
////            accel[1] = virtualAccel[1];
////            accel[2] = virtualAccel[2];
////            accel[3] = timeStamp;
//
//            return accel;
//        }
//
//        protected void onPostExecute(float[] result) {
////            Log.d(LOG_TAG, "LogTask - Log saved");
////            System.arraycopy(result, 0, virtualAccel, 0, result.length - 1);
////            onVirtualAccelChanged(result);
//        }
//    }
}
