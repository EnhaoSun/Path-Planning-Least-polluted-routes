package com.example.demo.MapService;

import smile.math.distance.Metric;

/**
 * @author Enhao Sun
 * @version 2019-06-12.
 */
public class PointMetric<E> implements Metric<E> {
    @Override
    public double d(Object t0, Object t1) {
        return ((Point) t0).getHarvesineDistance((Point) t1);
    }
}
