package com.medmax.potholedetector.main;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.medmax.potholedetector.R;
import com.medmax.potholedetector.data.AccelerometerDataContract;
import com.medmax.potholedetector.utilities.PotholeDbHelper;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class AnalyzerActivity extends Activity implements View.OnClickListener, SensorEventListener, Runnable {
    // constants
    public static final String LOG_TAG = AnalyzerActivity.class.getSimpleName();
    private int SAMPLING_RATE = SensorManager.SENSOR_DELAY_FASTEST;

    // Variables
    protected volatile float[] acceleration = new float[3];
    private boolean isSaving = false;
    private PotholeDbHelper mDbHelper;
    private String mDeviceName;
    private DecimalFormat df;
    private long mTimestamp = 0;

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
    private TextView mTvTimestamp;

    // Sensor's variables
    private SensorManager mSensorManager;
    private Sensor mSensor;

    // Handler for the UI plots so everything plots smoothly
    protected Handler mHandler;
    protected Runnable mRunnable;
    private Thread mThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_analyzer);

        mDeviceName = Build.MANUFACTURER + " " + Build.MODEL;
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        df = (DecimalFormat) nf;
        df.applyPattern("###.####");

        mButton = (ToggleButton) findViewById(R.id.btnToggle);
        mTvAccAxisX = (TextView) findViewById(R.id.acc_axis_x);
        mTvAccAxisY = (TextView) findViewById(R.id.acc_axis_y);
        mTvAccAxisZ = (TextView) findViewById(R.id.acc_axis_z);
        mTvTimestamp = (TextView) findViewById(R.id.tv_timestamp);
        mTvSamplingRate = (TextView) findViewById(R.id.tv_sampling_rate);

        mButton.setOnClickListener(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, SAMPLING_RATE);

        mDbHelper = PotholeDbHelper.getInstance(this.getApplicationContext());

        mHandler = new Handler();

        mRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                mHandler.postDelayed(this, 10);
                updateUI();
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        mHandler.removeCallbacks(mRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SAMPLING_RATE);
        resetSensorTimer();
        mHandler.post(mRunnable);
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

            mTimestamp =  System.currentTimeMillis();
//            Log.d(LOG_TAG, "timestamp: " + Long.toString(timestamp));
            System.arraycopy(event.values, 0, acceleration, 0, event.values.length);
//            updateUI(event.values[0], event.values[1], event.values[2], hz, timestamp);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnToggle:
                isSaving = !isSaving;
                if(isSaving){
                    startLog();
                }
                else{
                    stopLog();
                }
                break;
            default:
                break;
        }
    }

//    private void updateUI(float x, float y, float z, float f, long t){
//        mTvAccAxisX.setText(String.format("%.2f", x));
//        mTvAccAxisY.setText(String.format("%.2f", y));
//        mTvAccAxisZ.setText(String.format("%.2f", z));
//        mTvSamplingRate.setText(String.format("%.2f", f));
//        mTvTimestamp.setText(Long.toString(t));
//    }

    private void updateUI(){
        mTvAccAxisX.setText(String.format("%.2f", acceleration[0]));
        mTvAccAxisY.setText(String.format("%.2f", acceleration[1]));
        mTvAccAxisZ.setText(String.format("%.2f", acceleration[2]));
        mTvSamplingRate.setText(String.format("%.2f", hz));
        mTvTimestamp.setText(Long.toString(mTimestamp));
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

    private void logData(){
        Log.d(LOG_TAG, "LogData - Storing Data in database!");
        if(mDbHelper != null) {
            ContentValues values = new ContentValues();
            values.put(AccelerometerDataContract.AccelerometerReading.COLUMN_NAME_TIME, timestamp);
            values.put(AccelerometerDataContract.AccelerometerReading.COLUMN_NAME_DEVICE_NAME, mDeviceName);
            values.put(AccelerometerDataContract.AccelerometerReading.COLUMN_NAME_ACC_X_AXIS, acceleration[0]);
            values.put(AccelerometerDataContract.AccelerometerReading.COLUMN_NAME_ACC_Y_AXIS, acceleration[1]);
            values.put(AccelerometerDataContract.AccelerometerReading.COLUMN_NAME_ACC_Z_AXIS, acceleration[2]);

            long newId = mDbHelper.getWritableDatabase().insert(AccelerometerDataContract.AccelerometerReading.TABLE_NAME, null, values);
        }
    }

    private void startLog() {
        mThread = new Thread(AnalyzerActivity.this);
        mThread.start();
    }

    private void stopLog() {
        if (mThread != null)
        {
            mThread.interrupt();
            mThread = null;
        }
    }
}
