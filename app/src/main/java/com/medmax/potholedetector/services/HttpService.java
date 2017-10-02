package com.medmax.potholedetector.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.medmax.potholedetector.R;
import com.medmax.potholedetector.config.AppSettings;
import com.medmax.potholedetector.models.StreetDefect;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by maxme on 2017-09-21.
 */

public class HttpService {
    private String mAPIBaseUrl = "";
    private RequestQueue queue;
    private Map headers;

    public void init(Context context){
        headers = new HashMap();
        SharedPreferences sharedPrefs = android.preference.PreferenceManager
                .getDefaultSharedPreferences(context);
        String url = sharedPrefs.getString(context.getString(R.string.pref_api_url), "52.168.3.123");
        mAPIBaseUrl = String.format("http://%s:5099/api/v1/street-defects", url);
        Log.d(this.getClass().getSimpleName(), mAPIBaseUrl);

        sharedPrefs = context.getSharedPreferences(AppSettings.PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
        String token = sharedPrefs.getString(context.getString(R.string.saved_token_id), "");
        headers.put("Authorization", String.format("Pothole %s", token));
        headers.put("Content-Type", "application/json");
        queue = Volley.newRequestQueue(context);
    }

    public void postStreetDefect(StreetDefect defect, Response.Listener listener, Response.ErrorListener errorListener) {
        JSONObject body = new JSONObject();
        try {
//            JSONObject location = new JSONObject();
//            location.put("type", "{type: \"Point\"}");
//            location.put("coordinates", String.format("[%f, %f]", defect.getLongitude(), defect.getLatitude()));
//
//            body.put("location", location);
            body.put("latitude", Float.toString(defect.getLatitude()));
            body.put("longitude", Float.toString(defect.getLongitude()));
            body.put("userId", "NO_USER");

            body.put("xMean", Float.toString(defect.getxMean()));
            body.put("yMean", Float.toString(defect.getyMean()));
            body.put("zMean", Float.toString(defect.getzMean()));

            body.put("xStd", Float.toString(defect.getxStd()));
            body.put("yStd", Float.toString(defect.getyStd()));
            body.put("zStd", Float.toString(defect.getzStd()));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        CustomJSONRequest postRequest = new CustomJSONRequest(
                Request.Method.POST,
                mAPIBaseUrl,
                body,
                listener,
                errorListener,
                headers
        );
        queue.add(postRequest);
    }

    private class CustomJSONRequest extends JsonObjectRequest {

        private Map headers;

        public CustomJSONRequest(int method, String url, JSONObject jsonRequest,Response.Listener listener, Response.ErrorListener errorListener)
        {
            super(method, url, jsonRequest, listener, errorListener);
        }

        public CustomJSONRequest(int method, String url, JSONObject jsonRequest,Response.Listener listener, Response.ErrorListener errorListener, Map headers)
        {
            super(method, url, jsonRequest, listener, errorListener);
            this.headers = headers;
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            if(headers == null)
                return super.getHeaders();

            return headers;
        }
    }
}
