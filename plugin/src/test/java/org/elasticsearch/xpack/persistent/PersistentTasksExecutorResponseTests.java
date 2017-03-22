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

import org.elasticsearch.test.AbstractStreamableTestCase;

public class PersistentTasksExecutorResponseTests extends AbstractStreamableTestCase<PersistentTaskResponse> {

    @Override
    protected PersistentTaskResponse createTestInstance() {
        return new PersistentTaskResponse(randomLong());
    }

    @Override
    protected PersistentTaskResponse createBlankInstance() {
        return new PersistentTaskResponse();
    }
}