package com.medmax.potholedetector.main;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.medmax.potholedetector.R;
import com.medmax.potholedetector.multithreading.ThreadPoolManager;
import com.medmax.potholedetector.utilities.PotholeDbHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.Callable;

import static com.medmax.potholedetector.data.AccelerometerDataContract.*;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    public String LOG_TAG = "MainActivity";
    public String fileName = "POTHOLE";

    private int SAMPLING_RATE = SensorManager.SENSOR_DELAY_FASTEST;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private File mFile;

    private TextView xAxis;
    private TextView yAxis;
    private TextView zAxis;
    private TextView deviceModelLabel;

    private Switch sensingSwitch;
    private long lastUpdate = 0;
    private boolean isSensing = false;

    PotholeDbHelper dbHelper;
    private String deviceModelName;

    // Thread Pool
    private ThreadPoolManager mthreadPool;

    private double mLongitude = 0;
    private double mLatitude = 0;
    private double mAltitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceModelName = Build.MANUFACTURER + " " + Build.MODEL;

        xAxis = (TextView) findViewById(R.id.xaxis);
        yAxis = (TextView) findViewById(R.id.yaxis);
        zAxis = (TextView) findViewById(R.id.zaxis);
        deviceModelLabel = (TextView) findViewById(R.id.device_model_label);

        sensingSwitch = (Switch) findViewById(R.id.sensing_switch);
        sensingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isSensing = isChecked;
            }
        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, SAMPLING_RATE);
        dbHelper = PotholeDbHelper.getInstance(this.getApplicationContext());

        deviceModelLabel.setText(deviceModelName);
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String datenow = sdf.format(c.getTime());
        fileName += datenow + ".csv";

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        mFile = new File(
                this.getExternalFilesDir(Environment.MEDIA_MOUNTED),
                fileName);

        mthreadPool = ThreadPoolManager.getsInstance();

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                10,
                new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        mLongitude = location.getLongitude();
                        mLatitude = location.getLatitude();
                        mAltitude = location.getAltitude();
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {

                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    @Override
                    public void onProviderDisabled(String provider) {

                    }
                });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            final float x = event.values[0];
            final float y = event.values[1];
            final float z = event.values[2];
            final long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                xAxis.setText(Float.toString(x));
                yAxis.setText(Float.toString(y));
                zAxis.setText(Float.toString(z));

                lastUpdate = curTime;

                //record only if allowed
                if (isSensing) {
                    mthreadPool.addCallable(new Callable() {
                        @Override
                        public Object call() throws Exception {
                            Log.i(LOG_TAG, "Background Task");
                            saveDataToCSV(curTime, deviceModelName, x, y, z);
                            saveDatatoDB(curTime, deviceModelName, x, y, z);
                            Log.i(LOG_TAG, "Background Task Done!");
                            return null;
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SAMPLING_RATE);
    }

    private void saveDatatoDB(long timespan, String deviceName, float x, float y, float z) {
        float gx = (float) (x / 9.81);
        float gy = (float) (y / 9.81);
        float gz = (float) (z / 9.81);

        if(dbHelper != null) {
            ContentValues values = new ContentValues();
            values.put(AccelerometerReading.COLUMN_NAME_TIMESPAN, timespan);
            values.put(AccelerometerReading.COLUMN_NAME_DEVICE_NAME, deviceName);
            values.put(AccelerometerReading.COLUMN_NAME_ACC_X_AXIS, x);
            values.put(AccelerometerReading.COLUMN_NAME_ACC_Y_AXIS, y);
            values.put(AccelerometerReading.COLUMN_NAME_ACC_Z_AXIS, z);
            values.put(AccelerometerReading.COLUMN_NAME_G_FORCE_X_AXIS, gx);
            values.put(AccelerometerReading.COLUMN_NAME_G_FORCE_Y_AXIS, gy);
            values.put(AccelerometerReading.COLUMN_NAME_G_FORCE_Z_AXIS, gz);

            values.put(AccelerometerReading.COLUMN_NAME_LONGITUDE, mLongitude);
            values.put(AccelerometerReading.COLUMN_NAME_LATITUDE, mLatitude);
            values.put(AccelerometerReading.COLUMN_NAME_ALTITUDE, mAltitude);

            long newId = dbHelper.getWritableDatabase().insert(AccelerometerReading.TABLE_NAME, null, values);
        }
    }

    private void saveDataToCSV(long timespan, String deviceName, float x, float y, float z) {
        float gx = (float) (x / 9.81);
        float gy = (float) (y / 9.81);
        float gz = (float) (z / 9.81);

        try (BufferedWriter bwriter = new BufferedWriter(new FileWriter(mFile, true))) {
            bwriter.write(String.format("%d, %s, %f, %f, %f %f %f %f %f %f %f", timespan, deviceName , x, y, z, gx, gy, gz, mLongitude, mLatitude, mAltitude));
            bwriter.newLine();
            bwriter.close();


        } catch (IOException e) {
            Log.e(LOG_TAG, e.toString());
        }
    }
    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
