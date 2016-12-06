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

import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

public class PostDataActionRequestTests extends AbstractStreamableTestCase<JobDataAction.Request> {
    @Override
    protected JobDataAction.Request createTestInstance() {
        JobDataAction.Request request = new JobDataAction.Request(randomAsciiOfLengthBetween(1, 20));
        request.setIgnoreDowntime(randomBoolean());
        if (randomBoolean()) {
            request.setResetStart(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            request.setResetEnd(randomAsciiOfLengthBetween(1, 20));
        }
        return request;
    }

    @Override
    protected JobDataAction.Request createBlankInstance() {
        return new JobDataAction.Request();
    }
}
