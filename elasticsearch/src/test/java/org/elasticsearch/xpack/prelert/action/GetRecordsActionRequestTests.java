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
import org.elasticsearch.xpack.prelert.action.GetRecordsAction.Request;
import org.elasticsearch.xpack.prelert.job.results.PageParams;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableXContentTestCase;

public class GetRecordsActionRequestTests extends AbstractStreamableXContentTestCase<GetRecordsAction.Request> {

    @Override
    protected Request parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return GetRecordsAction.Request.parseRequest(null, parser, () -> matcher);
    }

    @Override
    protected Request createTestInstance() {
        Request request = new Request(randomAsciiOfLengthBetween(1, 20), randomAsciiOfLengthBetween(1, 20),
                randomAsciiOfLengthBetween(1, 20));
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
            request.setAnomalyScore(randomDouble());
        }
        if (randomBoolean()) {
            request.setIncludeInterim(randomBoolean());
        }
        if (randomBoolean()) {
            request.setMaxNormalizedProbability(randomDouble());
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
