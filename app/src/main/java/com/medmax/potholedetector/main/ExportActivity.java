package com.medmax.potholedetector.main;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.medmax.potholedetector.R;
import com.medmax.potholedetector.data.AccelerometerDataContract;
import com.medmax.potholedetector.dialog.DatePickerFragment;
import com.medmax.potholedetector.multithreading.AccelerometerData;
import com.medmax.potholedetector.utilities.AppSettings;
import com.medmax.potholedetector.utilities.DateTimeHelper;
import com.medmax.potholedetector.utilities.PotholeDbHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Max Medina on 2017-06-17.
 */

public class ExportActivity extends Activity {

    private static final String LOG_TAG = ExportActivity.class.getSimpleName();
    private EditText edTxStartDate;
    private EditText edTxEndDate;
    private Button btnExport;

    private PotholeDbHelper mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        edTxStartDate = (EditText)findViewById(R.id.edtx_start_date);
        edTxEndDate = (EditText)findViewById(R.id.edtx_end_date);
        btnExport = (Button) findViewById(R.id.btn_export);

        edTxStartDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerFragment newFragment = new DatePickerFragment();
                newFragment.setCallback(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            edTxStartDate.setText(DateTimeHelper.getFormatDate(year, month, dayOfMonth, "yyyy-MM-dd"));
                            edTxEndDate.setText(DateTimeHelper.getFormatDate(year, month, dayOfMonth+1, "yyyy-MM-dd"));

                    }
                });
                newFragment.show(getFragmentManager(), "datePicker");
            }
        });

        edTxEndDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerFragment newFragment = new DatePickerFragment();
                newFragment.setCallback(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        edTxEndDate.setText(DateTimeHelper.getFormatDate(year, month, dayOfMonth, "yyyy-MM-dd"));
                    }
                });
                newFragment.show(getFragmentManager(), "datePicker");
            }
        });

        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportDatatoCSV();

            }
        });

        mDbHelper = PotholeDbHelper.getInstance(this.getApplicationContext());


    }

    private void exportDatatoCSV(){
        File exportDir = new File(Environment.getExternalStorageDirectory(), AppSettings.POTHOLE_DIRECTORY);
        if(!exportDir.exists()){
            exportDir.mkdir();
        }

        String startDate = edTxStartDate.getText().toString();
        String endDate = edTxEndDate.getText().toString();

        File file = new File(exportDir, String.format("EXPORT_%s_%s.csv", startDate, endDate));
        try (BufferedWriter bwriter = new BufferedWriter(new FileWriter(file, true))) {

            SQLiteDatabase db = mDbHelper.getReadableDatabase();
//            SELECT * FROM accdata WHERE date BETWEEN "2017-08-18 12:08:09" AND "2017-08-18 12:08:20";
            Cursor cursor = db.rawQuery(
                    String.format(
                        Locale.US,
                        "SELECT * FROM %s WHERE %s BETWEEN \"%s\" AND \"%s\"",
                        AccelerometerDataContract.AccelerometerReading.TABLE_NAME,
                        AccelerometerDataContract.AccelerometerReading.COLUMN_NAME_DATE_CREATED,
                        startDate,
                        endDate
                    ), null);

            bwriter.write(AccelerometerDataContract.AccelerometerReading.getColumnNames());
            while(cursor.moveToNext()){
                bwriter.write(String.format(
                        Locale.US,
                        "%d,%d,%s,%s,%s,%s,%s\n",
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getString(6)
                        ));
            }
            bwriter.close();
            Log.d(LOG_TAG, String.format("Export finished! filepath: %s", file.getAbsolutePath()));

            Toast toast = Toast.makeText(
                    this,
                    "Export finished!",
                    Toast.LENGTH_SHORT);
            toast.show();

        } catch (IOException e) {
            Log.e(LOG_TAG, e.toString());
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }
}
