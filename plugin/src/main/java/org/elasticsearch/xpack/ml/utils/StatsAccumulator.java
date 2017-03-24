/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to collect min, max, avg and total statistics for a quantity
 */
public class StatsAccumulator {

    private static final String MIN = "min";
    private static final String MAX = "max";
    private static final String AVG = "avg";
    private static final String TOTAL = "total";

    private long count;
    private double total;
    private Double min;
    private Double max;

    public void add(double value) {
        count++;
        total += value;
        min = min == null ? value : (value < min ? value : min);
        max = max == null ? value : (value > max ? value : max);
    }

    public double getMin() {
        return min == null ? 0.0 : min;
    }

    public double getMax() {
        return max == null ? 0.0 : max;
    }

    public double getAvg() {
        return count == 0.0 ? 0.0 : total/count;
    }

    public double getTotal() {
        return total;
    }

    public Map<String, Double> asMap() {
        Map<String, Double> map = new HashMap<>();
        map.put(MIN, getMin());
        map.put(MAX, getMax());
        map.put(AVG, getAvg());
        map.put(TOTAL, getTotal());
        return map;
    }
}
