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
import org.elasticsearch.xpack.prelert.action.GetJobsAction.Request;
import org.elasticsearch.xpack.prelert.job.results.PageParams;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableXContentTestCase;

public class GetJobsActionRequestTests extends AbstractStreamableXContentTestCase<GetJobsAction.Request> {

    @Override
    protected Request parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return GetJobsAction.Request.PARSER.apply(parser, () -> matcher);
    }

    @Override
    protected Request createTestInstance() {
        Request request = new Request();
        if (randomBoolean()) {
            int skip = randomInt(PageParams.MAX_SKIP_TAKE_SUM);
            int maxTake = PageParams.MAX_SKIP_TAKE_SUM - skip;
            int take = randomInt(maxTake);
            request.setPageParams(new PageParams(skip, take));
        }
        return request;
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

}
