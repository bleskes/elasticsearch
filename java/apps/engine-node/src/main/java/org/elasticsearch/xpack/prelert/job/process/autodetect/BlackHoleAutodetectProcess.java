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

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.FlushAcknowledgement;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;

/**
 * A placeholder class simulating the actions of the native Autodetect process.
 * Most methods consume data without performing any action however, after a call to
 * {@link #flushJob(InterimResultsParams)} a {@link org.elasticsearch.xpack.prelert.job.process.autodetect.output.FlushAcknowledgement}
 * message is expected on the {@link #getProcessOutStream()} stream. This class writes the flush
 * acknowledgement immediately.
 */
public class BlackHoleAutodetectProcess implements AutodetectProcess, Closeable {

    private static final Logger LOGGER = Loggers.getLogger(BlackHoleAutodetectProcess.class);
    private static final String FLUSH_ID = "flush-1";

    private final PipedInputStream processOutStream;
    private final PipedInputStream persistStream;
    private PipedOutputStream pipedProcessOutStream;
    private PipedOutputStream pipedPersistStream;
    private final ZonedDateTime startTime;

    public BlackHoleAutodetectProcess() {
        processOutStream = new PipedInputStream();
        persistStream = new PipedInputStream();
        try {
            pipedProcessOutStream = new PipedOutputStream(processOutStream);
            pipedPersistStream = new PipedOutputStream(persistStream);
        } catch (IOException e) {
            LOGGER.error("Error connecting PipedOutputStream", e);
        }
        startTime = ZonedDateTime.now();
    }

    @Override
    public void writeRecord(String[] record) throws IOException {
    }

    @Override
    public void writeResetBucketsControlMessage(DataLoadParams params) throws IOException {
    }

    @Override
    public void writeUpdateConfigMessage(String config) throws IOException {
    }

    /**
     * Accept the request do nothing with it but write the flush acknowledgement to {@link #getProcessOutStream()}
     * @param params Should interim results be generated
     * @return {@link #FLUSH_ID}
     */
    @Override
    public String flushJob(InterimResultsParams params) throws IOException {
        FlushAcknowledgement flushAcknowledgement = new FlushAcknowledgement(FLUSH_ID);
        pipedProcessOutStream
        .write(flushAcknowledgement.toXContent(XContentFactory.jsonBuilder(), ToXContent.EMPTY_PARAMS).string()
                        .getBytes(StandardCharsets.UTF_8));
        pipedProcessOutStream.flush();
        return FLUSH_ID;
    }

    @Override
    public void flushStream() throws IOException {
    }

    @Override
    public void close() throws IOException {
        pipedProcessOutStream.close();
        pipedPersistStream.close();
    }

    @Override
    public InputStream getProcessOutStream() {
        return processOutStream;
    }

    @Override
    public InputStream getPersistStream() {
        return persistStream;
    }

    @Override
    public ZonedDateTime getProcessStartTime() {
        return startTime;
    }

    @Override
    public boolean isProcessAlive() {
        return true;
    }

    @Override
    public String readError() {
        return "";
    }
}
