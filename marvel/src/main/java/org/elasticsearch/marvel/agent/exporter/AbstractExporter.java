/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.marvel.agent.exporter;


import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

import java.util.Collection;

public abstract class AbstractExporter<T> extends AbstractLifecycleComponent<T> implements Exporter<T> {

    private final String name;

    protected final ClusterService clusterService;

    @Inject
    public AbstractExporter(Settings settings, String name, ClusterService clusterService) {
        super(settings);
        this.name = name;
        this.clusterService = clusterService;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public T start() {
        logger.debug("starting exporter [{}]", name());
        return super.start();
    }

    @Override
    protected void doStart() {
    }

    protected boolean masterOnly() {
        return false;
    }

    @Override
    public void export(Collection<MarvelDoc> marvelDocs) {
        if (masterOnly() && !clusterService.state().nodes().localNodeMaster()) {
            logger.trace("exporter [{}] runs on master only", name());
            return;
        }

        if (marvelDocs == null) {
            logger.debug("no objects to export for [{}]", name());
            return;
        }

        try {
            doExport(marvelDocs);
        } catch (Exception e) {
            logger.error("export [{}] throws exception when exporting data", e, name());
        }
    }

    protected abstract void doExport(Collection<MarvelDoc> marvelDocs) throws Exception;

    @Override
    public T stop() {
        logger.debug("stopping exporter [{}]", name());
        return super.stop();
    }

    @Override
    protected void doStop() {
    }

    @Override
    public void close() {
        logger.trace("closing exporter [{}]", name());
        super.close();
    }

    @Override
    protected void doClose() {
    }
}