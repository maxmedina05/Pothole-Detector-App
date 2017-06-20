package com.medmax.potholedetector.main;

import android.app.Activity;
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
import com.medmax.potholedetector.models.SensorState;
import com.medmax.potholedetector.utilities.AppSettings;
import com.medmax.potholedetector.utilities.PotholeDbHelper;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by Max Medina on 2017-06-11.
 */

public class PotholeDetectorActivity extends Activity implements View.OnClickListener, SensorEventListener, Runnable {
    // constants
    public static final String LOG_TAG = PotholeDetectorActivity.class.getSimpleName();
    private int SAMPLING_RATE = SensorManager.SENSOR_DELAY_FASTEST;

    // Variables
    protected volatile float[] acceleration = new float[3];
    protected volatile float[] linearAcceleration = new float[3];

    private boolean isSaving = false;
    private PotholeDbHelper mDbHelper;
    private String mDeviceName;
    private DecimalFormat df;
    private long mTimestamp = 0;

    // Threshold values
    private float mThresh = 2;

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
//    private Sensor mSensor;

    // Handler for the UI plots so everything plots smoothly
    protected Handler mHandler;
    protected Runnable mRunnable;
    private Thread mThread;

    // Prefs
    SharedPreferences sharedPrefs;

    //State Machine
    private boolean currentState = false;
    private boolean previousState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyzer);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        float thresh = Float.parseFloat(sharedPrefs.getString("pref_thresh", "2"));

        Log.d(LOG_TAG, String.format("THRESH: %.4f", thresh));

        mDeviceName = Build.MANUFACTURER + " " + Build.MODEL;
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        df = (DecimalFormat) nf;
        df.applyPattern("###.####");

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
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SAMPLING_RATE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SAMPLING_RATE);
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
                isSaving = !isSaving;
                if (isSaving) {
                    startLog();
                } else {
                    stopLog();
                }
                break;
            default:
                break;
        }
    }

    private void updateUI() {
        mTvAccAxisX.setText(String.format("%.2f", acceleration[0]));
        mTvAccAxisY.setText(String.format("%.2f", acceleration[1]));
        mTvAccAxisZ.setText(String.format("%.2f", acceleration[2]));
        mTvSamplingRate.setText(String.format("%.2f", hz));

    }

    private void calculateSensorFrequency() {
        // Initialize the start time.
        if (startTime == 0) {
            startTime = System.nanoTime();
        }
        currentTime = System.nanoTime();

        // Find the sample period (between updates) and convert from
        // nanoseconds to seconds. Note that the sensor delivery rates can
        // individually vary by a relatively large time frame, so we use an
        // averaging technique with the number of sensor updates to
        // determine the delivery rate.
        hz = (count++ / ((currentTime - startTime) / 1000000000.0f));
    }

    private void resetSensorTimer() {
        count = 0;
        startTime = 0;
        currentTime = 0;
        hz = 0;
    }

    private void logData() {
//        Log.d(LOG_TAG, "LogData - Analyzing Data!");
        zThreshAlgorithm(acceleration, mThresh);
    }

    private void zThreshAlgorithm(float[] data, float thresh) {
        float y = data[1] / AppSettings.GRAVITY_CONSTANT;
        handleState(thresh, y);
        
    }

    private void handleState(float thresh, float y) {
        if(currentState && !previousState && y >= thresh) {
            previousState = currentState;
  //                currentState = true;
        }
        
        if(!currentState && previousState && y < thresh){
            previousState = currentState;
        }
        
        if(currentState && previousState && y < thresh) {
            previousState = currentState;
            currentState = false;
        }
        
        if(!currentState && !previousState && y >= thresh){
            previousState = currentState;
            currentState = true;
        } 
      
        Log.d(LOG_TAG, String.format("State Machine: State: %b\nPrevious: %b %s", currentState, previousState, stateString(new Boolean[] {currentState, previousState})));
    }

    public static String stateString(Boolean[] states){
        if(!states[0] && !states[1])    return "Flujo Normal";
       
        if(states[0] && !states[1])
                return "Cayo en un hoyo";       
        if(!states[0] && states[1])
                return "Salio del hoyo";
        if(states[0] && states[1])
                return "esta dentro del hoyo";
        
        return null;
    }

    private void startLog() {
        mThread = new Thread(PotholeDetectorActivity.this);
        mThread.start();
    }

    private void stopLog() {
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
    }
}
