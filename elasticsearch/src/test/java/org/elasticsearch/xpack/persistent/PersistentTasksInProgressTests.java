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
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.xpack.ml.support.AbstractWireSerializingTestCase;
import org.elasticsearch.xpack.persistent.TestPersistentActionPlugin.TestPersistentAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PersistentTasksInProgressTests extends AbstractWireSerializingTestCase<PersistentTasksInProgress> {

    @Override
    protected PersistentTasksInProgress createTestInstance() {
        int numberOfTasks = randomInt(10);
        List<PersistentTasksInProgress.PersistentTaskInProgress<?>> entries = new ArrayList<>();
        for (int i = 0; i < numberOfTasks; i++) {
            entries.add(new PersistentTasksInProgress.PersistentTaskInProgress<>(
                    randomLong(), randomAsciiOfLength(10), new TestPersistentActionPlugin.TestRequest(randomAsciiOfLength(10)),
                    randomAsciiOfLength(10)));
        }
        return new PersistentTasksInProgress(randomLong(), entries);
    }

    @Override
    protected Writeable.Reader<PersistentTasksInProgress> instanceReader() {
        return PersistentTasksInProgress::new;
    }

    @Override
    protected NamedWriteableRegistry getNamedWriteableRegistry() {
        return new NamedWriteableRegistry(Collections.singletonList(
                new Entry(PersistentActionRequest.class, TestPersistentAction.NAME, TestPersistentActionPlugin.TestRequest::new)
        ));
    }
}