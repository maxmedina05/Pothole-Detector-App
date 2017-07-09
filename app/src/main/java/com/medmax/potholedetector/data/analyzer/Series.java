package com.medmax.potholedetector.data.analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Max Medina on 2017-07-08.
 */

public class Series {
    private List<Double> values;
    private String header;

    public Series() {
        values = new ArrayList<>();
    }

    public Series(String header) {
        this.header = header;
        values = new ArrayList<>();
    }

    public double computeMean(){
        double sum = 0;
        double mean = 0;
        double n = values.size();

        for (double x : values) {
            sum += x;
        }
        mean = sum / n;
        return mean;
    }

    public double computeStd(){
        double sum = 0;
        double std = 0;
        double mean = computeMean();
        double n = values.size();

        for (double x : values) {
            sum += (x - mean)*(x - mean);
        }

        std = Math.sqrt(sum / (n - 1));
        return std;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public Double getValue(int index) {
        return values.get(index);
    }

    public List<Double> getValues() {
        return values;
    }

    public void setValues(List<Double> values) {
        this.values.addAll(values);
    }

    public void setValues(Double[] values) {
        this.values.addAll(Arrays.asList(values));
    }

    public void addValue(double v) {
        values.add(v);
    }
}
