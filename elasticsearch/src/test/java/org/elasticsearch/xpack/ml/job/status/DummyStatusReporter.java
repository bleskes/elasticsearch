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
package org.elasticsearch.xpack.ml.job.status;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.ml.job.DataCounts;
import org.elasticsearch.xpack.ml.job.persistence.JobDataCountsPersister;
import org.elasticsearch.xpack.ml.job.usage.UsageReporter;

import static org.mockito.Mockito.mock;

/**
 * Dummy StatusReporter for testing
 */
class DummyStatusReporter extends StatusReporter {

    int logStatusCallCount = 0;

    public DummyStatusReporter(UsageReporter usageReporter) {
        super(mock(ThreadPool.class), Settings.EMPTY, "DummyJobId", new DataCounts("DummyJobId"),
                usageReporter, mock(JobDataCountsPersister.class));
    }

    /**
     * It's difficult to use mocking to get the number of calls to {@link #logStatus(long)}
     * and Mockito.spy() doesn't work due to the lambdas used in {@link StatusReporter}.
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
}
