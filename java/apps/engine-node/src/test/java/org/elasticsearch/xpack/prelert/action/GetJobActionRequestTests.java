package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.action.GetJobAction.Request;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

public class GetJobActionRequestTests extends AbstractStreamableTestCase<GetJobAction.Request> {


    @Override
    protected Request createTestInstance() {
        return new Request(randomAsciiOfLengthBetween(1, 20));
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

}
