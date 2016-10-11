
package org.elasticsearch.xpack.prelert.job.process.autodetect.params;

import java.util.Objects;

public class DataLoadParams {
    private final boolean isPersisting;
    private final TimeRange resetTimeRange;
    private final boolean ignoreDowntime;

    public DataLoadParams(boolean isPersisting, TimeRange resetTimeRange) {
        this(isPersisting, resetTimeRange, false);
    }

    public DataLoadParams(boolean isPersisting, TimeRange resetTimeRange, boolean ignoreDowntime) {
        this.isPersisting = isPersisting;
        this.resetTimeRange = Objects.requireNonNull(resetTimeRange);
        this.ignoreDowntime = ignoreDowntime;
    }

    public boolean isPersisting() {
        return isPersisting;
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

