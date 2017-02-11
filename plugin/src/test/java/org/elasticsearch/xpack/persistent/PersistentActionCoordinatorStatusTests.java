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

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.xpack.ml.support.AbstractWireSerializingTestCase;
import org.elasticsearch.xpack.persistent.PersistentActionCoordinator.State;
import org.elasticsearch.xpack.persistent.PersistentActionCoordinator.Status;

import static org.hamcrest.Matchers.containsString;

public class PersistentActionCoordinatorStatusTests extends AbstractWireSerializingTestCase<Status> {

    @Override
    protected Status createTestInstance() {
        return new Status(randomFrom(State.values()));
    }

    @Override
    protected Writeable.Reader<Status> instanceReader() {
        return Status::new;
    }

    public void testToString() {
        assertThat(createTestInstance().toString(), containsString("state"));
    }
}