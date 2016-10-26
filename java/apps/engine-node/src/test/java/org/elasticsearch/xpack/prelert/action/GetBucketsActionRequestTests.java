package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.action.GetBucketsAction.Request;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableXContentTestCase;

public class GetBucketsActionRequestTests extends AbstractStreamableXContentTestCase<GetBucketsAction.Request> {

    @Override
    protected Request parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return GetBucketsAction.Request.parseRequest(null, parser, () -> matcher);
    }

    @Override
    protected Request createTestInstance() {
        Request request = new Request(randomAsciiOfLengthBetween(1, 20), randomAsciiOfLengthBetween(1, 20),
                randomAsciiOfLengthBetween(1, 20));
        if (randomBoolean()) {
            request.setAnomalyScore(randomDouble());
        }
        if (randomBoolean()) {
            request.setExpand(randomBoolean());
        }
        if (randomBoolean()) {
            request.setIncludeInterim(randomBoolean());
        }
        if (randomBoolean()) {
            request.setMaxNormalizedProbability(randomDouble());
        }
        if (randomBoolean()) {
            request.setPartitionValue(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setSkip(randomInt());
        }
        if (randomBoolean()) {
            request.setTake(randomInt());
        }
        return request;
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

}
