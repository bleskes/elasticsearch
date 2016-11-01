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

import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.persistence.JobDataCountsPersister;
import org.elasticsearch.xpack.prelert.job.usage.UsageReporter;

/**
 * Dummy StatusReporter for testing abstract class
 */
public class DummyStatusReporter extends StatusReporter {
    boolean statusReported = false;

    public DummyStatusReporter(Environment env, UsageReporter usageReporter) {
        super(env, env.settings(), "DummyJobId", usageReporter, new JobDataCountsPersister() {
            @Override
            public void persistDataCounts(String jobId, DataCounts counts) {

            }
        }, null, 1);
    }

    public DummyStatusReporter(Environment env, DataCounts counts,
            UsageReporter usageReporter) {
        super(env, env.settings(), "DummyJobId", counts, usageReporter, new JobDataCountsPersister() {
            @Override
            public void persistDataCounts(String jobId, DataCounts counts) {

            }
        }, null, 1);
    }


    public boolean isStatusReported() {
        return statusReported;
    }
}
