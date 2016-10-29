package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.action.PutModelSnapshotDescriptionAction.Request;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableXContentTestCase;

public class PutModelSnapshotDescriptionActionRequestTests
        extends AbstractStreamableXContentTestCase<PutModelSnapshotDescriptionAction.Request> {

    @Override
    protected Request parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return PutModelSnapshotDescriptionAction.Request.parseRequest(null, null, parser, () -> matcher);
    }

    @Override
    protected Request createTestInstance() {
        return new Request(randomAsciiOfLengthBetween(1, 20), randomAsciiOfLengthBetween(1, 20), randomAsciiOfLengthBetween(1, 20));
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

}
