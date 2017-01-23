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
package org.elasticsearch.xpack.ml.action;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.action.ValidateTransformsAction.Request;
import org.elasticsearch.xpack.ml.job.config.transform.TransformConfig;
import org.elasticsearch.xpack.ml.job.config.transform.TransformType;
import org.elasticsearch.xpack.ml.support.AbstractStreamableXContentTestCase;

import java.util.ArrayList;
import java.util.List;

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
    protected Request parseInstance(XContentParser parser) {
        return Request.PARSER.apply(parser, null);
    }

}
