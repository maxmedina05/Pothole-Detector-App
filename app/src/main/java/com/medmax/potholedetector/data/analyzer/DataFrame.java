package com.medmax.potholedetector.data.analyzer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max Medina on 2017-07-08.
 */

public class DataFrame {
    private List<Series> series;

    public DataFrame() {
        series = new ArrayList<>();
    }

    public DataFrame query(String header, double begin, double end) {
        DataFrame df = new DataFrame();

        return df;
    }
}
