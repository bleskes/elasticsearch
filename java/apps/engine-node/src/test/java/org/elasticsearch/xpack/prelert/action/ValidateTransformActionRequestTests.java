package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.action.ValidateTransformAction.Request;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;
import org.elasticsearch.xpack.prelert.job.transform.TransformType;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableXContentTestCase;

public class ValidateTransformActionRequestTests extends AbstractStreamableXContentTestCase<ValidateTransformAction.Request> {

    @Override
    protected Request createTestInstance() {
        TransformType transformType = randomFrom(TransformType.values());
        TransformConfig transform = new TransformConfig(transformType.prettyName());
        return new Request(transform);
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
