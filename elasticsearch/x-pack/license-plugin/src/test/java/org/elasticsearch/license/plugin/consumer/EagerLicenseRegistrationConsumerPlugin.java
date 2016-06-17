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
 * Registers licenses upon the start of the service lifecycle
 * see {@link EagerLicenseRegistrationPluginService}
 * <p>
 * License registration might happen before clusterService start()
 */
public class EagerLicenseRegistrationConsumerPlugin extends TestConsumerPluginBase {

    public final static String NAME = "test_consumer_plugin_1";

    @Inject
    public EagerLicenseRegistrationConsumerPlugin(Settings settings) {
        super(settings);
    }

    @Override
    public Class<? extends TestPluginServiceBase> service() {
        return EagerLicenseRegistrationPluginService.class;
    }

    @Override
    public String id() {
        return EagerLicenseRegistrationPluginService.ID;
    }
}
