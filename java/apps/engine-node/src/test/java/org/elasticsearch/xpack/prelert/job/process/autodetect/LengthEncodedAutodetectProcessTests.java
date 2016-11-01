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
package org.elasticsearch.xpack.prelert.job.process.autodetect;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.TimeRange;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.ControlMsgToProcessWriter;
import org.junit.Assert;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

public class LengthEncodedAutodetectProcessTests extends ESTestCase {

    private static final int NUMBER_ANALYSIS_FIELDS = 3;

    public void testProcessStartTime() throws InterruptedException {
        LengthEncodedAutodetectProcess process = new LengthEncodedAutodetectProcess(Mockito.mock(OutputStream.class),
                NUMBER_ANALYSIS_FIELDS);

        ZonedDateTime startTime = process.getProcessStartTime();
        Thread.sleep(500);
        ZonedDateTime now = ZonedDateTime.now();
        assertTrue(now.isAfter(startTime));

        ZonedDateTime startPlus3 = startTime.plus(3, ChronoUnit.SECONDS);
        assertTrue(now.isBefore(startPlus3));
    }

    public void testErrorStream() throws IOException {
        LengthEncodedAutodetectProcess process = new LengthEncodedAutodetectProcess(Mockito.mock(OutputStream.class),
                NUMBER_ANALYSIS_FIELDS);
        assertNotNull(process.error());
        assertThat(process.error().read(), is(equalTo(-1)));
    }

    public void testOutputStream() throws IOException {
        LengthEncodedAutodetectProcess process = new LengthEncodedAutodetectProcess(Mockito.mock(OutputStream.class),
                NUMBER_ANALYSIS_FIELDS);
        assertNotNull(process.out());
        assertThat(process.out().read(), is(equalTo(-1)));
    }

    public void testWriteRecord() throws IOException {
        String[] record = {"r1", "r2", "r3", "r4", "r5"};

        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

        LengthEncodedAutodetectProcess process = new LengthEncodedAutodetectProcess(bos, NUMBER_ANALYSIS_FIELDS);

        process.writeRecord(record);

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

    public void testFlush() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(ControlMsgToProcessWriter.FLUSH_SPACES_LENGTH + 1024);
        LengthEncodedAutodetectProcess process = new LengthEncodedAutodetectProcess(bos, NUMBER_ANALYSIS_FIELDS);

        InterimResultsParams params = InterimResultsParams.builder().build();
        process.flushJob(params);

        ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
        assertThat(bb.remaining(), is(greaterThan(ControlMsgToProcessWriter.FLUSH_SPACES_LENGTH)));
    }

    public void testWriteResetBucketsControlMessage() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        LengthEncodedAutodetectProcess process = new LengthEncodedAutodetectProcess(bos, NUMBER_ANALYSIS_FIELDS);

        DataLoadParams params = new DataLoadParams(TimeRange.builder().startTime("1").endTime("86400").build(), true);
        process.writeResetBucketsControlMessage(params);

        String message = new String(bos.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(message.contains(ControlMsgToProcessWriter.RESET_BUCKETS_MESSAGE_CODE));
    }

    public void testWriteUpdateConfigMessage() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        LengthEncodedAutodetectProcess process = new LengthEncodedAutodetectProcess(bos, NUMBER_ANALYSIS_FIELDS);

        process.writeUpdateConfigMessage("");

        String message = new String(bos.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(message.contains(ControlMsgToProcessWriter.UPDATE_MESSAGE_CODE));
    }

    public void testClose() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        LengthEncodedAutodetectProcess process = new LengthEncodedAutodetectProcess(bos, NUMBER_ANALYSIS_FIELDS);

        assertThat(process.error().available(), is(equalTo(0)));
        assertThat(process.out().available(), is(equalTo(0)));
    }

}
