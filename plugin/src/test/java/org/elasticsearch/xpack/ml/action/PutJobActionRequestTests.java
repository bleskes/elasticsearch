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

import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.ml.action.PutJobAction.Request;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.support.AbstractStreamableXContentTestCase;

import java.io.IOException;
import java.util.Date;

import static org.elasticsearch.xpack.ml.job.config.JobTests.buildJobBuilder;
import static org.elasticsearch.xpack.ml.job.config.JobTests.randomValidJobId;

public class PutJobActionRequestTests extends AbstractStreamableXContentTestCase<Request> {

    private final String jobId = randomValidJobId();

    @Override
    protected Request createTestInstance() {
        Job.Builder jobConfiguration = buildJobBuilder(jobId, null);
        return new Request(jobConfiguration);
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

    @Override
    protected Request parseInstance(XContentParser parser) {
        return Request.parseRequest(jobId, parser);
    }

    public void testParseRequest_InvalidCreateSetting() throws IOException {
        Job.Builder jobConfiguration = buildJobBuilder(jobId, null);
        jobConfiguration.setLastDataTime(new Date());
        XContentBuilder xContentBuilder = toXContent(jobConfiguration, XContentType.JSON);
        XContentParser parser = XContentFactory.xContent(XContentType.JSON)
                .createParser(NamedXContentRegistry.EMPTY, xContentBuilder.bytes());

        expectThrows(IllegalArgumentException.class, () -> Request.parseRequest(jobId, parser));
    }
}
