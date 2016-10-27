package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.action.GetInfluencersAction.Request;
import org.elasticsearch.xpack.prelert.job.results.PageParams;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableXContentTestCase;

public class GetInfluencersActionRequestTests extends AbstractStreamableXContentTestCase<GetInfluencersAction.Request> {

    @Override
    protected Request parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return GetInfluencersAction.Request.parseRequest(null, null, null, parser, () -> matcher);
    }

    @Override
    protected Request createTestInstance() {
        Request request = new Request(randomAsciiOfLengthBetween(1, 20), randomAsciiOfLengthBetween(1, 20),
                randomAsciiOfLengthBetween(1, 20));
        if (randomBoolean()) {
            request.setAnomalyScore(randomDouble());
        }
        if (randomBoolean()) {
            request.setIncludeInterim(randomBoolean());
        }
        if (randomBoolean()) {
            request.setSort(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setDecending(randomBoolean());
        }
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
