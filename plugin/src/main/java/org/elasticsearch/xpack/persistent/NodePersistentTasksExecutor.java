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

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportResponse.Empty;

/**
 * This component is responsible for execution of persistent tasks.
 *
 * It abstracts away the execution of tasks and greatly simplifies testing of PersistentTasksNodeService
 */
public class NodePersistentTasksExecutor {
    private final ThreadPool threadPool;

    public NodePersistentTasksExecutor(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    public <Request extends PersistentTaskRequest> void executeTask(Request request,
                                                                    AllocatedPersistentTask task,
                                                                    PersistentTasksExecutor<Request> action,
                                                                    ActionListener<Empty> listener) {
        threadPool.executor(action.getExecutor()).execute(new AbstractRunnable() {
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void doRun() throws Exception {
                try {
                    action.nodeOperation(task, request, listener);
                } catch (Exception ex) {
                    listener.onFailure(ex);
                }

            }
        });

    }

}
