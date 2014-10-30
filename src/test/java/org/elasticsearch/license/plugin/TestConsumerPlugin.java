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

package org.elasticsearch.license.plugin;

import org.elasticsearch.common.collect.ImmutableSet;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;

public class TestConsumerPlugin extends AbstractPlugin {

    private final Settings settings;

    @Inject
    public TestConsumerPlugin(Settings settings) {
    this.settings = settings;
    }

    @Override
    public String name() {
        return "test_plugin";
    }

    @Override
    public String description() {
        return "test licensing consumer plugin";
    }


    @Override
    public Collection<Class<? extends LifecycleComponent>> services() {
        return ImmutableSet.<Class<? extends LifecycleComponent>>of(TestPluginService.class);
    }
}
