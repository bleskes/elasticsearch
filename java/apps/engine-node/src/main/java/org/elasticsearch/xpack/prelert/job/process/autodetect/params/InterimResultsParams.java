
package org.elasticsearch.xpack.prelert.job.process.autodetect.params;

import java.util.Objects;

public class InterimResultsParams {
    private final boolean calcInterim;
    private final TimeRange timeRange;
    private final Long advanceTimeSeconds;

    private InterimResultsParams(boolean calcInterim, TimeRange timeRange, Long advanceTimeSeconds) {
        this.calcInterim = calcInterim;
        this.timeRange = Objects.requireNonNull(timeRange);
        this.advanceTimeSeconds = advanceTimeSeconds;
    }

    public boolean shouldCalculateInterim() {
        return calcInterim;
    }

    public boolean shouldAdvanceTime() {
        return advanceTimeSeconds != null;
    }

    public String getStart() {
        return timeRange.getStart();
    }

    public String getEnd() {
        return timeRange.getEnd();
    }

    public long getAdvanceTime() {
        if (!shouldAdvanceTime()) {
            throw new IllegalStateException();
        }
        return advanceTimeSeconds;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private boolean calcInterim;
        private TimeRange timeRange;
        private Long advanceTimeSeconds;

        private Builder() {
            calcInterim = false;
            timeRange = new TimeRange(null, null);
            advanceTimeSeconds = null;
        }

        public Builder calcInterim(boolean value) {
            calcInterim = value;
            return this;
        }

        public Builder forTimeRange(Long startSeconds, Long endSeconds) {
            return forTimeRange(new TimeRange(startSeconds, endSeconds));
        }

        public Builder forTimeRange(TimeRange timeRange) {
            this.timeRange = timeRange;
            return this;
        }

        public Builder advanceTime(long targetSeconds) {
            advanceTimeSeconds = targetSeconds;
            return this;
        }

        public InterimResultsParams build() {
            return new InterimResultsParams(calcInterim, timeRange, advanceTimeSeconds);
        }
    }
}
