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
import org.elasticsearch.xpack.ml.action.UpdateModelSnapshotAction.Request;
import org.elasticsearch.xpack.ml.support.AbstractStreamableXContentTestCase;

public class UpdateModelSnapshotActionRequestTests
        extends AbstractStreamableXContentTestCase<UpdateModelSnapshotAction.Request> {

    @Override
    protected Request parseInstance(XContentParser parser) {
        return UpdateModelSnapshotAction.Request.parseRequest(null, null, parser);
    }

    @Override
    protected Request createTestInstance() {
        Request request = new Request(randomAlphaOfLengthBetween(1, 20),
                randomAlphaOfLengthBetween(1, 20));
        if (randomBoolean()) {
            request.setDescription(randomAlphaOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setRetain(randomBoolean());
        }
        return request;
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }
}
