
package org.elasticsearch.xpack.prelert.job.usage;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.persistence.UsagePersister;
import org.mockito.Mockito;


public class UsageReporterTest extends ESTestCase {
    public void testUpdatePeriod() throws JobException {
        // set the update interval to 1 secs
        System.setProperty(UsageReporter.UPDATE_INTERVAL_PROP, "1");

        UsagePersister persister = Mockito.mock(UsagePersister.class);
        UsageReporter usage = new UsageReporter("job1", persister, Mockito.mock(Logger.class));

        usage.addBytesRead(10);
        usage.addFieldsRecordsRead(5);

        assertEquals(10, usage.getBytesReadSinceLastReport());
        assertEquals(5, usage.getFieldsReadSinceLastReport());
        assertEquals(1, usage.getRecordsReadSinceLastReport());

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            assertTrue(false);
        }

        usage.addBytesRead(50);
        Mockito.verify(persister, Mockito.times(1)).persistUsage("job1", 60l, 5l, 1l);

        assertEquals(0, usage.getBytesReadSinceLastReport());
        assertEquals(0, usage.getFieldsReadSinceLastReport());
        assertEquals(0, usage.getRecordsReadSinceLastReport());


        // Write another
        usage.addBytesRead(20);
        usage.addFieldsRecordsRead(10);

        assertEquals(20, usage.getBytesReadSinceLastReport());
        assertEquals(10, usage.getFieldsReadSinceLastReport());
        assertEquals(1, usage.getRecordsReadSinceLastReport());


        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            assertTrue(false);
        }

        usage.addBytesRead(10);
        Mockito.verify(persister, Mockito.times(1)).persistUsage("job1", 30l, 10l, 1l);

        assertEquals(0, usage.getBytesReadSinceLastReport());
        assertEquals(0, usage.getFieldsReadSinceLastReport());
        assertEquals(0, usage.getRecordsReadSinceLastReport());

    }
}
