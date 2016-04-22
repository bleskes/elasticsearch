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

package org.elasticsearch.graph;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.xpack.XPackPlugin;

/**
 *
 */
public class GraphModule extends AbstractModule {

    private final boolean enabled;
    private final boolean transportClientNode;

    public GraphModule(boolean enabled, boolean transportClientNode) {
        this.enabled = enabled;
        this.transportClientNode = transportClientNode;
    }

    @Override
    protected void configure() {
        XPackPlugin.bindFeatureSet(binder(), GraphFeatureSet.class);
        if (enabled && transportClientNode == false) {
            bind(GraphLicensee.class).asEagerSingleton();
        } else {
            bind(GraphLicensee.class).toProvider(Providers.of(null));
        }
    }

}
