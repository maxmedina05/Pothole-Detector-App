package com.medmax.potholedetector.main;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.medmax.potholedetector.R;
import com.medmax.potholedetector.multithreading.ThreadPoolManager;
import com.medmax.potholedetector.utilities.PotholeDbHelper;
import com.medmax.potholedetector.utilities.TimeHelper;

public class AnalyzerActivity extends Activity implements View.OnClickListener, SensorEventListener {

    // constants
    public static final String LOG_TAG = AnalyzerActivity.class.getSimpleName();
    private int SAMPLING_RATE = SensorManager.SENSOR_DELAY_FASTEST;

    // Variables
    private boolean mIsSaving = false;
    private PotholeDbHelper mDbHelper;
    private String mDeviceName;

    private int count = 0;
    private float startTime = 0;
    private float timestamp = 0;
    protected float hz = 0;

    // UI Components
    private ToggleButton mButton;
    private TextView mTvAccAxisX;
    private TextView mTvAccAxisY;
    private TextView mTvAccAxisZ;
    private TextView mTvSamplingRate;

    // Sensor's variables
    private SensorManager mSensorManager;
    private Sensor mSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sensor);

        mDeviceName = Build.MANUFACTURER + " " + Build.MODEL;

        mButton = (ToggleButton) findViewById(R.id.btnToggle);
        mTvAccAxisX = (TextView) findViewById(R.id.acc_axis_x);
        mTvAccAxisY = (TextView) findViewById(R.id.acc_axis_y);
        mTvAccAxisZ = (TextView) findViewById(R.id.acc_axis_z);

        mTvSamplingRate = (TextView) findViewById(R.id.tv_sampling_rate);

        mButton.setOnClickListener(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, SAMPLING_RATE);

        mDbHelper = PotholeDbHelper.getInstance(this.getApplicationContext());

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SAMPLING_RATE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnToggle:
                mIsSaving = !mIsSaving;
//                if(mIsSaving){
//                    mTvStartTime.setText(TimeHelper.getCurrentDateTime("yyyy-MM-dd hh:mm:ss"));
//                } else {
//                    mTvEndTime.setText(TimeHelper.getCurrentDateTime("yyyy-MM-dd hh:mm:ss"));
//                }
                break;
            default:
                break;
        }

    }

    private void updateUI(float x, float y, float z, double f){
        mTvAccAxisX.setText(String.format("%.2f", x));
        mTvAccAxisY.setText(String.format("%.2f", y));
        mTvAccAxisZ.setText(String.format("%.2f", z);
        mTvSamplingRate.setText(String.format("%.2f", f));
    }

    private void calculateSensorFrequency(){
        // Initialize the start time.
        if (startTime == 0)
        {
            startTime = System.nanoTime();
        }

        timestamp = System.nanoTime();

        // Find the sample period (between updates) and convert from
        // nanoseconds to seconds. Note that the sensor delivery rates can
        // individually vary by a relatively large time frame, so we use an
        // averaging technique with the number of sensor updates to
        // determine the delivery rate.
        hz = (count++ / ((timestamp - startTime) / 1000000000.0f));
    }

    private void resetSensorTimer() {
        count = 0;
        startTime = 0;
        timestamp = 0;
        hz = 0;
    }
}
