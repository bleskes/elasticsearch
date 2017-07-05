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

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.action.OpenJobAction.Request;
import org.elasticsearch.xpack.ml.support.AbstractStreamableXContentTestCase;

public class OpenJobActionRequestTests extends AbstractStreamableXContentTestCase<Request> {

    @Override
    protected Request createTestInstance() {
        OpenJobAction.JobParams params = new OpenJobAction.JobParams(randomAlphaOfLengthBetween(1, 20));
        if (randomBoolean()) {
            params.setTimeout(TimeValue.timeValueMillis(randomNonNegativeLong()));
        }
        return new Request(params);
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

    @Override
    protected Request parseInstance(XContentParser parser) {
        return Request.parseRequest(null, parser);
    }
}