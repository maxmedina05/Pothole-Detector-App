package com.medmax.potholedetector.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.medmax.potholedetector.BaseSensorActivity;
import com.medmax.potholedetector.R;
import com.medmax.potholedetector.config.AppSettings;
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

public class FinderExActivity extends BaseSensorActivity {

    private int defectSeed = 0;

    // UI Components
    private Button btnClearlist;
    private ListView mListView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        readDefectSeed();
        setupUI();
    }

    private void setupUI() {
        List<Defect> data = new ArrayList<Defect>();
        data.add(new Defect(1, Defect.ClassType.NOTHING));
        data.add(new Defect(2, Defect.ClassType.NOTHING));
        data.add(new Defect(3, Defect.ClassType.NOTHING));
        data.add(new Defect(4, Defect.ClassType.NOTHING));
        data.add(new Defect(5, Defect.ClassType.NOTHING));
        data.add(new Defect(6, Defect.ClassType.NOTHING));
        data.add(new Defect(7, Defect.ClassType.NOTHING));

        final DefectAdapter adapter = new DefectAdapter(this, data);

        mListView = (ListView)findViewById(R.id.list_view_defects);
        btnClearlist = (Button) findViewById(R.id.btn_clear);
        mListView.setAdapter(adapter);

        btnClearlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDefectsToCSV(adapter.getItems());
                adapter.clear();
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void saveDefectsToCSV(List<Defect> defects) {
        CSVHelper helper = new CSVHelper();
        String fileName = AppSettings.DEFECTS_FILENAME;
        File exportDir = new File(Environment.getExternalStorageDirectory(), AppSettings.DEFECTS_DIRECTORY);
        try {
            helper.open(exportDir, fileName, true);
            helper.setHeader(DefectCSVContract.DefectCSV.getHeaders());

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

    }

    @Override
    public void myOnSensorChanged(SensorEvent event) {

    }

}
