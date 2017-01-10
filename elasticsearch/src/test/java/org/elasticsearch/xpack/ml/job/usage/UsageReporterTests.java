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
package org.elasticsearch.xpack.ml.job.usage;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.persistence.UsagePersister;
import org.mockito.Mockito;

public class UsageReporterTests extends ESTestCase {
    public void testUpdatePeriod() {
        Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString())
                .put(UsageReporter.UPDATE_INTERVAL_SETTING.getKey(), 1).build();

        UsagePersister persister = Mockito.mock(UsagePersister.class);
        UsageReporter usage = new UsageReporter(settings, "job1", persister);

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
        Mockito.verify(persister, Mockito.times(1)).persistUsage("job1", 60L, 5L, 1L);

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
        Mockito.verify(persister, Mockito.times(1)).persistUsage("job1", 30L, 10L, 1L);

        assertEquals(0, usage.getBytesReadSinceLastReport());
        assertEquals(0, usage.getFieldsReadSinceLastReport());
        assertEquals(0, usage.getRecordsReadSinceLastReport());

    }
}
