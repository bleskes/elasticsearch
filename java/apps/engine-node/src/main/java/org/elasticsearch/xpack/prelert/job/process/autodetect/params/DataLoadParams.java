
package org.elasticsearch.xpack.prelert.job.process.autodetect.params;

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

