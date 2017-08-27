package com.medmax.potholedetector.views;

import android.graphics.Color;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.medmax.potholedetector.BaseSensorActivity;
import com.medmax.potholedetector.R;

import java.util.Locale;

/**
 * Created by Max Medina on 2017-08-23.
 */

public class VirtualOrientationActivity extends BaseSensorActivity {

    // Constants
    public final static String LOG_TAG = VirtualOrientationActivity.class.getSimpleName();
    protected GraphView graph;
    protected TextView tvAxisVX;
    protected TextView tvAxisVY;
    protected TextView tvAxisVZ;
    protected volatile float[] virtualAccel = new float[3];
    private LineGraphSeries<DataPoint> seriesx;
    private LineGraphSeries<DataPoint> seriesy;
    private LineGraphSeries<DataPoint> seriesz;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tvAxisVX = (TextView)findViewById(R.id.tv_vx_axis);
        tvAxisVY = (TextView)findViewById(R.id.tv_vy_axis);
        tvAxisVZ = (TextView)findViewById(R.id.tv_vz_axis);
        graph    = (GraphView) findViewById(R.id.graph);
        setupGraph();
    }

    private void setupGraph() {
        seriesx = new LineGraphSeries<>();
        seriesy = new LineGraphSeries<>();
        seriesz = new LineGraphSeries<>();

        seriesx.setColor(Color.BLUE);
        seriesy.setColor(Color.RED);
        seriesz.setColor(Color.GREEN);

        graph.addSeries(seriesx);
        graph.addSeries(seriesy);
        graph.addSeries(seriesz);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(20);

        graph.getViewport().setMinY(-1);
        graph.getViewport().setMaxY(2);
    }

    @Override
    protected void setUILayout() {
        setContentView(R.layout.activity_virtual_orientation);
    }

    @Override
    public void myOnClick(View v) {
        if(!mStartLogger) {
            graph.removeAllSeries();
            setupGraph();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        graph.removeAllSeries();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupGraph();
    }

    @Override
    public void myOnSensorChanged(SensorEvent event) {
        computeAccelOrientation();
    }

    public void computeAccelOrientation() {
        if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            float l[] = linear_acceleration;
            float m[][] = invertRotationMatrix(rotationMatrix);
            float r[] = new float[3];

            for(int i=0; i<3; i++) {
                for(int j=0; j<3; j++) {
                    for(int k=0; k<3; k++) {
                        r[i] = l[i] * m[k][j];
                    }
                }
            }

            System.arraycopy(r, 0, virtualAccel, 0, r.length);
        }
    }

//    Invert this matrix
//            /  M[ 0]   M[ 1]   M[ 2]  \
//            |  M[ 3]   M[ 4]   M[ 5]  |
//            \  M[ 6]   M[ 7]   M[ 8]  /
    float[][] invertRotationMatrix(float[] matrix) {
        float[][] result = new float[3][3];
        // first build matrix
        result[0][0] = matrix[0];
        result[0][1] = matrix[3];
        result[0][2] = matrix[6];

        result[1][0] = matrix[1];
        result[1][1] = matrix[4];
        result[1][2] = matrix[7];

        result[2][0] = matrix[2];
        result[2][1] = matrix[5];
        result[2][2] = matrix[8];

        return result;
    }

    @Override
    protected void updateUI() {
        super.updateUI();

        tvAxisVX.setText(String.format(Locale.US, "x: %.4f", virtualAccel[0]));
        tvAxisVY.setText(String.format(Locale.US, "y: %.4f", virtualAccel[1]));
        tvAxisVZ.setText(String.format(Locale.US, "z: %.4f", virtualAccel[2]));

        if(mStartLogger) {
            seriesx.appendData(new DataPoint(mTimestamp, (double) virtualAccel[0]), true, 400);
            seriesy.appendData(new DataPoint(mTimestamp, (double) virtualAccel[1]), true, 400);
            seriesz.appendData(new DataPoint(mTimestamp, (double) virtualAccel[2]), true, 400);
        }
    }
}
