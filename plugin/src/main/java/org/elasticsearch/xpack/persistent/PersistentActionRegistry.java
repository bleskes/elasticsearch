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

import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;

import java.util.Collections;
import java.util.Map;

/**
 * Components that registers all persistent actions
 */
public class PersistentActionRegistry extends AbstractComponent {

    private volatile Map<String, PersistentActionHolder<?>> actions = Collections.emptyMap();

    private final Object actionHandlerMutex = new Object();

    public PersistentActionRegistry(Settings settings) {
        super(settings);
    }

    public <Request extends PersistentActionRequest> void registerPersistentAction(String action,
                                                                                   TransportPersistentAction<Request> persistentAction) {
        registerPersistentAction(new PersistentActionHolder<>(action, persistentAction, persistentAction.getExecutor()));
    }

    private <Request extends PersistentActionRequest> void registerPersistentAction(
            PersistentActionHolder<Request> reg) {

        synchronized (actionHandlerMutex) {
            PersistentActionHolder<?> replaced = actions.get(reg.getAction());
            actions = MapBuilder.newMapBuilder(actions).put(reg.getAction(), reg).immutableMap();
            if (replaced != null) {
                logger.warn("registered two handlers for persistent action {}, handlers: {}, {}", reg.getAction(), reg, replaced);
            }
        }
    }

    public void removeHandler(String action) {
        synchronized (actionHandlerMutex) {
            actions = MapBuilder.newMapBuilder(actions).remove(action).immutableMap();
        }
    }

    @SuppressWarnings("unchecked")
    public <Request extends PersistentActionRequest> PersistentActionHolder<Request> getPersistentActionHolderSafe(String action) {
        PersistentActionHolder<Request> holder = (PersistentActionHolder<Request>) actions.get(action);
        if (holder == null) {
            throw new IllegalStateException("Unknown persistent action [" + action + "]");
        }
        return holder;
    }

    public <Request extends PersistentActionRequest>
    TransportPersistentAction<Request> getPersistentActionSafe(String action) {
        PersistentActionHolder<Request> holder = getPersistentActionHolderSafe(action);
        return holder.getPersistentAction();
    }

    public static final class PersistentActionHolder<Request extends PersistentActionRequest> {

        private final String action;
        private final TransportPersistentAction<Request> persistentAction;
        private final String executor;


        public PersistentActionHolder(String action, TransportPersistentAction<Request> persistentAction, String executor) {
            this.action = action;
            this.persistentAction = persistentAction;
            this.executor = executor;
        }

        public String getAction() {
            return action;
        }

        public TransportPersistentAction<Request> getPersistentAction() {
            return persistentAction;
        }

        public String getExecutor() {
            return executor;
        }
    }
}