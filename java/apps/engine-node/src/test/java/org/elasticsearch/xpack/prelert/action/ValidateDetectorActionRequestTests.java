package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.action.ValidateDetectorAction.Request;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableXContentTestCase;

public class ValidateDetectorActionRequestTests extends AbstractStreamableXContentTestCase<ValidateDetectorAction.Request> {

    @Override
    protected Request createTestInstance() {
        Detector detector;
        if (randomBoolean()) {
            detector = new Detector(randomFrom(Detector.COUNT_WITHOUT_FIELD_FUNCTIONS));
        } else {
            detector = new Detector(randomFrom(Detector.FIELD_NAME_FUNCTIONS), randomAsciiOfLengthBetween(1, 20));
        }
        return new Request(detector);
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
