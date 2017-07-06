package com.medmax.potholedetector.main;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.medmax.potholedetector.R;
import com.medmax.potholedetector.multithreading.ThreadPoolManager;
import com.medmax.potholedetector.utilities.AppSettings;
import com.medmax.potholedetector.utilities.DateTimeHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Created by maxme on 2017-06-29.
 */

public class AccelerometerActivity extends Activity implements SensorEventListener, View.OnClickListener, Runnable {

    // constants
    public static final String LOG_TAG = AccelerometerActivity.class.getSimpleName();
    private int SAMPLING_RATE = SensorManager.SENSOR_DELAY_FASTEST;

    // Variables
    protected volatile float[] acceleration = new float[3];
    private boolean isSaving = false;
    // The idSeed of the log output
    private int idSeed = 0;

    // for calculating the frequency
    private int count = 0;
    private float startTime = 0;
    private float currentTime = 0;
    protected float hz = 0;
    private float mTimestamp = 0;

    // UI Components
    private ToggleButton mButton;
    private TextView mTvAccAxisX;
    private TextView mTvAccAxisY;
    private TextView mTvAccAxisZ;
    private TextView mTvSamplingRate;
    private TextView mTvTimestamp;

    // Sensor's variables
    private SensorManager mSensorManager;

    // Handler for the UI plots so everything plots smoothly
    private Handler mHandler;
    private Runnable mRunnable;
    private Thread mThread;
    private ThreadPoolManager mThreadPool;
    private String mDeviceName;
    private File mFile;

    private long logTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyzer);

        mButton = (ToggleButton) findViewById(R.id.btnToggle);
        mTvAccAxisX = (TextView) findViewById(R.id.acc_axis_x);
        mTvAccAxisY = (TextView) findViewById(R.id.acc_axis_y);
        mTvAccAxisZ = (TextView) findViewById(R.id.acc_axis_z);
        mTvSamplingRate = (TextView) findViewById(R.id.tv_sampling_rate);
        mTvTimestamp = (TextView) findViewById(R.id.tv_timestamp);

        mButton.setOnClickListener(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mHandler = new Handler();

        mRunnable = new Runnable() {
            @Override
            public void run() {
                mHandler.postDelayed(this, 10);
                updateUI();
            }
        };
        mThreadPool = ThreadPoolManager.getsInstance();
        mDeviceName = Build.MANUFACTURER + " " + Build.MODEL;
        String fileName = AppSettings.ACCELEROMETER_RAW_DATA_CSV_FILE + DateTimeHelper.getCurrentDateTime("yyyyMMdd_hhmmss") + ".csv";

        File exportDir = new File(Environment.getExternalStorageDirectory(), AppSettings.POTHOLE_DIRECTORY);
        if(!exportDir.exists()){
            exportDir.mkdir();
        }

        mFile = new File(
                exportDir,
                fileName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetSensorTimer();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SAMPLING_RATE);
        mHandler.post(mRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        mHandler.removeCallbacks(mRunnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            calculateSensorFrequency();
            System.arraycopy(event.values, 0, acceleration, 0, event.values.length);

            if (idSeed == 0)
            {
                logTime = System.currentTimeMillis();
            }
            // Store the data in SQL using a background process.
//            final long timeStamp =  System.currentTimeMillis() + ((event.timestamp - SystemClock.elapsedRealtimeNanos())/1000000L);
            final float timeStamp = (System.currentTimeMillis() - logTime) / 1000.0f;

            if(isSaving) {
                mTimestamp = timeStamp;
                mThreadPool.addCallable(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        saveDataToCSV(++idSeed, timeStamp, mDeviceName, acceleration[0], acceleration[1], acceleration[2]);
                        Log.i(LOG_TAG, "onSensorChanged - background task done!");
                        return null;
                    }
                });
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnToggle:
                handleOnSavingToggle();
                break;
            default:
                break;
        }
    }

    @Override
    public void run() {
        Thread.currentThread().interrupt();
    }

    private void handleOnSavingToggle() {
        isSaving = !isSaving;
        mTimestamp = 0;
        resetSensorTimer();
        idSeed = 0;
    }

    private void updateUI() {
        mTvAccAxisX.setText(String.format(Locale.US, "%.2f", acceleration[0]));
        mTvAccAxisY.setText(String.format(Locale.US, "%.2f", acceleration[1]));
        mTvAccAxisZ.setText(String.format(Locale.US, "%.2f", acceleration[2]));
        mTvSamplingRate.setText(String.format(Locale.US, "%.2f", hz));
        mTvTimestamp.setText(String.format(Locale.US, "%.4f", mTimestamp));
    }

    private synchronized void saveDataToCSV(int idCount, float timestamp, String deviceName, float x, float y, float z) {
        try (BufferedWriter bwriter = new BufferedWriter(new FileWriter(mFile, true))) {

            if(idCount == 0) {
                bwriter.write("ID,Date,Timestamp,DeviceName,X-Axis,Y-Axis,Z-Axis");
            }
            else {
                bwriter.write(String.format(
                        Locale.US,
                        "%d,%s,%f,%s,%f,%f,%f",
                        idCount,
                        DateTimeHelper.getCurrentDateTime("yyyy-MM-dd hh:mm:ss.SSS"),
                        timestamp,
                        deviceName,
                        x,
                        y,
                        z)
                );
            }
            bwriter.newLine();
            bwriter.close();

        } catch (IOException e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    private void calculateSensorFrequency() {
        if (startTime == 0) {
            startTime = System.nanoTime();
        }
        currentTime = System.nanoTime();
        hz = (count++ / ((currentTime - startTime) / 1000000000.0f));
    }

    private void resetSensorTimer() {
        count = 0;
        startTime = 0;
        currentTime = 0;
        hz = 0;
    }


}
