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
package org.elasticsearch.xpack.ml.job.process;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.ml.job.config.AnalysisConfig;
import org.elasticsearch.xpack.ml.job.config.Detector;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.persistence.JobDataCountsPersister;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCounts;

import java.util.Arrays;
import java.util.Date;

import static org.mockito.Mockito.mock;

/**
 * Dummy DataCountsReporter for testing
 */
class DummyDataCountsReporter extends DataCountsReporter {

    int logStatusCallCount = 0;

    DummyDataCountsReporter() {  
        super(mock(ThreadPool.class), Settings.EMPTY, createJob(), new DataCounts("DummyJobId"),
                mock(JobDataCountsPersister.class));
    }

    /**
     * It's difficult to use mocking to get the number of calls to {@link #logStatus(long)}
     * and Mockito.spy() doesn't work due to the lambdas used in {@link DataCountsReporter}.
     * Override the method here an count the calls
     */
    @Override
    protected void logStatus(long totalRecords) {
        super.logStatus(totalRecords);
        ++logStatusCallCount;
    }


    /**
     * @return Then number of times {@link #logStatus(long)} was called.
     */
    public int getLogStatusCallCount() {
        return logStatusCallCount;
    }

    @Override
    public void close() {
        // Do nothing
    }
    
    private static Job createJob() {
        AnalysisConfig.Builder acBuilder = new AnalysisConfig.Builder(
                Arrays.asList(new Detector.Builder("metric", "field").build()));
        acBuilder.setBucketSpan(TimeValue.timeValueSeconds(300));
        acBuilder.setLatency(TimeValue.ZERO);
        acBuilder.setDetectors(Arrays.asList(new Detector.Builder("metric", "field").build()));

        Job.Builder builder = new Job.Builder("dummy_job_id");
        builder.setAnalysisConfig(acBuilder);
        return builder.build(new Date());
    }
}
