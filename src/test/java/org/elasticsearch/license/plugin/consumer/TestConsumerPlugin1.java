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

package org.elasticsearch.license.plugin.consumer;

import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

public class TestConsumerPlugin1 extends TestConsumerPluginBase {

    public final static String NAME = "test_consumer_plugin_1";

    @Inject
    public TestConsumerPlugin1(Settings settings) {
        super(settings);
    }

    @Override
    protected Class<? extends LifecycleComponent> service() {
        return TestPluginService1.class;
    }

    @Override
    protected String pluginName() {
        return NAME;
    }
}
