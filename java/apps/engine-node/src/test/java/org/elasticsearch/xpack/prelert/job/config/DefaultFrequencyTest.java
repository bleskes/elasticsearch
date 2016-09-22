
package org.elasticsearch.xpack.prelert.job.config;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;

import static org.junit.Assert.assertEquals;

public class DefaultFrequencyTest extends ESTestCase {

    @Rule
    public ExpectedException thrown= ExpectedException.none();

    public void testCalc_GivenNegative() {
        thrown.expect(IllegalArgumentException.class);
        DefaultFrequency.ofBucketSpan(-1);
    }


    public void testCalc() {
        assertEquals(Duration.ofMinutes(1), DefaultFrequency.ofBucketSpan(1));
        assertEquals(Duration.ofMinutes(1), DefaultFrequency.ofBucketSpan(30));
        assertEquals(Duration.ofMinutes(1), DefaultFrequency.ofBucketSpan(60));
        assertEquals(Duration.ofMinutes(1), DefaultFrequency.ofBucketSpan(90));
        assertEquals(Duration.ofMinutes(1), DefaultFrequency.ofBucketSpan(120));
        assertEquals(Duration.ofMinutes(1), DefaultFrequency.ofBucketSpan(121));

        assertEquals(Duration.ofSeconds(61), DefaultFrequency.ofBucketSpan(122));
        assertEquals(Duration.ofSeconds(75), DefaultFrequency.ofBucketSpan(150));
        assertEquals(Duration.ofSeconds(150), DefaultFrequency.ofBucketSpan(300));
        assertEquals(Duration.ofMinutes(10), DefaultFrequency.ofBucketSpan(1200));

        assertEquals(Duration.ofMinutes(10), DefaultFrequency.ofBucketSpan(1201));
        assertEquals(Duration.ofMinutes(10), DefaultFrequency.ofBucketSpan(1800));
        assertEquals(Duration.ofMinutes(10), DefaultFrequency.ofBucketSpan(3600));
        assertEquals(Duration.ofMinutes(10), DefaultFrequency.ofBucketSpan(7200));
        assertEquals(Duration.ofMinutes(10), DefaultFrequency.ofBucketSpan(12 * 3600));

        assertEquals(Duration.ofHours(1), DefaultFrequency.ofBucketSpan(12 * 3600 + 1));
        assertEquals(Duration.ofHours(1), DefaultFrequency.ofBucketSpan(13 * 3600));
        assertEquals(Duration.ofHours(1), DefaultFrequency.ofBucketSpan(24 * 3600));
        assertEquals(Duration.ofHours(1), DefaultFrequency.ofBucketSpan(48 * 3600));
    }
}
