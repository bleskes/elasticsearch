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
import org.elasticsearch.xpack.ml.action.GetBucketsAction.Request;
import org.elasticsearch.xpack.ml.action.util.PageParams;
import org.elasticsearch.xpack.ml.support.AbstractStreamableXContentTestCase;

public class GetBucketActionRequestTests extends AbstractStreamableXContentTestCase<GetBucketsAction.Request> {

    @Override
    protected Request createTestInstance() {
        GetBucketsAction.Request request = new GetBucketsAction.Request(randomAsciiOfLengthBetween(1, 20));

        if (randomBoolean()) {
            request.setTimestamp(String.valueOf(randomLong()));
        } else {
            if (randomBoolean()) {
                request.setPartitionValue(randomAsciiOfLengthBetween(1, 20));
            }
            if (randomBoolean()) {
                request.setStart(String.valueOf(randomLong()));
            }
            if (randomBoolean()) {
                request.setEnd(String.valueOf(randomLong()));
            }
            if (randomBoolean()) {
                request.setIncludeInterim(randomBoolean());
            }
            if (randomBoolean()) {
                request.setAnomalyScore(randomDouble());
            }
            if (randomBoolean()) {
                request.setPartitionValue(randomAsciiOfLengthBetween(1, 20));
            }
            if (randomBoolean()) {
                int from = randomInt(PageParams.MAX_FROM_SIZE_SUM);
                int maxSize = PageParams.MAX_FROM_SIZE_SUM - from;
                int size = randomInt(maxSize);
                request.setPageParams(new PageParams(from, size));
            }
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
        return new GetBucketsAction.Request();
    }

    @Override
    protected Request parseInstance(XContentParser parser) {
        return GetBucketsAction.Request.parseRequest(null, parser);
    }

}
