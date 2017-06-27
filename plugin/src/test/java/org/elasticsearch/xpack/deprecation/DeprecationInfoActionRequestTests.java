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
package org.elasticsearch.xpack.deprecation;

import org.elasticsearch.test.AbstractStreamableTestCase;

public class DeprecationInfoActionRequestTests extends AbstractStreamableTestCase<DeprecationInfoAction.Request> {

    @Override
    protected DeprecationInfoAction.Request createTestInstance() {
        return new DeprecationInfoAction.Request(randomAlphaOfLength(10));
    }

    @Override
    protected DeprecationInfoAction.Request createBlankInstance() {
        return new DeprecationInfoAction.Request();
    }
}
