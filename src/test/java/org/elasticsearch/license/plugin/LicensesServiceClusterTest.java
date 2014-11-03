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

import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.gateway.GatewayService;
import org.elasticsearch.license.TestUtils;
import org.elasticsearch.license.core.ESLicense;
import org.elasticsearch.license.plugin.action.get.GetLicenseRequestBuilder;
import org.elasticsearch.license.plugin.action.get.GetLicenseResponse;
import org.elasticsearch.license.plugin.action.put.PutLicenseRequestBuilder;
import org.elasticsearch.license.plugin.action.put.PutLicenseResponse;
import org.elasticsearch.license.plugin.consumer.TestConsumerPlugin1;
import org.elasticsearch.license.plugin.consumer.TestPluginService1;
import org.elasticsearch.license.plugin.core.LicensesStatus;
import org.elasticsearch.node.internal.InternalNode;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope.TEST;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

@ClusterScope(scope = TEST, numDataNodes = 0, numClientNodes = 0, maxNumDataNodes = 0, transportClientRatio = 0)
public class LicensesServiceClusterTest extends AbstractLicensesIntegrationTests {

    private final String FEATURE_NAME = TestPluginService1.FEATURE_NAME;
    
    protected Settings transportClientSettings() {
        return super.transportClientSettings();
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return nodeSettingsBuilder(nodeOrdinal).build();
    }

    private ImmutableSettings.Builder nodeSettingsBuilder(int nodeOrdinal) {
        return settingsBuilder()
                .put(super.nodeSettings(nodeOrdinal))
                .put("gateway.type", "local")
                .put("plugins.load_classpath_plugins", false)
                .put("node.data", true)
                .put("format", "json")
                .put(TestConsumerPlugin1.NAME + ".trial_license_duration_in_seconds", 5)
                .putArray("plugin.types", LicensePlugin.class.getName(), TestConsumerPlugin1.class.getName())
                .put(InternalNode.HTTP_ENABLED, true);
    }

    @Before
    public void beforeTest() throws Exception {
        wipeAllLicenses();
    }

    @Test
    public void testClusterRestart() throws Exception {

        int numNodes = randomIntBetween(1, 5);
        logger.info("--> starting " + numNodes + " node(s)");
        for (int i = 0; i < numNodes; i++) {
            internalCluster().startNode();
        }
        ensureGreen();

        logger.info("--> put signed license");
        final List<ESLicense> esLicenses = generateAndPutLicense();
        getAndCheckLicense(esLicenses);
        logger.info("--> restart all nodes");
        internalCluster().fullRestart();
        ensureYellow();

        logger.info("--> get and check signed license");
        getAndCheckLicense(esLicenses);
    }

    @Test
    public void testClusterNotRecovered() throws Exception {

        logger.info("--> start first node (should not recover)");
        String name = internalCluster().startNode(nodeSettingsBuilder(0).put("gateway.recover_after_master_nodes", 2).put("node.master", true));
        /* TODO: figure out why STATE_NOT_RECOVERED_BLOCK is not on
        assertThat(internalCluster().client(name).admin().cluster().prepareState().setLocal(true).execute().actionGet()
                        .getState().blocks().global(ClusterBlockLevel.METADATA),
                hasItem(GatewayService.STATE_NOT_RECOVERED_BLOCK));
        */
        assertLicenseManagerEnabledFeatureFor(FEATURE_NAME);
        assertConsumerPlugin1EnableNotification(1);

        logger.info("--> start second node (should recover)");
        internalCluster().startNode(nodeSettingsBuilder(1).put("gateway.recover_after_master_nodes", 2).put("node.master", true));
        assertLicenseManagerEnabledFeatureFor(FEATURE_NAME);
        assertConsumerPlugin1EnableNotification(1);

        //internalCluster().startNode(nodeSettingsBuilder(2).put("gateway.expected_master_nodes", 3));
        //assertLicenseManagerEnabledFeatureFor(TestPluginService.FEATURE_NAME);
        //assertConsumerPlugin1EnableNotification(1);

        logger.info("--> kill master node");
        internalCluster().stopCurrentMasterNode();
        assertLicenseManagerEnabledFeatureFor(FEATURE_NAME);
        assertConsumerPlugin1EnableNotification(1);

        Thread.sleep(5 * 1050l);
        internalCluster().startNode(nodeSettingsBuilder(3).put("gateway.recover_after_master_nodes", 2).put("node.master", true));
        assertLicenseManagerDisabledFeatureFor(FEATURE_NAME);
        assertConsumerPlugin1DisableNotification(1);

    }

    private List<ESLicense> generateAndPutLicense() throws Exception {
        ClusterAdminClient cluster = internalCluster().client().admin().cluster();
        ESLicense license = generateSignedLicense(FEATURE_NAME, TimeValue.timeValueMinutes(1));
        PutLicenseRequestBuilder putLicenseRequestBuilder = new PutLicenseRequestBuilder(cluster);
        final List<ESLicense> putLicenses = Arrays.asList(license);
        putLicenseRequestBuilder.setLicense(putLicenses);
        ensureGreen();

        final PutLicenseResponse putLicenseResponse = putLicenseRequestBuilder.execute().get();

        assertThat(putLicenseResponse.isAcknowledged(), equalTo(true));
        assertThat(putLicenseResponse.status(), equalTo(LicensesStatus.VALID));

        return putLicenses;
    }

    private void getAndCheckLicense(List<ESLicense> license) {
        ClusterAdminClient cluster = internalCluster().client().admin().cluster();
        final GetLicenseResponse response = new GetLicenseRequestBuilder(cluster).get();
        assertThat(response.licenses().size(), equalTo(1));
        TestUtils.isSame(license, response.licenses());
    }
}
