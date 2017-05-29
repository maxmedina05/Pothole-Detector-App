package com.medmax.potholedetector.main;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.medmax.potholedetector.R;
import com.medmax.potholedetector.utilities.AppSettings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.view.View.OnClickListener;

public class RegisterActivity extends AppCompatActivity implements OnClickListener, android.location.LocationListener {

    // Constants
    private static final String LOG_TAG = "RegisterActivity";

    // Variables
    private File mFile;

    // GPS Interfaces
    private LocationManager mLocationManager;
    private double mLastKnownLatitude;
    private double mLastKnownLongitude;

    // UI Components
    private TextView mTvLongitude;
    private TextView mTvLatitude;
    private Button mBtnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // makefile
        mFile = new File(
                this.getExternalFilesDir(Environment.MEDIA_MOUNTED),
                AppSettings.POTHOLE_REGISTER_CSV_FILE);

        // connect ui
        mBtnRegister = (Button) findViewById(R.id.btn_register);
        mTvLongitude = (TextView) findViewById(R.id.gps_longitude);
        mTvLatitude = (TextView) findViewById(R.id.gps_latitude);

        mBtnRegister.setOnClickListener(this);

        mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

    }

    @Override
    protected void onPause() {
      super.onPause();
        mLocationManager.removeUpdates(this);

    }

    @Override
    public void onClick(View v) {
      saveDataToCSV(mLastKnownLatitude, mLastKnownLongitude);
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
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

    private void handleNewLocation(Location location) {
        if(location != null) {
            Log.d(LOG_TAG, location.toString());

            mLastKnownLatitude = location.getLatitude();
            mLastKnownLongitude = location.getLongitude();
            updateUI(mLastKnownLatitude, mLastKnownLongitude);
        }
    }

    private void updateUI(double latitude, double longitude) {
        mTvLongitude.setText(String.valueOf(longitude));
        mTvLatitude.setText(String.valueOf(latitude));
    }



    private void saveDataToCSV(double latitude, double longitude) {
        String deviceName = Build.MANUFACTURER + " " + Build.MODEL;
        String currentDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(Calendar.getInstance().getTime());

        try (BufferedWriter bwriter = new BufferedWriter(new FileWriter(mFile, true))) {
            bwriter.write(String.format("%s, %s, %f, %f", currentDate, deviceName, longitude, latitude));
            bwriter.newLine();
            bwriter.close();

            Toast toast = Toast.makeText(
                    this.getApplicationContext(),
                    getString(R.string.pothole_registered_success),
                    Toast.LENGTH_SHORT);
            toast.show();

        } catch (IOException e) {
            Log.e(LOG_TAG, e.toString());
            Toast toast = Toast.makeText(
                    this.getApplicationContext(),
                    "Ups!, something went wrong",
                    Toast.LENGTH_SHORT);
            toast.show();
        }
    }

}
