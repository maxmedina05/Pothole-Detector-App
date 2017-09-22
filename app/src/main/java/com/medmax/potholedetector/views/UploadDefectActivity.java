package com.medmax.potholedetector.views;

import android.app.Activity;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.medmax.potholedetector.R;
import com.medmax.potholedetector.models.StreetDefect;
import com.medmax.potholedetector.services.GPSManager;
import com.medmax.potholedetector.services.HttpService;
import com.medmax.potholedetector.services.OnGPSUpdateListener;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Max Medina on 2017-09-21.
 */

public class UploadDefectActivity extends Activity implements OnGPSUpdateListener, View.OnClickListener, Response.Listener<JSONObject>, Response.ErrorListener {
    private static final String LOG_TAG = UploadDefectActivity.class.getSimpleName();
    private float mLastKnownLatitude = 0;
    private float mLastKnownLongitude = 0;

    private TextView tvLatitude;
    private TextView tvLongitude;
    private Button btnUpload;
    private GPSManager mGPSManager;
    private HttpService mHttpService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_upload_defect);
        tvLatitude = (TextView) findViewById(R.id.tv_latitude);
        tvLongitude = (TextView) findViewById(R.id.tv_longitude);
        btnUpload = (Button) findViewById(R.id.btn_upload);

        mGPSManager = new GPSManager(this);
        mGPSManager.setOnGPSUpdateListener(this);
        btnUpload.setOnClickListener(this);

        mHttpService = new HttpService();
        mHttpService.init(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGPSManager.requestLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGPSManager.removeLocationUpdates();
    }

    @Override
    public void onGPSUpdate(Location location) {
        mLastKnownLatitude = (float) location.getLatitude();
        mLastKnownLongitude = (float) location.getLongitude();

        tvLatitude.setText(String.format("Latitude: %f", mLastKnownLatitude));
        tvLongitude.setText(String.format("Longitude: %f", mLastKnownLongitude));
    }

    @Override
    public void onClick(View v) {
        StreetDefect streetDefect = new StreetDefect();
        streetDefect.setDeviceName(Build.MANUFACTURER + " " + Build.MODEL);
        streetDefect.setLatitude(mLastKnownLatitude);
        streetDefect.setLongitude(mLastKnownLongitude);

        mHttpService.postStreetDefect(streetDefect, this, this);
    }

    @Override
    public void onResponse(JSONObject response) {
        Log.d(LOG_TAG, "response: " + response.toString());

        try {
            Toast.makeText(this.getApplicationContext(), response.get("message").toString(), Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e(LOG_TAG, "response: " + error.toString());
    }
}
