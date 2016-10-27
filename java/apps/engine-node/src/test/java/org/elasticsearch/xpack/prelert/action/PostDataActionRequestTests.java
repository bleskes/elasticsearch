package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

public class PostDataActionRequestTests extends AbstractStreamableTestCase<PostDataAction.Request> {
    @Override
    protected PostDataAction.Request createTestInstance() {
        PostDataAction.Request request = new PostDataAction.Request(randomAsciiOfLengthBetween(1, 20));
        request.setIgnoreDowntime(randomBoolean());
        if (randomBoolean()) {
            request.setResetStart(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setResetEnd(randomAsciiOfLengthBetween(1, 20));
        }
        return request;
    }

    @Override
    protected PostDataAction.Request createBlankInstance() {
        return new PostDataAction.Request();
    }
}
