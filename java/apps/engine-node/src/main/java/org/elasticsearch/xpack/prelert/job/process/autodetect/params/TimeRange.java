
package org.elasticsearch.xpack.prelert.job.process.autodetect.params;

public class TimeRange {
    private final Long start;
    private final Long end;

    public TimeRange(Long start, Long end) {
        this.start = start;
        this.end = end;
    }

    public String getStart() {
        return start == null ? "" : String.valueOf(start);
    }

    public String getEnd() {
        return end == null ? "" : String.valueOf(end);
    }
}
