package com.dt.sunenhao.pathplanning.Object;

import smile.math.distance.Metric;

public class PointMetric implements Metric {
    @Override
    public double d(Object x, Object y) {
        return ((MyPoint) x).getHarvesineDistance((MyPoint) y);
    }
}
