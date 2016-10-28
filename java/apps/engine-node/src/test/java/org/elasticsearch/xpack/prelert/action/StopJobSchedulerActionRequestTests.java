package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.action.StopJobSchedulerAction.Request;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

public class StopJobSchedulerActionRequestTests extends AbstractStreamableTestCase<StopJobSchedulerAction.Request> {

    @Override
    protected Request createTestInstance() {
        return new Request(randomAsciiOfLengthBetween(1, 20));
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

}
