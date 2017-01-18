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

import org.elasticsearch.xpack.ml.job.DataDescription;

import java.util.Objects;
import java.util.Optional;

public class DataLoadParams {
    private final TimeRange resetTimeRange;
    private final boolean ignoreDowntime;
    private final Optional<DataDescription> dataDescription;

    public DataLoadParams(TimeRange resetTimeRange, boolean ignoreDowntime, Optional<DataDescription> dataDescription) {
        this.resetTimeRange = Objects.requireNonNull(resetTimeRange);
        this.ignoreDowntime = ignoreDowntime;
        this.dataDescription = Objects.requireNonNull(dataDescription);
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

    public Optional<DataDescription> getDataDescription() {
        return dataDescription;
    }
}

