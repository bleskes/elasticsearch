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

import org.elasticsearch.client.Client;
import org.elasticsearch.common.component.Lifecycle;
import org.elasticsearch.common.component.LifecycleListener;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.marvel.shield.SecuredClient;

import java.util.Collection;

/**
 *
 */
public class LocalExporter implements Exporter<LocalExporter> {

    public static final String NAME = "local";

    private final Client client;

    @Inject
    public LocalExporter(SecuredClient client) {
        this.client = client;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void export(Collection<MarvelDoc> marvelDocs) {

    }

    @Override
    public Lifecycle.State lifecycleState() {
        return null;
    }

    @Override
    public void addLifecycleListener(LifecycleListener lifecycleListener) {

    }

    @Override
    public void removeLifecycleListener(LifecycleListener lifecycleListener) {

    }

    @Override
    public LocalExporter start() {
        return null;
    }

    @Override
    public LocalExporter stop() {
        return null;
    }

    @Override
    public void close() {

    }
}
