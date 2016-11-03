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
package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.action.PutJobAction.Response;
import org.elasticsearch.xpack.prelert.job.IgnoreDowntime;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

import static org.elasticsearch.xpack.prelert.job.JobDetailsTests.buildJobBuilder;
import static org.elasticsearch.xpack.prelert.job.JobDetailsTests.randomValidJobId;

public class PutJobActionResponseTests extends AbstractStreamableTestCase<Response> {

    @Override
    protected Response createTestInstance() {
        JobDetails.Builder builder = buildJobBuilder(randomValidJobId());
        builder.setIgnoreDowntime(IgnoreDowntime.NEVER);
        return new Response(builder.build());
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
