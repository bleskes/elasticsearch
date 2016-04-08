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

import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.consumer.EagerLicenseRegistrationConsumerPlugin;
import org.elasticsearch.license.plugin.consumer.EagerLicenseRegistrationPluginService;
import org.elasticsearch.license.plugin.core.LicenseState;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.xpack.XPackPlugin;

import java.util.Arrays;
import java.util.Collection;

import static org.elasticsearch.test.ESIntegTestCase.Scope.TEST;

/**
 */
@ESIntegTestCase.ClusterScope(scope = TEST, numDataNodes = 10, numClientNodes = 0)
public class LicensesServiceNodeTests extends AbstractLicensesIntegrationTestCase {
    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(NetworkModule.HTTP_ENABLED.getKey(), true)
                .build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(XPackPlugin.class, EagerLicenseRegistrationConsumerPlugin.class);
    }

    @Override
    protected Collection<Class<? extends Plugin>> transportClientPlugins() {
        return nodePlugins();
    }

    public void testPluginStatus() throws Exception {
        final Iterable<EagerLicenseRegistrationPluginService> testPluginServices =
                internalCluster().getDataNodeInstances(EagerLicenseRegistrationPluginService.class);
        assertTrue(awaitBusy(() -> {
            for (EagerLicenseRegistrationPluginService pluginService : testPluginServices) {
                if (pluginService.state() != LicenseState.ENABLED) {
                    return false;
                }
            }
            return true;
        }));

    }

}
