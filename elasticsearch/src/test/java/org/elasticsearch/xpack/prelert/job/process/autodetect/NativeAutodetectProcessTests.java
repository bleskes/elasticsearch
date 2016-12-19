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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class NativeAutodetectProcessTests extends ESTestCase {

    private static final int NUMBER_ANALYSIS_FIELDS = 3;

    public void testProcessStartTime() throws Exception {
        InputStream logStream = Mockito.mock(InputStream.class);
        when(logStream.read(new byte[1024])).thenReturn(-1);
        try (NativeAutodetectProcess process = new NativeAutodetectProcess("foo", logStream,
                Mockito.mock(OutputStream.class), Mockito.mock(InputStream.class), Mockito.mock(InputStream.class),
                NUMBER_ANALYSIS_FIELDS, null, EsExecutors.newDirectExecutorService())) {

            ZonedDateTime startTime = process.getProcessStartTime();
            Thread.sleep(500);
            ZonedDateTime now = ZonedDateTime.now();
            assertTrue(now.isAfter(startTime));

            ZonedDateTime startPlus3 = startTime.plus(3, ChronoUnit.SECONDS);
            assertTrue(now.isBefore(startPlus3));
        }
    }

    public void testWriteRecord() throws IOException {
        InputStream logStream = Mockito.mock(InputStream.class);
        when(logStream.read(new byte[1024])).thenReturn(-1);
        String[] record = {"r1", "r2", "r3", "r4", "r5"};
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        try (NativeAutodetectProcess process = new NativeAutodetectProcess("foo", logStream,
                bos, Mockito.mock(InputStream.class), Mockito.mock(InputStream.class),
                NUMBER_ANALYSIS_FIELDS, Collections.emptyList(), EsExecutors.newDirectExecutorService())) {

            process.writeRecord(record);
            process.flushStream();

            ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

            // read header
            int numFields = bb.getInt();
            Assert.assertEquals(record.length, numFields);
            for (int i = 0; i < numFields; i++) {
                int recordSize = bb.getInt();
                assertEquals(2, recordSize);

                byte[] charBuff = new byte[recordSize];
                for (int j = 0; j < recordSize; j++) {
                    charBuff[j] = bb.get();
                }

                String value = new String(charBuff, StandardCharsets.UTF_8);
                Assert.assertEquals(record[i], value);
            }
        }
    }

    public void testFlush() throws IOException {
        InputStream logStream = Mockito.mock(InputStream.class);
        when(logStream.read(new byte[1024])).thenReturn(-1);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(ControlMsgToProcessWriter.FLUSH_SPACES_LENGTH + 1024);
        try (NativeAutodetectProcess process = new NativeAutodetectProcess("foo", logStream,
                bos, Mockito.mock(InputStream.class), Mockito.mock(InputStream.class),
                NUMBER_ANALYSIS_FIELDS, Collections.emptyList(), EsExecutors.newDirectExecutorService())) {

            InterimResultsParams params = InterimResultsParams.builder().build();
            process.flushJob(params);

            ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
            assertThat(bb.remaining(), is(greaterThan(ControlMsgToProcessWriter.FLUSH_SPACES_LENGTH)));
        }
    }

    public void testWriteResetBucketsControlMessage() throws IOException {
        InputStream logStream = Mockito.mock(InputStream.class);
        when(logStream.read(new byte[1024])).thenReturn(-1);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        try (NativeAutodetectProcess process = new NativeAutodetectProcess("foo", logStream,
                bos, Mockito.mock(InputStream.class), Mockito.mock(InputStream.class),
                NUMBER_ANALYSIS_FIELDS, Collections.emptyList(), EsExecutors.newDirectExecutorService())) {

            DataLoadParams params = new DataLoadParams(TimeRange.builder().startTime("1").endTime("86400").build(), true);
            process.writeResetBucketsControlMessage(params);
            process.flushStream();

            String message = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            assertTrue(message.contains(ControlMsgToProcessWriter.RESET_BUCKETS_MESSAGE_CODE));
        }
    }

    public void testWriteUpdateConfigMessage() throws IOException {
        InputStream logStream = Mockito.mock(InputStream.class);
        when(logStream.read(new byte[1024])).thenReturn(-1);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        try (NativeAutodetectProcess process = new NativeAutodetectProcess("foo", logStream,
                bos, Mockito.mock(InputStream.class), Mockito.mock(InputStream.class),
                NUMBER_ANALYSIS_FIELDS, Collections.emptyList(), EsExecutors.newDirectExecutorService())) {

            process.writeUpdateConfigMessage("");
            process.flushStream();

            String message = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            assertTrue(message.contains(ControlMsgToProcessWriter.UPDATE_MESSAGE_CODE));
        }
    }
}
