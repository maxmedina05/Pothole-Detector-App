package com.medmax.potholedetector.main;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.medmax.potholedetector.R;

import static com.google.android.gms.common.api.GoogleApiClient.Builder;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;

public class RegisterActivity extends AppCompatActivity implements OnConnectionFailedListener, 
ConnectionCallbacks, View.OnClickListener, LocationListener {

    // Constants
    private static final String LOG_TAG = "RegisterActivity";
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    
    // Variables
    private Location mLastKnownLocation;
    private LocationRequest mLocationRequest;
    
    // GPS Interfaces
    GoogleApiClient mGoogleApiClient;

    // UI Components
    private TextView mTvLongitude;
    private TextView mTvLatitude;
    private Button mBtnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // connect ui
        mBtnRegister = (Button) findViewById(R.id.btn_register);
        mTvLongitude = (TextView) findViewById(R.id.gps_longitude);
        mTvLatitude = (TextView) findViewById(R.id.gps_latitude);

        mBtnRegister.setOnClickListener(this);

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
        
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
      super.onPause();
      if (mGoogleApiClient.isConnected()) {
          LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
          mGoogleApiClient.disconnect();
      }
    }

    @Override
    protected void onStop() {
        if(mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(LOG_TAG, "Location services connected.");
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(location == null) {
          LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
          handleNewLocation(location);
        }
        
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    private void handleNewLocation(Location location) {
      Log.d(LOG_TAG, location.toString());
      double currentLatitude = location.getLatitude();
      double currentLongitude = location.getLongitude();
      updateUI(currentLatitude, currentLongitude);
    }

    private void updateUI(double latitude, double longitude) {
        mTvLongitude.setText(String.valueOf(longitude));
        mTvLatitude.setText(String.valueOf(latitude));
    }

    @Override
    public void onClick(View v) {
      LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }
}
