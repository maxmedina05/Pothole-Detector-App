package com.medmax.potholedetector;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
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

import com.medmax.potholedetector.config.AppSettings;
import com.medmax.potholedetector.services.GPSManager;
import com.medmax.potholedetector.services.OnGPSUpdateListener;
import com.medmax.potholedetector.threads.ThreadManager;
import com.medmax.potholedetector.utilities.CSVHelper;
import com.medmax.potholedetector.utilities.DateTimeHelper;
import com.medmax.potholedetector.utilities.PotholeCSVContract;
import com.medmax.potholedetector.views.FinderActivity;

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
    protected String defectFoundMsg = "";

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
    protected float lastLongitude = 0;
    protected float lastLatitude = 0;

    // Helpers
    private CSVHelper csvHelper;

    // Multi threading
    private ThreadManager mThreadManager;

    // Debugger
    protected boolean isDebuggerOn = false;

    // Preferences
    protected float z_thresh = 1.4f;
    protected float winSize = 1.0f;
    protected float smWinSize = 0.1f;
    protected float K = 3.0f;
    protected float z_std_thresh = 0.19f;
    protected float x_std_thresh = 0.10f;
    protected float coolDownTime = AppSettings.COOLDOWN_TIME;
    protected int selectedAlgorithm = FinderActivity.EnumAlgorithm.Z_THRESH_ALGORITHM;

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
        mThreadManager = ThreadManager.getsInstance();

        mDeviceName = Build.MANUFACTURER + " " + Build.MODEL;

        mGPSManager = new GPSManager(this);
        mGPSManager.setOnGPSUpdateListener(this);
    }

    private void loadPrefParameters() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        String key = getResources().getString(R.string.pref_zthresh_key);
        z_thresh = Float.parseFloat(sharedPrefs.getString(key, "1.4"));

        key = getResources().getString(R.string.pref_algorithm_list_key);
        selectedAlgorithm = Integer.parseInt(sharedPrefs.getString(key, "100"));
        key = getResources().getString(R.string.pref_window_size);
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
        setContentView(R.layout.activity_sensor);
    }

    private void setupUIComponents() {
        tvTimestamp = (TextView) findViewById(R.id.tv_timestamp);
        tvSpeed = (TextView) findViewById(R.id.tv_speed);
        tvFrequency = (TextView) findViewById(R.id.tv_frequency);
        tvAxisX = (TextView) findViewById(R.id.tv_x_axis);
        tvAxisY = (TextView) findViewById(R.id.tv_y_axis);
        tvAxisZ = (TextView) findViewById(R.id.tv_z_axis);
        btnLog = (ToggleButton) findViewById(R.id.btn_log);
        btnLog.setOnClickListener(this);
//        tvSpeed.setText("");
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
        mStartLogger = false;
        btnLog.setChecked(false);
        mSensorManager.unregisterListener(this);
        mHandler.removeCallbacks(mRunnable);
        mGPSManager.removeLocationUpdates();
        stopLogging();
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_log) {
            mStartLogger = !mStartLogger;

            if (mStartLogger) {
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

            if (mStartLogger) {
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
    }

    protected void sendToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void initLogging() {
        mLoggerStartTime = System.currentTimeMillis();

        if(!isDebuggerOn) {
            mIdSeed = 0;
            String fileName = String.format("%s_%s.%s",
                    AppSettings.POTHOLE_FILENAME,
                    DateTimeHelper.getCurrentDateTime("yyyy-MM-dd hh-mm-ss"),
                    AppSettings.CSV_EXTENSION_NAME
            );

            File exportDir = new File(Environment.getExternalStorageDirectory(), AppSettings.LOGGER_DIRECTORY);
            try {
                csvHelper.open(exportDir, fileName, true);
            } catch (FileNotFoundException e) {
                sendToast("File was not found!");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopLogging() {
        mStartLogger = false;
        mTimestamp = 0;

        if (!isDebuggerOn && csvHelper.isOpen()) {
            try {
                sendToast(String.format("file %s was created", csvHelper.getCurrentFileName()));
                csvHelper.close();

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
        if (location.hasSpeed()) {
            mSpeed = location.getSpeed() * AppSettings.SPEED_CONSTANT;
        }

        lastLatitude = (float) location.getLatitude();
        lastLongitude = (float) location.getLongitude();
    }
}