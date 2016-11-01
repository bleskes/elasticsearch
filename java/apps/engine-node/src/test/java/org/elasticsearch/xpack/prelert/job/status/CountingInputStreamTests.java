/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.job.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.persistence.UsagePersister;
import org.junit.Assert;
import org.mockito.Mockito;
import org.elasticsearch.xpack.prelert.job.usage.UsageReporter;


public class CountingInputStreamTests extends ESTestCase {

    public void testRead_OneByteAtATime() throws IOException {
        Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build();
        Environment env = new Environment(
                settings);
        UsageReporter usageReporter = new UsageReporter(settings, "foo", Mockito.mock(UsagePersister.class), Mockito.mock(Logger.class));
        DummyStatusReporter statusReporter = new DummyStatusReporter(env, usageReporter);

        final String TEXT = "123";
        InputStream source = new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8));

        try (CountingInputStream counting = new CountingInputStream(source,
                statusReporter)) {
            while (counting.read() >= 0) {
                ;
            }
            // an extra byte is read because we don't check the return
            // value of the read() method
            Assert.assertEquals(TEXT.length() + 1, usageReporter.getBytesReadSinceLastReport());

            Assert.assertEquals(usageReporter.getBytesReadSinceLastReport(),
                    statusReporter.getBytesRead());
        }
    }

    public void testRead_WithBuffer() throws IOException {
        Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build();
        Environment env = new Environment(
                settings);
        final String TEXT = "To the man who only has a hammer,"
                + " everything he encounters begins to look like a nail.";

        UsageReporter usageReporter = new UsageReporter(settings, "foo", Mockito.mock(UsagePersister.class), Mockito.mock(Logger.class));
        DummyStatusReporter statusReporter = new DummyStatusReporter(env, usageReporter);

        InputStream source = new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8));

        try (CountingInputStream counting = new CountingInputStream(source,
                statusReporter)) {
            byte buf[] = new byte[256];
            while (counting.read(buf) >= 0) {
                ;
            }
            // one less byte is reported because we don't check
            // the return value of the read() method
            Assert.assertEquals(TEXT.length() - 1, usageReporter.getBytesReadSinceLastReport());

            Assert.assertEquals(usageReporter.getBytesReadSinceLastReport(),
                    statusReporter.getBytesRead());
        }
    }

    public void testRead_WithTinyBuffer() throws IOException {
        Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build();
        Environment env = new Environment(
                settings);
        final String TEXT = "To the man who only has a hammer,"
                + " everything he encounters begins to look like a nail.";

        UsageReporter usageReporter = new UsageReporter(settings, "foo", Mockito.mock(UsagePersister.class), Mockito.mock(Logger.class));
        DummyStatusReporter statusReporter = new DummyStatusReporter(env, usageReporter);

        InputStream source = new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8));

        try (CountingInputStream counting = new CountingInputStream(source,
                statusReporter)) {
            byte buf[] = new byte[8];
            while (counting.read(buf, 0, 8) >= 0) {
                ;
            }
            // an extra byte is read because we don't check the return
            // value of the read() method
            Assert.assertEquals(TEXT.length() - 1, usageReporter.getBytesReadSinceLastReport());

            Assert.assertEquals(usageReporter.getBytesReadSinceLastReport(),
                    statusReporter.getBytesRead());
        }

    }

}
