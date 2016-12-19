/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
public class ValidateTransformsActionRequestTests extends AbstractStreamableXContentTestCase<ValidateTransformsAction.Request> {

    @Override
    protected Request createTestInstance() {
        int size = randomInt(10);
        List<TransformConfig> transforms = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            TransformType transformType = randomFrom(TransformType.values());
            TransformConfig transform = new TransformConfig(transformType.prettyName());
            transforms.add(transform);
        }
        return new Request(transforms);
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

    @Override
    protected Request parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return Request.PARSER.apply(parser, () -> matcher);
    }

}
