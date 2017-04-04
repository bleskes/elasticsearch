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

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.config.DataDescription.DataFormat;
import org.elasticsearch.xpack.ml.support.AbstractStreamableTestCase;

public class PostDataActionRequestTests extends AbstractStreamableTestCase<PostDataAction.Request> {
    @Override
    protected PostDataAction.Request createTestInstance() {
        PostDataAction.Request request = new PostDataAction.Request(randomAlphaOfLengthBetween(1, 20));
        if (randomBoolean()) {
            request.setResetStart(randomAlphaOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setResetEnd(randomAlphaOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setDataDescription(new DataDescription(randomFrom(DataFormat.values()),
                    randomAlphaOfLengthBetween(1, 20), randomAlphaOfLengthBetween(1, 20),
                    randomAlphaOfLength(1).charAt(0), randomAlphaOfLength(1).charAt(0)));
        }
        if (randomBoolean()) {
            request.setContent(new BytesArray(new byte[0]), randomFrom(XContentType.values()));
        }
        return request;
    }

    @Override
    protected PostDataAction.Request createBlankInstance() {
        return new PostDataAction.Request();
    }
}
