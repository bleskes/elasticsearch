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

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.xpack.persistent.PersistentTasksCustomMetaData.PersistentTask;

import java.io.IOException;
import java.util.Objects;

/**
 * Response upon a successful start or an persistent task
 */
public class PersistentTaskResponse extends ActionResponse {
    private PersistentTask<?> task;

    public PersistentTaskResponse() {
        super();
    }

    public PersistentTaskResponse(PersistentTask<?> task) {
        this.task = task;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        task = in.readOptionalWriteable(PersistentTask::new);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeOptionalWriteable(task);
    }

    public PersistentTask<?> getTask() {
        return task;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersistentTaskResponse that = (PersistentTaskResponse) o;
        return Objects.equals(task, that.task);
    }

    @Override
    public int hashCode() {
        return Objects.hash(task);
    }
}
