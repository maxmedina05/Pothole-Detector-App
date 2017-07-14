package com.medmax.potholedetector;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.medmax.potholedetector.config.AppSettings;
import com.medmax.potholedetector.services.GPSManager;
import com.medmax.potholedetector.services.OnGPSUpdateListener;
import com.medmax.potholedetector.threads.ThreadManager;
import com.medmax.potholedetector.utilities.CSVHelper;
import com.medmax.potholedetector.utilities.DateTimeHelper;
import com.medmax.potholedetector.utilities.PotholeCSVContract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Created by Max Medina on 2017-07-06.
 */

public abstract class BaseSensorActivity extends Activity implements View.OnClickListener, SensorEventListener, OnGPSUpdateListener {

    // Constants
    public final static String LOG_TAG = BaseSensorActivity.class.getSimpleName();
    protected static final int SAMPLING_RATE = SensorManager.SENSOR_DELAY_FASTEST;
    protected static final int UPDATE_UI_DELAY = 100;

    // Variables
    protected volatile float[] acc_values = new float[3];
    private String mDeviceName = "";
    protected float mTimestamp = 0;
    protected float mSpeed = 0;
    protected boolean mStartLogger = false;
    protected long mLoggerStartTime = 0;
    private int mIdSeed = 0;

    // Sensor properties
    private SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;

    // Frequency
    private int fqCount = 0;
    private float fqsTime = 0;
    private float fqcTime = 0;
    protected float fqHz = 0;

    private Handler mHandler;
    private Runnable mRunnable;

    // UI Components
    protected TextView tvTimestamp;
    protected TextView tvFrequency;
    protected TextView tvAxisX;
    protected TextView tvAxisY;
    protected TextView tvAxisZ;
    protected TextView tvSpeed;
    protected ToggleButton btnLog;

    // GPS
    private GPSManager mGPSManager;

    // Helpers
    private CSVHelper csvHelper;

    // Multi threading
    private ThreadManager mThreadManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setUILayout();
        setupUIComponents();
        setupSensors();
        setupUpdateUIThread();

        csvHelper = new CSVHelper();
        mThreadManager = ThreadManager.getsInstance();

        mDeviceName = Build.MANUFACTURER + " " + Build.MODEL;

        mGPSManager = new GPSManager(this);
        mGPSManager.setOnGPSUpdateListener(this);

    }

    protected void setUILayout() {
        setContentView(R.layout.activity_sensor);
    }

    private void setupUIComponents() {
        tvTimestamp = (TextView)findViewById(R.id.tv_timestamp);
        tvSpeed = (TextView) findViewById(R.id.tv_speed);
        tvFrequency = (TextView)findViewById(R.id.tv_frequency);
        tvAxisX = (TextView)findViewById(R.id.tv_x_axis);
        tvAxisY = (TextView)findViewById(R.id.tv_y_axis);
        tvAxisZ = (TextView)findViewById(R.id.tv_z_axis);
        btnLog = (ToggleButton)findViewById(R.id.btn_log);
        btnLog.setOnClickListener(this);
//        tvSpeed.setText("");
    }

    private void setupUpdateUIThread() {
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

    @Override
    protected void onResume() {
        super.onResume();
        resetSensorTimer();
        mSensorManager.registerListener(this, mAccelerometerSensor, SAMPLING_RATE);
        mHandler.postDelayed(mRunnable, UPDATE_UI_DELAY);
        mGPSManager.requestLocationUpdates();
    }

    @Override
    protected void onPause() {
        btnLog.setChecked(false);
        mSensorManager.unregisterListener(this);
        mHandler.removeCallbacks(mRunnable);
        mStartLogger = false;
        mGPSManager.removeLocationUpdates();
        stopLogging();
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_log) {
            mStartLogger = !mStartLogger;

            if(mStartLogger){
                mLoggerStartTime = System.currentTimeMillis();
            } else {
                mTimestamp = 0;
            }

            if (!csvHelper.isOpen()) {
                initLogging();
            } else {
                stopLogging();
            }
        }

        myOnClick(v);
    }

    public abstract void myOnClick(View v);

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (Sensor.TYPE_ACCELEROMETER == event.sensor.getType()) {

            if(mStartLogger){
                mTimestamp = (System.currentTimeMillis() - mLoggerStartTime) / 1000.0f;
            }

            calculateFrequency();
            System.arraycopy(event.values, 0, acc_values, 0, event.values.length);

            if (csvHelper.isOpen()) {
                mThreadManager.addCallable(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        logChanges(mTimestamp);
                        return null;
                    }
                });
            }
            myOnSensorChanged(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public abstract void myOnSensorChanged(SensorEvent event);

    protected void updateUI() {
        // TODO: Remove log
//        Log.d(LOG_TAG, String.format("x: %.4f, y: %.4f, z: %.4f", acc_values[0], acc_values[1], acc_values[2]));

        tvAxisX.setText(String.format(Locale.US, "x: %.4f", acc_values[0]));
        tvAxisY.setText(String.format(Locale.US, "y: %.4f", acc_values[1]));
        tvAxisZ.setText(String.format(Locale.US, "z: %.4f", acc_values[2]));

        tvFrequency.setText(String.format(Locale.US, "%.1f hz", fqHz));
        tvTimestamp.setText(String.format(Locale.US, "timestamp: %.3f s", mTimestamp));
        tvSpeed.setText(String.format(Locale.US, "speed: %.2f m/s", mSpeed));
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
        mStartLogger = false;
    }

    protected void sendToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void initLogging() {
        mIdSeed = 0;
        String fileName = String.format("%s_%s.%s",
                AppSettings.POTHOLE_FILENAME,
                DateTimeHelper.getCurrentDateTime("yyyy-MM-dd hh-mm-ss"),
                AppSettings.CSV_EXTENSION_NAME
        );

        File exportDir = new File(Environment.getExternalStorageDirectory(), AppSettings.LOGGER_DIRECTORY);
        try {
            csvHelper.open(exportDir, fileName, true);
            // TODO: Remove logger
            Log.d(LOG_TAG, "Start Logger");
        } catch (FileNotFoundException e) {
            sendToast("File was not found!");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopLogging() {
        mTimestamp = 0;
        if (csvHelper.isOpen()) {
            try {
                sendToast(String.format("file %s was created", csvHelper.getcurrentFileName()));
                csvHelper.close();
                // TODO: Remove logger
                Log.d(LOG_TAG, "Stop Logger");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void logChanges(float timestamp) throws IOException {
        // check again to see if the file is still open
        if (csvHelper.isOpen()) {
            if (mIdSeed == 0) {
                csvHelper.setHeader(PotholeCSVContract.PotholeCSV.getHeaders());
            }

            csvHelper.write(String.format(
                    Locale.US,
                    "%d,%s,%s,%.06f,%.6f,%.6f,%.6f",
                    ++mIdSeed,
                    DateTimeHelper.getCurrentDateTime("yyyy-MM-dd hh:mm:ss.SSS"),
                    mDeviceName,
                    timestamp,
                    acc_values[0],
                    acc_values[1],
                    acc_values[2])
            );
        }
    }

    @Override
    public void onGPSUpdate(Location location) {
        if(location.hasSpeed()) {
            mSpeed = (float) (location.getSpeed() * 3.6);
        }

        Log.d(LOG_TAG, String.format("Speed: %.4f", mSpeed));
    }
}
