
package org.elasticsearch.xpack.prelert.job;

import java.util.Objects;

public class SchedulerState {
    public static final String TYPE = "schedulerState";

    public static final String START_TIME_MILLIS = "startTimeMillis";
    public static final String END_TIME_MILLIS = "endTimeMillis";

    private Long startTimeMillis;
    private Long endTimeMillis;

    public SchedulerState()
    {
        // Default constructor needed for serialization
    }

    public SchedulerState(Long startTimeMillis, Long endTimeMillis)
    {
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
    }

    public Long getStartTimeMillis()
    {
        return startTimeMillis;
    }

    public void setStartTimeMillis(Long startTimeMillis)
    {
        this.startTimeMillis = startTimeMillis;
    }

    public Long getEndTimeMillis()
    {
        return endTimeMillis;
    }

    public void setEndTimeMillis(Long endTimeMillis)
    {
        this.endTimeMillis = endTimeMillis;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (other instanceof SchedulerState == false)
        {
            return false;
        }

        SchedulerState that = (SchedulerState) other;

        return Objects.equals(this.startTimeMillis, that.startTimeMillis) &&
                Objects.equals(this.endTimeMillis, that.endTimeMillis);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(startTimeMillis, endTimeMillis);
    }
}
