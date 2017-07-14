package com.medmax.potholedetector.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorEvent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
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
                saveDefectsToCSV(mAdapter.getItems());
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
            if(defectSeed == 0) {
                helper.setHeader(DefectCSVContract.DefectCSV.getHeaders());
            }

            for (Defect defect : defects) {
                helper.write(defect.getCSVPrint());
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
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.pref_defect_seed), seed);
        editor.commit();
    }

    @Override
    protected void setUILayout() {
        setContentView(R.layout.activity_finder_ex);
    }

    @Override
    public void myOnClick(View v) {
        super.myOnClick(v);
    }

    @Override
    public void myOnSensorChanged(SensorEvent event) {
        super.myOnSensorChanged(event);
    }

    @Override
    protected void onDefectFound(PotholeDataFrame win, PotholeDataFrame smWin, float stime, float ctime)  {
        super.onDefectFound(win, smWin, stime, ctime);

        new computeDefectStatistics(++defectSeed, stime, ctime).execute(win, smWin);
    }

    private class computeDefectStatistics extends AsyncTask<PotholeDataFrame, Integer, Defect> {

        private int seed = 0;
        private float startTime = 0;
        private float endTime = 0;

        public computeDefectStatistics(int defectSeed, float stime, float ctime) {
            this.seed = defectSeed;
            this.startTime = stime;
            this.endTime = ctime;
        }

        @Override
        protected Defect doInBackground(PotholeDataFrame... params) {
            PotholeDataFrame win = params[0];
            PotholeDataFrame smWin = params[1];

            Defect defect = new Defect();
            defect.setId(seed);
            defect.setStartTime(startTime);
            defect.setEndTime(endTime);

            // Compute win Mean
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

            return defect;
        }

        @Override
        protected void onPostExecute(Defect defect) {
            mDefects.add(defect);
            mAdapter.notifyDataSetChanged();
        }
    }
}
