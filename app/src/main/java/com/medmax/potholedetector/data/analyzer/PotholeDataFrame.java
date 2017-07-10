package com.medmax.potholedetector.data.analyzer;

import com.medmax.potholedetector.models.AccData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max Medina on 2017-07-08.
 */

public class PotholeDataFrame {
    private List<AccData> dataframe;

    public PotholeDataFrame() {
        dataframe = new ArrayList<>();
    }

    public PotholeDataFrame(List<AccData> dataframe) {
        this.dataframe = dataframe;
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
        double sum = 0;
        double mean = 0;
        double n = dataframe.size();

        for (AccData x : dataframe) {
            sum += x.getzAxis();
        }
        mean = sum / n;
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

    public void addRow(AccData row) {
        dataframe.add(row);
    }

    public void clear() {
        dataframe.clear();
    }
}
