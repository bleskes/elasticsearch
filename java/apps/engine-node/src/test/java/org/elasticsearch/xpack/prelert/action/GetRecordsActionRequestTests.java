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
