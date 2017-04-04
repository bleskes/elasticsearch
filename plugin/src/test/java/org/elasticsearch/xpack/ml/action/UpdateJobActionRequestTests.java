/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.action;

import org.elasticsearch.xpack.ml.job.config.AnalysisLimits;
import org.elasticsearch.xpack.ml.job.config.JobUpdate;
import org.elasticsearch.xpack.ml.support.AbstractStreamableTestCase;

public class UpdateJobActionRequestTests
        extends AbstractStreamableTestCase<UpdateJobAction.Request> {

    @Override
    protected UpdateJobAction.Request createTestInstance() {
        String jobId = randomAlphaOfLength(10);
        // no need to randomize JobUpdate this is already tested in: JobUpdateTests
        JobUpdate.Builder jobUpdate = new JobUpdate.Builder(jobId);
        jobUpdate.setAnalysisLimits(new AnalysisLimits(100L, 100L));
        return new UpdateJobAction.Request(jobId, jobUpdate.build());
    }

    @Override
    protected UpdateJobAction.Request createBlankInstance() {
        return new UpdateJobAction.Request();
    }

}
