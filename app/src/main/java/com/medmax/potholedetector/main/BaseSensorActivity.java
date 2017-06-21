package com.medmax.potholedetector.main;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.medmax.potholedetector.R;
import com.medmax.potholedetector.data.AccelerometerDataContract;
import com.medmax.potholedetector.utilities.DateTimeHelper;
import com.medmax.potholedetector.utilities.PotholeDbHelper;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by Max Medina on 2017-06-20.
 */

public abstract class BaseSensorActivity extends Activity implements View.OnClickListener, SensorEventListener, Runnable {
    private int SAMPLING_RATE = SensorManager.SENSOR_DELAY_FASTEST;
    private static final String LOG_TAG = BaseSensorActivity.class.getSimpleName();

    protected volatile float[] linearAcceleration = new float[3];
    protected volatile float[] acceleration = new float[3];

    // Variables
    private boolean isSaving = false;
    protected PotholeDbHelper mDbHelper;
    protected String mDeviceName;
    private long mTimestamp = 0;

    // for calculating the frequency
    private int count = 0;
    private float startTime = 0;
    private float currentTime = 0;
    protected float hz = 0;

    // UI Components
    private ToggleButton mButton;
    private TextView mTvAccAxisX;
    private TextView mTvAccAxisY;
    private TextView mTvAccAxisZ;
    private TextView mTvSamplingRate;

    // Sensor's variables
    private SensorManager mSensorManager;

    // Handler for the UI plots so everything plots smoothly
    private Handler mHandler;
    private Runnable mRunnable;
    private Thread mThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyzer);

        mButton = (ToggleButton) findViewById(R.id.btnToggle);
        mTvAccAxisX = (TextView) findViewById(R.id.acc_axis_x);
        mTvAccAxisY = (TextView) findViewById(R.id.acc_axis_y);
        mTvAccAxisZ = (TextView) findViewById(R.id.acc_axis_z);
        mTvSamplingRate = (TextView) findViewById(R.id.tv_sampling_rate);

        mButton.setOnClickListener(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mDbHelper = PotholeDbHelper.getInstance(this.getApplicationContext());

        mHandler = new Handler();

        mRunnable = new Runnable() {
            @Override
            public void run() {
                mHandler.postDelayed(this, 10);
                updateUI();
            }
        };

        configInit();
    }

    protected void configInit() {
        mDeviceName = Build.MANUFACTURER + " " + Build.MODEL;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SAMPLING_RATE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SAMPLING_RATE);
        resetSensorTimer();
        mHandler.post(mRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        mHandler.removeCallbacks(mRunnable);
    }

    @Override
    protected void onDestroy() {
        mDbHelper.close();
        super.onDestroy();
    }

    @Override
    public void run() {
        while (isSaving && !Thread.currentThread().isInterrupted()) {
            logData();
        }
        Thread.currentThread().interrupt();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            calculateSensorFrequency();
            mTimestamp = System.currentTimeMillis();
            System.arraycopy(event.values, 0, acceleration, 0, event.values.length);
        }

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            System.arraycopy(event.values, 0, linearAcceleration, 0, event.values.length);
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

    private void handleOnSavingToggle() {
        isSaving = !isSaving;

        if (isSaving) {
            startLog();
        } else {
            stopLog();
        }
    }

    private void updateUI() {
        mTvAccAxisX.setText(String.format(Locale.US, "%.2f", acceleration[0]));
        mTvAccAxisY.setText(String.format(Locale.US, "%.2f", acceleration[1]));
        mTvAccAxisZ.setText(String.format(Locale.US, "%.2f", acceleration[2]));
        mTvSamplingRate.setText(String.format(Locale.US, "%.2f", hz));
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

    protected void logData() {
        Log.d(LOG_TAG, "Logging accelerometer data!");

        if(mDbHelper != null) {
            ContentValues values = new ContentValues();
            values.put(AccelerometerDataContract.AccelerometerReading.COLUMN_NAME_TIME, mTimestamp);
            values.put(AccelerometerDataContract.AccelerometerReading.COLUMN_NAME_DEVICE_NAME, mDeviceName);
            values.put(AccelerometerDataContract.AccelerometerReading.COLUMN_NAME_DATE_CREATED, DateTimeHelper.getFormatDate(mTimestamp, "yyyy-MM-dd hh:mm:ss"));
            values.put(AccelerometerDataContract.AccelerometerReading.COLUMN_NAME_ACC_X_AXIS, acceleration[0]);
            values.put(AccelerometerDataContract.AccelerometerReading.COLUMN_NAME_ACC_Y_AXIS, acceleration[1]);
            values.put(AccelerometerDataContract.AccelerometerReading.COLUMN_NAME_ACC_Z_AXIS, acceleration[2]);

            mDbHelper.getWritableDatabase().insert(AccelerometerDataContract.AccelerometerReading.TABLE_NAME, null, values);
        }
    }

    private void startLog() {
        mThread = new Thread(BaseSensorActivity.this);
        mThread.start();
    }

    private void stopLog() {
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
    }
}
