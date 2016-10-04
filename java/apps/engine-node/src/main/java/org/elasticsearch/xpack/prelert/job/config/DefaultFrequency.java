
package org.elasticsearch.xpack.prelert.job.config;

import java.time.Duration;

/**
 * Factory methods for a sensible default for the scheduler frequency
 */
public final class DefaultFrequency
{
    private static final int SECONDS_IN_MINUTE = 60;
    private static final int TWO_MINS_SECONDS = 2 * SECONDS_IN_MINUTE;
    private static final int TWENTY_MINS_SECONDS = 20 * SECONDS_IN_MINUTE;
    private static final int HALF_DAY_SECONDS = 12 * 60 * SECONDS_IN_MINUTE;
    private static final Duration TEN_MINUTES = Duration.ofMinutes(10);
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private DefaultFrequency()
    {
        // Do nothing
    }

    /**
     * Creates a sensible default frequency for a given bucket span.
     *
     * The default depends on the bucket span:
     * <ul>
     *   <li> <= 2 mins -> 1 min
     *   <li> <= 20 mins -> bucket span / 2
     *   <li> <= 12 hours -> 10 mins
     *   <li> > 12 hours -> 1 hour
     * </ul>
     *
     * @param bucketSpanSeconds the bucket span in seconds
     * @return the default frequency
     */
    public static Duration ofBucketSpan(long bucketSpanSeconds)
    {
        if (bucketSpanSeconds <= 0)
        {
            throw new IllegalArgumentException("Bucket span has to be > 0");
        }

        if (bucketSpanSeconds <= TWO_MINS_SECONDS)
        {
            return Duration.ofSeconds(SECONDS_IN_MINUTE);
        }
        if (bucketSpanSeconds <= TWENTY_MINS_SECONDS)
        {
            return Duration.ofSeconds(bucketSpanSeconds / 2);
        }
        if (bucketSpanSeconds <= HALF_DAY_SECONDS)
        {
            return TEN_MINUTES;
        }
        return ONE_HOUR;
    }
}
