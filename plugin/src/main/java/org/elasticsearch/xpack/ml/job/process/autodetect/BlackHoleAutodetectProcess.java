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
package org.elasticsearch.xpack.ml.job.process.autodetect;

import org.elasticsearch.xpack.ml.job.config.DetectionRule;
import org.elasticsearch.xpack.ml.job.config.ModelPlotConfig;
import org.elasticsearch.xpack.ml.job.persistence.StateStreamer;
import org.elasticsearch.xpack.ml.job.process.autodetect.output.FlushAcknowledgement;
import org.elasticsearch.xpack.ml.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.ml.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSnapshot;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.Quantiles;
import org.elasticsearch.xpack.ml.job.results.AutodetectResult;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * A placeholder class simulating the actions of the native Autodetect process.
 * Most methods consume data without performing any action however, after a call to
 * {@link #flushJob(InterimResultsParams)} a {@link org.elasticsearch.xpack.ml.job.process.autodetect.output.FlushAcknowledgement}
 * message is expected on the {@link #readAutodetectResults()} ()} stream. This class writes the flush
 * acknowledgement immediately.
 */
public class BlackHoleAutodetectProcess implements AutodetectProcess {

    private static final String FLUSH_ID = "flush-1";

    private final String jobId;
    private final ZonedDateTime startTime;
    private final BlockingQueue<AutodetectResult> results = new LinkedBlockingDeque<>();
    private volatile boolean open = true;

    public BlackHoleAutodetectProcess(String jobId) {
        this.jobId = jobId;
        startTime = ZonedDateTime.now();
    }

    @Override
    public void restoreState(StateStreamer stateStreamer, ModelSnapshot modelSnapshot) {
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void writeRecord(String[] record) throws IOException {
    }

    @Override
    public void writeResetBucketsControlMessage(DataLoadParams params) throws IOException {
    }

    @Override
    public void writeUpdateModelPlotMessage(ModelPlotConfig modelPlotConfig) throws IOException {
    }

    @Override
    public void writeUpdateDetectorRulesMessage(int detectorIndex, List<DetectionRule> rules) throws IOException {
    }

    /**
     * Accept the request do nothing with it but write the flush acknowledgement to {@link #readAutodetectResults()}
     * @param params Should interim results be generated
     * @return {@link #FLUSH_ID}
     */
    @Override
    public String flushJob(InterimResultsParams params) throws IOException {
        FlushAcknowledgement flushAcknowledgement = new FlushAcknowledgement(FLUSH_ID);
        AutodetectResult result = new AutodetectResult(null, null, null, null, null, null, null, null, flushAcknowledgement);
        results.add(result);
        return FLUSH_ID;
    }

    @Override
    public void flushStream() throws IOException {
    }

    @Override
    public void close() throws IOException {
        if (open) {
            Quantiles quantiles = new Quantiles(jobId, new Date(), "black hole quantiles");
            AutodetectResult result = new AutodetectResult(null, null, null, quantiles, null, null, null, null, null);
            results.add(result);
            open = false;
        }
    }

    @Override
    public void kill() throws IOException {
        open = false;
    }

    @Override
    public Iterator<AutodetectResult> readAutodetectResults() {
        // Create a custom iterator here, because LinkedBlockingDeque iterator and stream are not blocking when empty:
        return new Iterator<AutodetectResult>() {

            AutodetectResult result;

            @Override
            public boolean hasNext() {
                try {
                    while (open) {
                        result = results.poll(100, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            return true;
                        }
                    }
                    result = results.poll();
                    return result != null;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            @Override
            public AutodetectResult next() {
                return result;
            }
        };
    }

    @Override
    public ZonedDateTime getProcessStartTime() {
        return startTime;
    }

    @Override
    public boolean isProcessAlive() {
        return open;
    }

    @Override
    public String readError() {
        return "";
    }
}
