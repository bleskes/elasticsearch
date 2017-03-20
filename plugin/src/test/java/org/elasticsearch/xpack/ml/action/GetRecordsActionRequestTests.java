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
import org.elasticsearch.xpack.ml.action.GetRecordsAction.Request;
import org.elasticsearch.xpack.ml.action.util.PageParams;
import org.elasticsearch.xpack.ml.support.AbstractStreamableXContentTestCase;

public class GetRecordsActionRequestTests extends AbstractStreamableXContentTestCase<GetRecordsAction.Request> {

    @Override
    protected Request parseInstance(XContentParser parser) {
        return GetRecordsAction.Request.parseRequest(null, parser);
    }

    @Override
    protected Request createTestInstance() {
        Request request = new Request(randomAsciiOfLengthBetween(1, 20));
        if (randomBoolean()) {
            String start = randomBoolean() ? randomAsciiOfLengthBetween(1, 20) : String.valueOf(randomNonNegativeLong());
            request.setStart(start);
        }
        if (randomBoolean()) {
            String end = randomBoolean() ? randomAsciiOfLengthBetween(1, 20) : String.valueOf(randomNonNegativeLong());
            request.setEnd(end);
        }
        if (randomBoolean()) {
            request.setPartitionValue(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setSort(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setDecending(randomBoolean());
        }
        if (randomBoolean()) {
            request.setRecordScore(randomDouble());
        }
        if (randomBoolean()) {
            request.setIncludeInterim(randomBoolean());
        }
        if (randomBoolean()) {
            int from = randomInt(PageParams.MAX_FROM_SIZE_SUM);
            int maxSize = PageParams.MAX_FROM_SIZE_SUM - from;
            int size = randomInt(maxSize);
            request.setPageParams(new PageParams(from, size));
        }
        return request;
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

}
