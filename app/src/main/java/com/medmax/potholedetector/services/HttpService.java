package com.medmax.potholedetector.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.medmax.potholedetector.R;
import com.medmax.potholedetector.models.StreetDefect;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by maxme on 2017-09-21.
 */

public class HttpService {
    private String mAPIBaseUrl = "";
    private RequestQueue queue;

    public void init(Context context){
        SharedPreferences sharedPrefs = android.preference.PreferenceManager
                .getDefaultSharedPreferences(context);

        String url = sharedPrefs.getString(context.getString(R.string.pref_api_url), "52.168.3.123");
        mAPIBaseUrl = String.format("http://%s:5099/api/street-defects", url);
        Log.d(this.getClass().getSimpleName(), mAPIBaseUrl);
        queue = Volley.newRequestQueue(context);
    }

    public void postStreetDefect(StreetDefect defect, Response.Listener listener, Response.ErrorListener errorListener) {
        JSONObject body = new JSONObject();
        try {
            body.put("deviceName", defect.getDeviceName());
            body.put("latitude", Float.toString(defect.getLatitude()));
            body.put("longitude", Float.toString(defect.getLongitude()));

            body.put("xMean", Float.toString(defect.getxMean()));
            body.put("yMean", Float.toString(defect.getyMean()));
            body.put("zMean", Float.toString(defect.getzMean()));

            body.put("xStd", Float.toString(defect.getxStd()));
            body.put("yStd", Float.toString(defect.getyStd()));
            body.put("zStd", Float.toString(defect.getzStd()));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest postRequest = new JsonObjectRequest(
                Request.Method.POST,
                mAPIBaseUrl,
                body,
                listener,
                errorListener
        );
        queue.add(postRequest);
    }
}
