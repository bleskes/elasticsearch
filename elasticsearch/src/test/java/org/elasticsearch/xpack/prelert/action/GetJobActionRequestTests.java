/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.action.GetJobAction.Request;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

public class GetJobActionRequestTests extends AbstractStreamableTestCase<GetJobAction.Request> {

    @Override
    protected Request createTestInstance() {
        Request instance = new Request(randomAsciiOfLengthBetween(1, 20));
        instance.config(randomBoolean());
        instance.dataCounts(randomBoolean());
        instance.modelSizeStats(randomBoolean());
        return instance;
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

}
