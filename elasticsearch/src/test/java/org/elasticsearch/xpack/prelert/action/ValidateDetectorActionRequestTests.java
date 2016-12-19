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
        Detector.Builder detector;
        if (randomBoolean()) {
            detector = new Detector.Builder(randomFrom(Detector.COUNT_WITHOUT_FIELD_FUNCTIONS), null);
        } else {
            detector = new Detector.Builder(randomFrom(Detector.FIELD_NAME_FUNCTIONS), randomAsciiOfLengthBetween(1, 20));
        }
        return new Request(detector.build());
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
