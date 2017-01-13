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
import org.elasticsearch.xpack.ml.action.GetModelSnapshotsAction.Request;
import org.elasticsearch.xpack.ml.job.results.PageParams;
import org.elasticsearch.xpack.ml.support.AbstractStreamableXContentTestCase;

public class GetModelSnapshotsActionRequestTests extends AbstractStreamableXContentTestCase<GetModelSnapshotsAction.Request> {

    @Override
    protected Request parseInstance(XContentParser parser) {
        return GetModelSnapshotsAction.Request.parseRequest(null, parser);
    }

    @Override
    protected Request createTestInstance() {
        Request request = new Request(randomAsciiOfLengthBetween(1, 20));
        if (randomBoolean()) {
            request.setDescriptionString(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setStart(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setEnd(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setSort(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setDescOrder(randomBoolean());
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
