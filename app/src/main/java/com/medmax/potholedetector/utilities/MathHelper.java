package com.medmax.potholedetector.utilities;

/**
 * Created by Max Medina on 2017-07-13.
 */

public class MathHelper {


    public static double round(double value, int zeros) {
        double n = (zeros == 0) ? 1 : powTen(zeros);
        double nvalue = Math.round(value * n) / n;
        // System.out.println(powTen(4));
        return nvalue;
    }

    private static double powTen(int e) {
        double res = 1;
        if(e == 0) return 1;

        for(int i =0; i<e; i++) {
            res *= 10;
        }

        return res;
    }
}
