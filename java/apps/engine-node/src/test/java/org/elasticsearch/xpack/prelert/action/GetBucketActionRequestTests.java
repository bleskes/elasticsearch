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

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.action.GetBucketAction.Request;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableXContentTestCase;

public class GetBucketActionRequestTests extends AbstractStreamableXContentTestCase<GetBucketAction.Request> {

    @Override
    protected Request createTestInstance() {
        GetBucketAction.Request request = new GetBucketAction.Request(randomAsciiOfLengthBetween(1, 20), String.valueOf(randomLong()));
        if (randomBoolean()) {
            request.setPartitionValue(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setExpand(randomBoolean());
        }
        if (randomBoolean()) {
            request.setIncludeInterim(randomBoolean());
        }
        return request;
    }

    @Override
    protected Request createBlankInstance() {
        return new GetBucketAction.Request();
    }

    @Override
    protected Request parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return GetBucketAction.Request.parseRequest(null, null, parser, () -> matcher);
    }

}
