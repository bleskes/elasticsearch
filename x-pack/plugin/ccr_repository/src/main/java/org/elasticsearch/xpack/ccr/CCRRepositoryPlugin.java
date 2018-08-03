/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.ccr;

import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.RepositoryPlugin;
import org.elasticsearch.repositories.Repository.Factory;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CCRRepositoryPlugin extends Plugin implements RepositoryPlugin {

    private final Settings settings;
    private Client client;

    public CCRRepositoryPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override
    public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool, ResourceWatcherService resourceWatcherService, ScriptService scriptService, NamedXContentRegistry xContentRegistry, Environment environment, NodeEnvironment nodeEnvironment, NamedWriteableRegistry namedWriteableRegistry) {
        this.client = client;
        return Collections.emptyList();
    }

    @Override
    public Map<String, Factory> getRepositories(Environment env, NamedXContentRegistry namedXContentRegistry) {
        return Collections.singletonMap("ccr", this::createRepository);
    }

    private org.elasticsearch.repositories.Repository createRepository(RepositoryMetaData metadata) throws Exception {
        return new CCRRepository(metadata, client);
    }
}
