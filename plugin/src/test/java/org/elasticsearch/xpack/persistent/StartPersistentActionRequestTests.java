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
package org.elasticsearch.xpack.persistent;

import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry.Entry;
import org.elasticsearch.xpack.persistent.CreatePersistentTaskAction.Request;
import org.elasticsearch.xpack.persistent.TestPersistentActionPlugin.TestPersistentAction;
import org.elasticsearch.xpack.persistent.TestPersistentActionPlugin.TestRequest;
import org.elasticsearch.test.AbstractStreamableTestCase;

import java.util.Collections;

public class StartPersistentActionRequestTests extends AbstractStreamableTestCase<Request> {

    @Override
    protected Request createTestInstance() {
        TestRequest testRequest = new TestRequest();
        if (randomBoolean()) {
            testRequest.setTestParam(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            testRequest.setParentTask(randomAsciiOfLengthBetween(1, 20), randomLong());
        }
        if (randomBoolean()) {
            testRequest.setExecutorNodeAttr(randomAsciiOfLengthBetween(1, 20));
        }
        return new Request(randomAsciiOfLengthBetween(1, 20), new TestRequest());
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

    @Override
    protected NamedWriteableRegistry getNamedWriteableRegistry() {
        return new NamedWriteableRegistry(Collections.singletonList(
                new Entry(PersistentActionRequest.class, TestPersistentAction.NAME, TestRequest::new)
        ));
    }
}