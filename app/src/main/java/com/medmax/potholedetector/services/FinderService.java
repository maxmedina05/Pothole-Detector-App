package com.medmax.potholedetector.services;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.medmax.potholedetector.config.AppSettings;
import com.medmax.potholedetector.data.analyzer.PotholeDataFrame;
import com.medmax.potholedetector.models.AccData;
import com.medmax.potholedetector.models.Defect;
import com.medmax.potholedetector.models.FinderObject;
import com.medmax.potholedetector.models.StreetDefect;
import com.medmax.potholedetector.utilities.MathHelper;
import com.medmax.potholedetector.utilities.MyPreferenceManager;

import org.json.JSONObject;

/**
 * Created by Max Medina on 2017-10-18.
 */

public class FinderService extends LoggerService implements Response.Listener<JSONObject>, Response.ErrorListener {

    // Analyzer
    private final static String LOG_TAG = FinderService.class.getSimpleName();
    private PotholeDataFrame mDataFrame;
    private boolean defectFound = false;
    private float finderStartTime = 0;
    private NotificationManager notificationManager;
    private int defectCount = 0;
    // Services
    private HttpService mHttpService;

    public FinderService(Context context) {
        super(context);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDataFrame = new PotholeDataFrame();
        mHttpService = new HttpService();
        mHttpService.init(mContext);

        notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    protected void onAccelerometerSensorChanged(float[] values) {
        super.onAccelerometerSensorChanged(values);

        float x = values[0];
        float y = values[1];
        float z = values[2];

        // X is Gravity Axis
        if (values[0] > 9 || values[0] < -9) {
            mGravityAxis = 'X';
            x = values[1];
            y = values[2];
            z = values[0];
            // Y is Gravity Axis
        } else if (values[1] > 9 || values[1] < -9) {
            mGravityAxis = 'Y';
            z = values[1];
            y = values[2];
        }
        // Z is Gravity Axis
        else if (values[2] > 9 || values[2] < -9) {
            mGravityAxis = 'Z';
        }

        float timestamp = mTimestamp;
        x /= AppSettings.GRAVITY_CONSTANT;
        y /= AppSettings.GRAVITY_CONSTANT;
        z /= AppSettings.GRAVITY_CONSTANT;

        pdAlgorithm(timestamp, x, y, z);
    }

    private void pdAlgorithm(float timestamp, float x, float y, float z) {
        mDataFrame.addRow(new AccData(x, y, z, timestamp));
        float finderCurrentTime = timestamp;
        float deltaTime = finderCurrentTime - finderStartTime;
        float winSize = mPreferenceManager.getWinSize();
        float smWinSize = mPreferenceManager.getSmWinSize();

        // wait for cooldown delay
        if (defectFound) {
            if (deltaTime <= mPreferenceManager.getCoolDownTime()) {
                return;
            } else {
                defectFound = false;
            }
        }

        // don't start working until it has enough data
        if (finderCurrentTime <= winSize) {
            return;
        }

        if (deltaTime >= smWinSize) {
            new FinderService.FinderTask(finderStartTime, finderCurrentTime, lastLatitude, lastLongitude).execute(mDataFrame.clone());
            finderStartTime = finderCurrentTime;
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e(LOG_TAG, "response: " + error.toString());

        Intent service = new Intent(mContext, StreetDefectDetectorService.class);
        mContext.stopService(service);

        Intent i = mContext.getPackageManager()
                .getLaunchIntentForPackage( mContext.getPackageName() );
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivity(i);
    }

    @Override
    public void onResponse(JSONObject response) {
        Log.d(LOG_TAG, "response: " + response.toString());
    }

    private void onDefectFound(float startTime, float currentTime, float longitude, float latitude) {
        Log.d(LOG_TAG, String.format("A defect was found between ti: %.4f and tf: %.4f", startTime, currentTime));
        //Define sound URI
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(android.R.drawable.btn_star)
                .setLights(0xFFFF0000, 300, 500)
                .setSound(soundUri)
                .setContentTitle("Street Defect detected!")
                .setContentText(String.format("Street defect #%d was found on lat: %.5f lng: %.5f", ++defectCount, longitude, latitude));
        notificationManager.notify(AppSettings.STREET_DEFECT_FOUND_NOTIFY_ID, builder.build());
    }

    private class FinderTask extends AsyncTask<PotholeDataFrame, Integer, FinderObject> {
        private float startTime;
        private float currentTime;
        private float latitude;
        private float longitude;

        FinderTask(float startTime, float currentTime, float lastLatitude, float lastLongitude) {
            this.startTime = startTime;
            this.currentTime = currentTime;
            this.latitude = lastLatitude;
            this.longitude = lastLongitude;
        }

        @Override
        protected FinderObject doInBackground(PotholeDataFrame... params) {
            StreetDefect streetDefect = new StreetDefect();
            FinderObject finderObject = new FinderObject(startTime, currentTime, latitude, longitude, false, streetDefect);

            float winSize = mPreferenceManager.getWinSize();
            float smWinSize = mPreferenceManager.getSmWinSize();

            PotholeDataFrame oneWin = params[0].query(currentTime - winSize, currentTime);
            PotholeDataFrame win = oneWin.query(currentTime - winSize, currentTime - smWinSize);
            PotholeDataFrame smWin = oneWin.query(currentTime - smWinSize, currentTime);

            float one_x_mean = (float) oneWin.computeMean(Defect.Axis.AXIS_X);
            float one_x_std = (float) oneWin.computeStd(Defect.Axis.AXIS_X, one_x_mean);

            float sm_z_mean = (float) smWin.computeMean(Defect.Axis.AXIS_Z);
            float sm_z_std = (float) smWin.computeStd(Defect.Axis.AXIS_Z, sm_z_mean);

            one_x_std = (float) MathHelper.round(one_x_std, AppSettings.NDIGITS);
            sm_z_std = (float) MathHelper.round(sm_z_std, AppSettings.NDIGITS);

            one_x_std = (float) MathHelper.round(one_x_std, AppSettings.NDIGITS);
            sm_z_std = (float) MathHelper.round(sm_z_std, AppSettings.NDIGITS);

            if(one_x_std < mPreferenceManager.getxStdThresh() || sm_z_std < mPreferenceManager.getzStdThresh()) {
                return finderObject;
            }

            float mean = (float) win.computeMean(Defect.Axis.AXIS_Z);
            float std = (float) win.computeStd(Defect.Axis.AXIS_Z, mean);
            // dynamic thresh
            float thresh = (float) MathHelper.round(mean + (mPreferenceManager.getK() * std), AppSettings.NDIGITS);
            float z_max = (float) MathHelper.round(smWin.computeMax(), AppSettings.NDIGITS);

            if (z_max >= thresh) {
                defectFound = true;
                finderObject.setDefectFound(true);
            }

            double one_y_std = oneWin.computeMean(Defect.Axis.AXIS_Y);
            double one_z_std = oneWin.computeMean(Defect.Axis.AXIS_Z);

            streetDefect.setDeviceName(mDeviceName);
            streetDefect.setLatitude(lastLatitude);
            streetDefect.setLongitude(lastLongitude);

            streetDefect.setxMean(one_x_mean);
            streetDefect.setyMean((float) one_y_std);
            streetDefect.setyMean((float) one_z_std);

            streetDefect.setxStd(one_x_std);
            streetDefect.setxStd((float) oneWin.computeStd(Defect.Axis.AXIS_Y, one_y_std));
            streetDefect.setxStd((float) oneWin.computeStd(Defect.Axis.AXIS_Z, one_z_std));

            return finderObject;
        }

        @Override
        protected void onPostExecute(FinderObject df) {
            if (df.wasDefectFound()) {
                onDefectFound(df.getStartTime(), df.getCurrentTime(), df.getLongitude(), df.getLatitude());
                mHttpService.postStreetDefect(df.getStreetDefect(), FinderService.this, FinderService.this);
//                if(!MyPreferenceManager.getInstance().isDebuggerOn()) {
//                    mHttpService.postStreetDefect(df.getStreetDefect(), FinderService.this, FinderService.this);
//                }
            }
        }
    }
}
