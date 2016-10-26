package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.action.PutJobAction.Request;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableXContentTestCase;

public class PutJobActionRequestTests extends AbstractStreamableXContentTestCase<Request> {

    @Override
    protected Request createTestInstance() {
        JobConfiguration jobConfiguration = new JobConfiguration(randomAsciiOfLength(10));
        return new Request(jobConfiguration);
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

    @Override
    protected Request parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return Request.parseRequest(parser, () -> matcher);
    }

}
