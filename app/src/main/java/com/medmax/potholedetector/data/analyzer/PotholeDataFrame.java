package com.medmax.potholedetector.data.analyzer;

import com.medmax.potholedetector.models.AccData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max Medina on 2017-07-08.
 *
 * Description: Record only 3 seconds of data
 * when it has 3 second of data delete the last second.
 *
 */

public class PotholeDataFrame {
    public static final int MAX_TIME_RECORDED = 3;
    public static final int LAST_SECOND_TIME = 1;
    private List<AccData> dataframe;
    private List<AccData> lastdf;
    private double mStartTime = 0;
    private double mMean = 0;
    private boolean isMeanCalculated = false;

    public PotholeDataFrame() {
        dataframe = new ArrayList<>();
        lastdf = new ArrayList<>();
    }

    public PotholeDataFrame(List<AccData> dataframe) {
        this.dataframe = dataframe;
        lastdf = new ArrayList<>();
    }

    public PotholeDataFrame query(double begin, double end){
        List<AccData> df = new ArrayList<>();

        for (AccData row : dataframe) {
            if(row.getTimestamp() >= begin && row.getTimestamp() <= end) {
                df.add(row);
            }
        }

        return new PotholeDataFrame(df);
    }

    public double computeMean(){
        if(isMeanCalculated) {
            return mMean;
        }

        double sum = 0;
        double mean = 0;
        double n = dataframe.size();

        for (AccData x : dataframe) {
            sum += x.getzAxis();
        }
        mMean = mean = sum / n;
        isMeanCalculated = true;
        return mean;
    }

    public double computeStd(){
        double sum = 0;
        double std = 0;
        double mean = computeMean();
        double n = dataframe.size();

        for (AccData acd : dataframe) {
            double x = acd.getzAxis();

            sum += (x - mean)*(x - mean);
        }

        std = Math.sqrt(sum / (n - 1));
        return std;
    }

    public double computeMax(){
        double max = dataframe.get(0).getzAxis();

        for (AccData acd : dataframe) {
            double x = acd.getzAxis();
            if(x > max) {
                max = x;
            }
        }
        return max;
    }

    /**
     * Add a new row but with conditions
     * @param row
     * it will only record 3 seconds when 3 seconds has been reached it will replace the list with
     * the last 1 recorded.
     * steps:
     *      1. check if it's empty if it is then that's the first record and set the start time.
     *      2. verify if 3 seconds has pass
     *          if yes: reset the dataframe; dataframe = lastdf
     *          if no: check if 2 seconds has pass
     *              if yes: add to the lastdf
     */
    public void addRow(AccData row) {
        dataframe.add(row);
        isMeanCalculated = false;

        double currentTime = row.getTimestamp();
        if(dataframe.size() == 1) {
            mStartTime = row.getTimestamp();
        }

        // It's almost full it has recorded at least 2 seconds
        if((currentTime - mStartTime) > (MAX_TIME_RECORDED - LAST_SECOND_TIME)) {
            lastdf.add(row);
        }

        // MAX_TIME_RECORDED REACHED!
        if((currentTime - mStartTime) >= MAX_TIME_RECORDED) {
            dataframe.clear();
            // dataframe.addAll(lastdf);
            dataframe = lastdf;
            lastdf = new ArrayList<>();

            // reset time
            mStartTime = currentTime;
        }

    }
}
