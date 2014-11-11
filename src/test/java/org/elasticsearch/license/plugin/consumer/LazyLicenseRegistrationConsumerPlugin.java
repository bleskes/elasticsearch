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

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

/**
 * Registers licenses only after cluster has recovered
 * see {@link org.elasticsearch.license.plugin.consumer.LazyLicenseRegistrationPluginService}
 * <p/>
 * License registration happens after clusterservice start()
 */
public class LazyLicenseRegistrationConsumerPlugin extends TestConsumerPluginBase {

    public static String NAME = "test_consumer_plugin_2";

    @Inject
    public LazyLicenseRegistrationConsumerPlugin(Settings settings) {
        super(settings);
    }

    @Override
    public Class<? extends TestPluginServiceBase> service() {
        return LazyLicenseRegistrationPluginService.class;
    }

    @Override
    protected String pluginName() {
        return NAME;
    }

    @Override
    public String featureName() {
        return LazyLicenseRegistrationPluginService.FEATURE_NAME;
    }
}
