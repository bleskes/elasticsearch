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

import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Singleton;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.core.LicensesClientService;

@Singleton
public class LazyLicenseRegistrationPluginService extends TestPluginServiceBase {


    public static String FEATURE_NAME = "feature2";

    @Inject
    public LazyLicenseRegistrationPluginService(Settings settings, LicensesClientService licensesClientService, ClusterService clusterService) {
        super(false, settings, licensesClientService, clusterService);
    }

    @Override
    public String featureName() {
        return FEATURE_NAME;
    }

    @Override
    public String settingPrefix() {
        return LazyLicenseRegistrationConsumerPlugin.NAME;
    }
}
