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

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.common.io.stream.NamedWriteable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.tasks.TaskId;

/**
 * Base class for a request for a persistent task
 */
public abstract class PersistentTaskRequest extends ActionRequest implements NamedWriteable, ToXContent {
    @Override
    public Task createTask(long id, String type, String action, TaskId parentTaskId) {
        return new NodePersistentTask(id, type, action, getDescription(), parentTaskId);
    }
}