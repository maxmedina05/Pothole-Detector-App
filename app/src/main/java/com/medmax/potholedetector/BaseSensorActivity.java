package com.medmax.potholedetector;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Created by Max Medina on 2017-07-06.
 */

public abstract class BaseSensorActivity extends Activity implements View.OnClickListener, SensorEventListener {

    // Constants
    public final static String LOG_TAG = BaseSensorActivity.class.getSimpleName();
    protected static final int SAMPLING_RATE = SensorManager.SENSOR_DELAY_FASTEST;
    protected static final int UPDATE_UI_DELAY = 100;

    // Variables
    protected volatile float[] acc_values = new float[3];
    protected String mDeviceName = "";
    protected float mTimestamp = 0;
    protected boolean startLogger = false;
    protected long loggerStartTime = 0;

    // Sensor properties
    protected SensorManager mSensorManager;
    protected Sensor mAccelerometer;

    // Frequency
    protected int fqCount = 0;
    protected float fqsTime = 0;
    protected float fqcTime = 0;
    protected float fqHz = 0;

    protected Handler mHandler;
    protected Runnable mRunnable;

    // UI Components
    protected TextView tvTimestamp;
    protected TextView tvFrequency;
    protected TextView tvAxisX;
    protected TextView tvAxisY;
    protected TextView tvAxisZ;
    protected ToggleButton btnLog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sensor);

        tvTimestamp = (TextView)findViewById(R.id.tv_timestamp);
        tvFrequency = (TextView)findViewById(R.id.tv_frequency);
        tvAxisX = (TextView)findViewById(R.id.tv_x_axis);
        tvAxisY = (TextView)findViewById(R.id.tv_y_axis);
        tvAxisZ = (TextView)findViewById(R.id.tv_z_axis);

        btnLog = (ToggleButton)findViewById(R.id.btn_log);
        btnLog.setOnClickListener(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRunnable = this;
                updateUI();
                mHandler.postDelayed(mRunnable, UPDATE_UI_DELAY);
            }
        }, UPDATE_UI_DELAY);

        mDeviceName = Build.MANUFACTURER + " " + Build.MODEL;
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetSensorTimer();
        mSensorManager.registerListener(this, mAccelerometer, SAMPLING_RATE);
        mHandler.postDelayed(mRunnable, UPDATE_UI_DELAY);
    }

    @Override
    protected void onPause() {
        btnLog.setChecked(false);
        mSensorManager.unregisterListener(this);
        mHandler.removeCallbacks(mRunnable);
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        startLogger = !startLogger;

        if(startLogger){
            loggerStartTime = System.currentTimeMillis();
        } else {

            mTimestamp = 0;
        }

        myOnClick(v);
    }

    public abstract void myOnClick(View v);

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (Sensor.TYPE_ACCELEROMETER == event.sensor.getType()) {

            if(startLogger){
                mTimestamp = (System.currentTimeMillis() - loggerStartTime) / 1000.0f;
            }

            calculateFrequency();
            System.arraycopy(event.values, 0, acc_values, 0, event.values.length);

            myOnSensorChanged(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public abstract void myOnSensorChanged(SensorEvent event);

    protected void updateUI() {
        // TODO: Remove log
        Log.d(LOG_TAG, "update ui");
        Log.d(LOG_TAG, String.format("x: %.4f, y: %.4f, z: %.4f", acc_values[0], acc_values[1], acc_values[2]));

        tvAxisX.setText(String.format(Locale.US, "x: %.4f", acc_values[0]));
        tvAxisY.setText(String.format(Locale.US, "y: %.4f", acc_values[1]));
        tvAxisZ.setText(String.format(Locale.US, "z: %.4f", acc_values[2]));

        tvFrequency.setText(String.format(Locale.US, "%.1f hz", fqHz));
        tvTimestamp.setText(String.format(Locale.US, "%.3f s", mTimestamp));
    }

    protected void calculateFrequency() {
        if (fqsTime == 0) {
            fqsTime = System.nanoTime();
        }
        fqcTime = System.nanoTime();
        fqHz = (fqCount++ / ((fqcTime - fqsTime) / 1000000000.0f));
    }

    protected void resetSensorTimer() {
        fqCount = 0;
        fqsTime = 0;
        fqcTime = 0;
        fqHz = 0;
        mTimestamp = 0;
    }

    protected void sendToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
