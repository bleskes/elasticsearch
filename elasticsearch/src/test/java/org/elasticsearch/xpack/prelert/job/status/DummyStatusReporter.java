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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.prelert.job.persistence.JobDataCountsPersister;
import org.elasticsearch.xpack.prelert.job.usage.UsageReporter;

import static org.mockito.Mockito.mock;

/**
 * Dummy StatusReporter for testing abstract class
 */
class DummyStatusReporter extends StatusReporter {

    DummyStatusReporter(UsageReporter usageReporter) {
        super(Settings.EMPTY, "DummyJobId", usageReporter, mock(JobDataCountsPersister.class));
    }

}
