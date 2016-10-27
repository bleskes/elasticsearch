package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.action.PostDataFlushAction.Request;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

public class PostDataFlushRequestTests extends AbstractStreamableTestCase<Request> {

    @Override
    protected Request createTestInstance() {
        Request request = new Request(randomAsciiOfLengthBetween(1, 20));
        request.setCalcInterim(randomBoolean());
        if (randomBoolean()) {
            request.setStart(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setEnd(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setAdvanceTime(randomAsciiOfLengthBetween(1, 20));
        }
        return request;
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }
}