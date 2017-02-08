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

import org.elasticsearch.common.inject.Provider;
import org.elasticsearch.tasks.CancellableTask;
import org.elasticsearch.tasks.TaskId;

/**
 * Task that returns additional state information
 */
public class PersistentTask extends CancellableTask {
    private Provider<Status> statusProvider;

    private long persistentTaskId;

    public PersistentTask(long id, String type, String action, String description, TaskId parentTask) {
        super(id, type, action, description, parentTask);
    }

    @Override
    public boolean shouldCancelChildrenOnCancellation() {
        return true;
    }

    // In case of persistent tasks we always need to return: `false`
    // because in case of persistent task the parent task isn't a task in the task manager, but in cluster state.
    // This instructs the task manager not to try to kill this persistent task when the task manager cannot find
    // a fake parent node id "cluster" in the cluster state
    @Override
    public final boolean cancelOnParentLeaving() {
        return false;
    }

    @Override
    public Status getStatus() {
        Provider<Status> statusProvider = this.statusProvider;
        if (statusProvider != null) {
            return statusProvider.get();
        } else {
            return null;
        }
    }

    public void setStatusProvider(Provider<Status> statusProvider) {
        assert this.statusProvider == null;
        this.statusProvider = statusProvider;
    }

    public long getPersistentTaskId() {
        return persistentTaskId;
    }

    public void setPersistentTaskId(long persistentTaskId) {
        this.persistentTaskId = persistentTaskId;
    }
}