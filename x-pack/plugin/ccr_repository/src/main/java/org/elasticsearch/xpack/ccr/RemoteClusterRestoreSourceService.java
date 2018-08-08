/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.ccr;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.internal.io.IOUtils;
import org.elasticsearch.index.engine.Engine;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteClusterRestoreSourceService extends AbstractLifecycleComponent {

    final private Map<String, Engine.IndexCommitRef> onGoingRestores = new ConcurrentHashMap<>();

    public RemoteClusterRestoreSourceService(Settings settings) {
        super(settings);
    }

    @Override
    protected void doStart() {

    }

    @Override
    protected void doStop() {
        IOUtils.closeWhileHandlingException(onGoingRestores.values());
    }

    @Override
    protected void doClose() throws IOException {

    }


    public void addCommit(String uuid, Engine.IndexCommitRef commit) {
        onGoingRestores.put(uuid, commit);
    }

    public Engine.IndexCommitRef getCommit(String uuid) {
        Engine.IndexCommitRef commit = onGoingRestores.get(uuid);
        if (commit == null) {
            throw new ElasticsearchException("commit for [" + uuid + "] not found");
        }
        return commit;
    }

    public void closeCommit(String uuid) {
        Engine.IndexCommitRef commit = onGoingRestores.remove(uuid);
        if (commit == null) {
            throw new ElasticsearchException("commit for [" + uuid + "] not found");
        }
        IOUtils.closeWhileHandlingException(commit);
    }
}
