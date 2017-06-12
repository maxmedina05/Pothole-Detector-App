package com.medmax.potholedetector.main;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.medmax.potholedetector.R;
import com.medmax.potholedetector.data.AccelerometerDataContract;
import com.medmax.potholedetector.data.PotholeDataContract;
import com.medmax.potholedetector.utilities.AppSettings;
import com.medmax.potholedetector.utilities.DateTimeHelper;
import com.medmax.potholedetector.utilities.PotholeDbHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.view.View.OnClickListener;

public class RegisterActivity extends Activity implements OnClickListener, android.location.LocationListener {

    // Constants
    private static final String LOG_TAG = "RegisterActivity";

    // Variables
    private File mFile;
    private String mDeviceName;

    // GPS Interfaces
    private LocationManager mLocationManager;
    private double mLastKnownLatitude;
    private double mLastKnownLongitude;

    // UI Components
    private TextView mTvLongitude;
    private TextView mTvLatitude;
    private Button mBtnRegister;
    private Spinner mSpnTypeHole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // makefile
//        mFile = new File(
//                this.getExternalFilesDir(Environment.MEDIA_MOUNTED),
//                AppSettings.POTHOLE_REGISTER_CSV_FILE);

        mDeviceName = Build.MANUFACTURER + " " + Build.MODEL;

        // connect ui
        mBtnRegister = (Button) findViewById(R.id.btn_register);
        mTvLongitude = (TextView) findViewById(R.id.gps_longitude);
        mTvLatitude = (TextView) findViewById(R.id.gps_latitude);
        mSpnTypeHole = (Spinner) findViewById(R.id.spn_type_hole);
        mBtnRegister.setOnClickListener(this);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.type_holes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpnTypeHole.setAdapter(adapter);

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
//      saveDataToCSV(mLastKnownLatitude, mLastKnownLongitude);

        saveToDB();

    }

    private void saveToDB() {
        Log.d(LOG_TAG, "LogData - Storing Data in database!");
        String type = (String)mSpnTypeHole.getSelectedItem();
//        Log.d(LOG_TAG, "LogData - " + type);

        PotholeDbHelper dbHelper = PotholeDbHelper.getInstance(this.getApplicationContext());

        String datestr = DateTimeHelper.getCurrentDateTime("yyyy-MM-dd hh:mm:ss");

        if(dbHelper != null) {
            ContentValues values = new ContentValues();
            values.put(PotholeDataContract.PossiblePothole.DATE_CREATED, datestr);
            values.put(PotholeDataContract.PossiblePothole.DEVICE_NAME, mDeviceName);
            values.put(PotholeDataContract.PossiblePothole.LATITUDE, mLastKnownLatitude);
            values.put(PotholeDataContract.PossiblePothole.LONGITUDE, mLastKnownLongitude);
            values.put(PotholeDataContract.PossiblePothole.TYPE, type);

            long newId = dbHelper.getWritableDatabase().insert(PotholeDataContract.PossiblePothole.TABLE_NAME, null, values);

            if(newId != -1) {
                Toast toast = Toast.makeText(
                        this,
                        getString(R.string.pothole_registered_success),
                        Toast.LENGTH_SHORT);
                toast.show();
            } else {
                Toast toast = Toast.makeText(
                        this.getApplicationContext(),
                        "Ups!, something went wrong",
                        Toast.LENGTH_SHORT);
                toast.show();
            }

        }

        if(dbHelper != null)
            dbHelper.close();
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
