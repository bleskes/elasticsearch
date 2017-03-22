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

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Components that registers all persistent task executors
 */
public class PersistentTasksExecutorRegistry extends AbstractComponent {

    private final Map<String, PersistentTasksExecutor<?>> taskExecutors;

    @SuppressWarnings("unchecked")
    public PersistentTasksExecutorRegistry(Settings settings, Collection<PersistentTasksExecutor<?>> taskExecutors) {
        super(settings);
        Map<String, PersistentTasksExecutor<?>> map = new HashMap<>();
        for (PersistentTasksExecutor<?> executor : taskExecutors) {
            map.put(executor.getTaskName(), executor);
        }
        this.taskExecutors = Collections.unmodifiableMap(map);
    }

    @SuppressWarnings("unchecked")
    public <Request extends PersistentTaskRequest> PersistentTasksExecutor<Request> getPersistentTaskExecutorSafe(String taskName) {
        PersistentTasksExecutor<Request> executor = (PersistentTasksExecutor<Request>) taskExecutors.get(taskName);
        if (executor == null) {
            throw new IllegalStateException("Unknown persistent executor [" + taskName + "]");
        }
        return executor;
    }
}