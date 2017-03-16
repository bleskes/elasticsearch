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

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.action.ValidateJobConfigAction.Request;
import org.elasticsearch.xpack.ml.job.config.JobTests;
import org.elasticsearch.xpack.ml.support.AbstractStreamableXContentTestCase;

public class ValidateJobConfigActionRequestTests extends AbstractStreamableXContentTestCase<ValidateJobConfigAction.Request> {

    @Override
    protected Request createTestInstance() {
        return new Request(JobTests.createRandomizedJob());
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

    @Override
    protected Request parseInstance(XContentParser parser) {
        return Request.parseRequest(parser);
    }
}
