/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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

import org.elasticsearch.test.AbstractStreamableTestCase;
import org.elasticsearch.xpack.ml.action.PreviewDatafeedAction.Request;

public class PreviewDatafeedActionRequestTests extends AbstractStreamableTestCase<Request> {

    @Override
    protected Request createTestInstance() {
        return new Request(randomAsciiOfLength(10));
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }
}
