/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.job.process.autodetect.params;

import java.util.Objects;

public class DataLoadParams {
    private final TimeRange resetTimeRange;
    private final boolean ignoreDowntime;

    public DataLoadParams(TimeRange resetTimeRange) {
        this(resetTimeRange, false);
    }

    public DataLoadParams(TimeRange resetTimeRange, boolean ignoreDowntime) {
        this.resetTimeRange = Objects.requireNonNull(resetTimeRange);
        this.ignoreDowntime = ignoreDowntime;
    }

    public boolean isResettingBuckets() {
        return !getStart().isEmpty();
    }

    public String getStart() {
        return resetTimeRange.getStart();
    }

    public String getEnd() {
        return resetTimeRange.getEnd();
    }

    public boolean isIgnoreDowntime() {
        return ignoreDowntime;
    }
}

