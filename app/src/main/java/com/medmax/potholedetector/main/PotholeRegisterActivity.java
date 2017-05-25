package com.medmax.potholedetector.main;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.medmax.potholedetector.R;

import static com.google.android.gms.common.api.GoogleApiClient.Builder;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;

public class PotholeRegisterActivity extends AppCompatActivity implements OnConnectionFailedListener, ConnectionCallbacks, View.OnClickListener {

    // Constants
    // private static final int TWO_MINUTES = 1000 * 60 * 2;

    // Variables
    private Location mLastKnownLocation;

    // GPS Interfaces
    GoogleApiClient mGoogleApiClient;
//    private LocationManager mLocationManager;

    // UI Components
    private TextView mTvLongitude;
    private TextView mTvLatitude;
    private Button mBtnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pothole_register);

        // connect ui
        mBtnRegister = (Button) findViewById(R.id.register_button);
        mTvLongitude = (TextView) findViewById(R.id.gps_longitude);
        mTvLatitude = (TextView) findViewById(R.id.gps_latitude);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mBtnRegister.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        updateUI();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private void updateUI() {
        if(mLastKnownLocation != null) {
            mTvLongitude.setText(String.valueOf(mLastKnownLocation.getLongitude()));
            mTvLatitude.setText(String.valueOf(mLastKnownLocation.getLatitude()));
        }
    }

    @Override
    public void onClick(View v) {
        mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        updateUI();
    }
}
