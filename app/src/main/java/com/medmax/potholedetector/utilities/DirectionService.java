package com.medmax.potholedetector.utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max Medina on 2017-10-04.
 */

public class DirectionService {

    private List<Coordinate> coordinates;

    public DirectionService() {
        coordinates = new ArrayList<>();
    }

    public void addCoordinate(float latitude, float longitude) {
        if(coordinates.size() == 10) {
            coordinates.clear();
        }
        coordinates.add(new Coordinate(latitude, longitude));
    }

    public float[] dd2dms(float deg) {
        int d = (int) deg;
        float md = Math.abs(deg - d) * 60;
        int m = (int) md;
        float s = (md - m) * 60;

        return new float[] {d, m, s};
    }

    private boolean isArrayIncreasing(float[] arr) {
        for (int i=1; i<arr.length; i++) {
            if(arr[i] < arr[i-1])
                return false;
        }
        return true;
    }

    public char determineDirection(Coordinate[] coors) {
        List<Float> slat = new ArrayList<>();
        List<Float> slng = new ArrayList<>();

        for(Coordinate c : coors) {
            float[] lat = dd2dms(c.getLatitude());
            float[] lng = dd2dms(c.getLongitude());

            slat.add(lat[2]);
            slng.add(lng[2]);
        }


        float latmean = computeMean(slat.toArray(new Float[slat.size()]));
        float lngmean = computeMean(slng.toArray(new Float[slng.size()]));
//        float latstd = computeStd()

        return 'C';
    }

    public float computeMean(Float[] X){
        float sum = 0;
        float mean = 0;
        float N = X.length;

        for (float x : X) {
            sum += x;
        }
        mean = sum / N;
        return mean;
    }

    public float computeStd(Float[] X, float mean){
        float sum = 0;
        float std = 0;
        float N = X.length;

        for (float x : X) {
            sum += (x - mean)*(x - mean);
        }

        std = (float) Math.sqrt(sum / (N - 1));
        return std;
    }

    public class Coordinate {
        float latitude = 0;
        float longitude = 0;

        public Coordinate() {
        }

        public Coordinate(float latitude, float longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public float getLatitude() {
            return latitude;
        }

        public void setLatitude(float latitude) {
            this.latitude = latitude;
        }

        public float getLongitude() {
            return longitude;
        }

        public void setLongitude(float longitude) {
            this.longitude = longitude;
        }
    }
}
