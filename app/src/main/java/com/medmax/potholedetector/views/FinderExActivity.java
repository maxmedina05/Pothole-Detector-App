package com.medmax.potholedetector.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorEvent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.medmax.potholedetector.R;
import com.medmax.potholedetector.config.AppSettings;
import com.medmax.potholedetector.data.analyzer.PotholeDataFrame;
import com.medmax.potholedetector.models.Defect;
import com.medmax.potholedetector.utilities.CSVHelper;
import com.medmax.potholedetector.utilities.DefectCSVContract;
import com.medmax.potholedetector.views.adapters.DefectAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max Medina on 2017-07-11.
 */

public class FinderExActivity extends FinderActivity {

    private volatile int defectSeed = 0;
    private volatile List<Defect> mDefects;
    // UI Components
    private Button btnClearlist;
    private ListView mListView;
    private DefectAdapter mAdapter;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        readDefectSeed();
        setupUI();
    }

    private void setupUI() {
        mDefects = new ArrayList<Defect>();

        mAdapter = new DefectAdapter(this, mDefects);
        mListView = (ListView)findViewById(R.id.list_view_defects);
        btnClearlist = (Button) findViewById(R.id.btn_clear);
        mListView.setAdapter(mAdapter);

        btnClearlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                saveDefectsToCSV(mAdapter.getItems());
                if(!isDebuggerOn) {
                    saveDefectsToCSV(mAdapter.getItems());
                }
                mAdapter.clear();
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        writeDefectSeed(defectSeed);
    }

    private void saveDefectsToCSV(List<Defect> defects) {
        CSVHelper helper = new CSVHelper();
        String fileName = AppSettings.DEFECTS_FILENAME;
        File exportDir = new File(Environment.getExternalStorageDirectory(), AppSettings.DEFECTS_DIRECTORY);

        try {
            helper.open(exportDir, fileName, true);
            if(helper.isOpen()) {
                helper.setHeader(DefectCSVContract.DefectCSV.getHeaders());

                for (Defect defect : defects) {
                    helper.write(defect.getCSVPrint());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                helper.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void readDefectSeed() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        int defaultValue = 0;
        defectSeed = sharedPref.getInt(getString(R.string.pref_defect_seed), defaultValue);
    }

    private void writeDefectSeed(int seed) {
        if(!isDebuggerOn) {
            SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(getString(R.string.pref_defect_seed), seed);
            editor.commit();
        }
    }

    @Override
    protected void setUILayout() {
        setContentView(R.layout.activity_finder_ex);
    }

    @Override
    public void myOnSensorChanged(SensorEvent event) {
        super.myOnSensorChanged(event);
    }

    @Override
    protected void onDefectFound(PotholeDataFrame oneWin, PotholeDataFrame win, PotholeDataFrame smWin, float stime, float ctime, float longitude, float latitude)  {
        super.onDefectFound(oneWin, win, smWin, stime, ctime, longitude, latitude);

        new computeDefectStatistics(++defectSeed, stime, ctime, longitude, latitude).execute(oneWin, win, smWin);
    }

    private class computeDefectStatistics extends AsyncTask<PotholeDataFrame, Integer, Defect> {

        private int seed = 0;
        private float startTime = 0;
        private float endTime = 0;
        private float longitude = 0;
        private float latitude = 0;

        private computeDefectStatistics(int defectSeed, float stime, float ctime, float longitude, float latitude) {
            this.seed = defectSeed;
            this.startTime = stime;
            this.endTime = ctime;
            this.longitude = longitude;
            this.latitude = latitude;
        }

        @Override
        protected Defect doInBackground(PotholeDataFrame... params) {
            PotholeDataFrame oneWin = params[0];
            PotholeDataFrame win = params[1];
            PotholeDataFrame smWin = params[2];

            Defect defect = new Defect();
            defect.setId(seed);
            defect.setStartTime(startTime);
            defect.setEndTime(endTime);
            defect.setLatitude(latitude);
            defect.setLongitude(longitude);

            // Compute one win Mean
            defect.setOnexMean(oneWin.computeMean(Defect.Axis.AXIS_X));
            defect.setOneyMean(oneWin.computeMean(Defect.Axis.AXIS_Y));
            defect.setOnezMean(oneWin.computeMean(Defect.Axis.AXIS_Z));
            // Compute one win std
            defect.setOnexStd(oneWin.computeStd(Defect.Axis.AXIS_X, defect.getOnexMean()));
            defect.setOneyStd(oneWin.computeStd(Defect.Axis.AXIS_Y, defect.getOneyMean()));
            defect.setOnezStd(oneWin.computeStd(Defect.Axis.AXIS_Z, defect.getOnezMean()));

            // Compute win std
            defect.setxMean(win.computeMean(Defect.Axis.AXIS_X));
            defect.setyMean(win.computeMean(Defect.Axis.AXIS_Y));
            defect.setzMean(win.computeMean(Defect.Axis.AXIS_Z));

            // Compute win std
            defect.setxStd(win.computeStd(Defect.Axis.AXIS_X, defect.getxMean()));
            defect.setyStd(win.computeStd(Defect.Axis.AXIS_Y, defect.getyMean()));
            defect.setzStd(win.computeStd(Defect.Axis.AXIS_Z, defect.getzMean()));

            // Compute small win Mean
            defect.setSm_xMean(smWin.computeMean(Defect.Axis.AXIS_X));
            defect.setSm_yMean(smWin.computeMean(Defect.Axis.AXIS_Y));
            defect.setSm_zMean(smWin.computeMean(Defect.Axis.AXIS_Z));

            // Compute small win std
            defect.setSm_xStd(smWin.computeStd(Defect.Axis.AXIS_X, defect.getSm_xMean()));
            defect.setSm_yStd(smWin.computeStd(Defect.Axis.AXIS_Y, defect.getSm_yMean()));
            defect.setSm_zStd(smWin.computeStd(Defect.Axis.AXIS_Z, defect.getSm_zMean()));

            defect.setSm_zMax(smWin.computeMax());

            return defect;
        }

        @Override
        protected void onPostExecute(Defect defect) {
            mDefects.add(defect);
            mAdapter.notifyDataSetChanged();
        }
    }
}
