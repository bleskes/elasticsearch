/*
 * ELASTICSEARCH CONFIDENTIAL
 *  __________________
 *
 * [2014] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.license;

import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.transport.Netty4Plugin;
import org.elasticsearch.xpack.XPackPlugin;

import java.util.Arrays;
import java.util.Collection;

import static org.elasticsearch.test.ESIntegTestCase.Scope.TEST;

@ESIntegTestCase.ClusterScope(scope = TEST, numDataNodes = 0, numClientNodes = 0, maxNumDataNodes = 0, transportClientRatio = 0,
        autoMinMasterNodes = false)
public class LicenseServiceClusterNotRecoveredTests extends AbstractLicensesIntegrationTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return nodeSettingsBuilder(nodeOrdinal).build();
    }

    private Settings.Builder nodeSettingsBuilder(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put("node.data", true)
                .put("resource.reload.interval.high", "500ms") // for license mode file watcher
                .put(NetworkModule.HTTP_ENABLED.getKey(), true);
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(XPackPlugin.class, Netty4Plugin.class);
    }

    @Override
    protected Collection<Class<? extends Plugin>> transportClientPlugins() {
        return nodePlugins();
    }

    public void testClusterNotRecovered() throws Exception {
        logger.info("--> start one master out of two [recovery state]");
        internalCluster().startNode(nodeSettingsBuilder(0).put("discovery.zen.minimum_master_nodes", 2).put("node.master", true));
        logger.info("--> start second master out of two [recovered state]");
        internalCluster().startNode(nodeSettingsBuilder(1).put("discovery.zen.minimum_master_nodes", 2).put("node.master", true));
        assertLicenseActive(true);
    }
}
