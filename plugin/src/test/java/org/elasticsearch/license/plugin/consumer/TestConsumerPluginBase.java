/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated
 *  All Rights Reserved.
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
package org.elasticsearch.license.plugin.consumer;

import com.google.common.collect.Lists;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;

public abstract class TestConsumerPluginBase extends AbstractPlugin {

    private final boolean isEnabled;

    public TestConsumerPluginBase(Settings settings) {
        if (DiscoveryNode.clientNode(settings)) {
            // Enable plugin only on node clients
            this.isEnabled = "node".equals(settings.get(Client.CLIENT_TYPE_SETTING));
        } else {
            this.isEnabled = true;
        }
    }

    @Override
    public String name() {
        return pluginName();
    }

    @Override
    public String description() {
        return "test licensing consumer plugin";
    }


    @Override
    public Collection<Class<? extends LifecycleComponent>> services() {
        Collection<Class<? extends LifecycleComponent>> services = Lists.newArrayList();
        if (isEnabled) {
            services.add(service());
        }
        return services;
    }

    public abstract Class<? extends TestPluginServiceBase> service();

    protected abstract String pluginName();

    public abstract String featureName();
}
