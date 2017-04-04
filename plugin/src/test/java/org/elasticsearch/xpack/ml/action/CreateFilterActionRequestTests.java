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
import org.elasticsearch.xpack.ml.action.PutFilterAction.Request;
import org.elasticsearch.xpack.ml.job.config.MlFilter;
import org.elasticsearch.xpack.ml.support.AbstractStreamableXContentTestCase;

import java.util.ArrayList;
import java.util.List;

public class CreateFilterActionRequestTests extends AbstractStreamableXContentTestCase<PutFilterAction.Request> {

    @Override
    protected Request createTestInstance() {
        int size = randomInt(10);
        List<String> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(randomAlphaOfLengthBetween(1, 20));
        }
        MlFilter filter = new MlFilter(randomAlphaOfLengthBetween(1, 20), items);
        return new PutFilterAction.Request(filter);
    }

    @Override
    protected Request createBlankInstance() {
        return new PutFilterAction.Request();
    }

    @Override
    protected Request parseInstance(XContentParser parser) {
        return PutFilterAction.Request.parseRequest(parser);
    }

}
