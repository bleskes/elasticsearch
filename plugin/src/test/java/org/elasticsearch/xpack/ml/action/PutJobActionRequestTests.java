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
package org.elasticsearch.xpack.ml.action;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.action.PutJobAction.Request;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.support.AbstractStreamableXContentTestCase;

import java.util.Date;

import static org.elasticsearch.xpack.ml.job.config.JobTests.buildJobBuilder;
import static org.elasticsearch.xpack.ml.job.config.JobTests.randomValidJobId;

public class PutJobActionRequestTests extends AbstractStreamableXContentTestCase<Request> {

    private final String jobId = randomValidJobId();
    private final Date date = new Date();

    @Override
    protected Request createTestInstance() {
        Job.Builder jobConfiguration = buildJobBuilder(jobId, date);
        return new Request(jobConfiguration.build());
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

    @Override
    protected Request parseInstance(XContentParser parser) {
        return Request.parseRequest(jobId, parser);
    }

}
