/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.license.plugin;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ProcessedClusterStateUpdateTask;
import org.elasticsearch.cluster.ack.ClusterStateUpdateResponse;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.base.Predicate;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.TestUtils;
import org.elasticsearch.license.core.ESLicense;
import org.elasticsearch.license.core.ESLicenses;
import org.elasticsearch.license.manager.ESLicenseManager;
import org.elasticsearch.license.plugin.action.put.PutLicenseRequest;
import org.elasticsearch.license.plugin.core.*;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.InternalTestCluster;
import org.junit.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.elasticsearch.license.plugin.core.LicensesService.LicensesUpdateResponse;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope.TEST;
import static org.hamcrest.Matchers.equalTo;

@ClusterScope(scope = TEST, numDataNodes = 10)
public class LicensesServiceTests extends AbstractLicensesIntegrationTests {


    private static String pubKeyPath = null;
    private static String priKeyPath = null;
    private static String node = null;

    @BeforeClass
    public static void setup() throws IOException, URISyntaxException {
        priKeyPath = Paths.get(LicenseTransportTests.class.getResource("/private.key").toURI()).toAbsolutePath().toString();
        pubKeyPath = Paths.get(LicenseTransportTests.class.getResource("/public.key").toURI()).toAbsolutePath().toString();
    }

    @Before
    public void beforeTest() throws Exception {
        wipeAllLicenses();
        clear();

        DiscoveryNodes discoveryNodes = LicensesServiceTests.masterClusterService().state().getNodes();
        Set<String> dataNodeSet = new HashSet<>();
        for(DiscoveryNode discoveryNode : discoveryNodes) {
            if (discoveryNode.dataNode()) {
                dataNodeSet.add(discoveryNode.getName());
            }
        }
        String[] dataNodes = dataNodeSet.toArray(new String[dataNodeSet.size()]);
        node = dataNodes[randomIntBetween(0, dataNodes.length - 1)];
    }

    @Test
    public void testEmptySignedLicenseCheck() {
        LicensesManagerService licensesManagerService = masterLicensesManagerService();
        assertTrue(LicensesStatus.VALID == licensesManagerService.checkLicenses(new HashSet<ESLicense>()));
    }

    @Test
    public void testInvalidSignedLicenseCheck() throws Exception {
        LicensesManagerService licensesManagerService = masterLicensesManagerService();

        Map<String, TestUtils.FeatureAttributes> map = new HashMap<>();
        TestUtils.FeatureAttributes featureAttributes =
                new TestUtils.FeatureAttributes("shield", "subscription", "platinum", "foo bar Inc.", "elasticsearch", 2, "2014-12-13", "2015-12-13");
        map.put(TestUtils.SHIELD, featureAttributes);
        String licenseString = TestUtils.generateESLicenses(map);
        String licenseOutput = TestUtils.runLicenseGenerationTool(licenseString, pubKeyPath, priKeyPath);
        Set<ESLicense> licenses = new HashSet<>(ESLicenses.fromSource(licenseOutput));

        assertTrue(LicensesStatus.VALID == licensesManagerService.checkLicenses(licenses));

        ESLicense esLicense = ESLicenses.reduceAndMap(licenses).get(TestUtils.SHIELD);

        final ESLicense tamperedLicense = ESLicense.builder()
                .fromLicenseSpec(esLicense, esLicense.signature())
                .expiryDate(esLicense.expiryDate() + 10 * 24 * 60 * 60 * 1000l)
                .verify()
                .build();

        assertTrue(LicensesStatus.INVALID == licensesManagerService.checkLicenses(Collections.singleton(tamperedLicense)));
    }

    @Test
    public void testStoringLicenses() throws Exception {
        Map<String, TestUtils.FeatureAttributes> map = new HashMap<>();
        TestUtils.FeatureAttributes featureAttributes1 =
                new TestUtils.FeatureAttributes("shield", "subscription", "platinum", "foo bar Inc.", "elasticsearch", 2, "2014-12-13", "2015-12-13");
        map.put(TestUtils.SHIELD, featureAttributes1);
        String licenseString = TestUtils.generateESLicenses(map);
        String licenseOutput = TestUtils.runLicenseGenerationTool(licenseString, pubKeyPath, priKeyPath);
        List<ESLicense> licenses = ESLicenses.fromSource(licenseOutput);

        LicensesManagerService licensesManagerService = masterLicensesManagerService();
        ESLicenseManager esLicenseManager = masterLicenseManager();
        final CountDownLatch latch1 = new CountDownLatch(1);
        // todo: fix with awaitBusy
        licensesManagerService.registerLicenses(new LicensesService.PutLicenseRequestHolder(new PutLicenseRequest().licenses(licenses), "test"), new ActionListener<LicensesUpdateResponse>() {
            @Override
            public void onResponse(LicensesUpdateResponse clusterStateUpdateResponse) {
                if (clusterStateUpdateResponse.isAcknowledged()) {
                    latch1.countDown();
                }
            }

            @Override
            public void onFailure(Throwable e) {

            }
        });

        latch1.await();
        LicensesMetaData metaData = clusterService().state().metaData().custom(LicensesMetaData.TYPE);
        Set<ESLicense> metaDataLicense = esLicenseManager.fromSignatures(metaData.getSignatures());
        TestUtils.isSame(new HashSet<>(licenses), metaDataLicense);


        TestUtils.FeatureAttributes featureAttributes2 =
                new TestUtils.FeatureAttributes("shield", "subscription", "platinum", "foo bar Inc.", "elasticsearch", 2, "2014-12-13", "2016-12-13");
        map.put(TestUtils.SHIELD, featureAttributes2);
        licenseString = TestUtils.generateESLicenses(map);
        licenseOutput = TestUtils.runLicenseGenerationTool(licenseString, pubKeyPath, priKeyPath);
        List<ESLicense> licenses2 = ESLicenses.fromSource(licenseOutput);
        final CountDownLatch latch2 = new CountDownLatch(1);
        // todo: fix with awaitBusy
        licensesManagerService.registerLicenses(new LicensesService.PutLicenseRequestHolder(new PutLicenseRequest().licenses(licenses2), "test"), new ActionListener<LicensesUpdateResponse>() {
            @Override
            public void onResponse(LicensesUpdateResponse clusterStateUpdateResponse) {
                if (clusterStateUpdateResponse.isAcknowledged()) {
                    latch2.countDown();
                }
            }

            @Override
            public void onFailure(Throwable e) {

            }
        });

        latch2.await();
        metaData = clusterService().state().metaData().custom(LicensesMetaData.TYPE);
        metaDataLicense = esLicenseManager.fromSignatures(metaData.getSignatures());
        TestUtils.isSame(new HashSet<>(licenses2), metaDataLicense);
    }

    @Test
    public void testTrialLicenseGeneration() throws Exception {
        LicensesClientService clientService = licensesClientService();
        final CountDownLatch latch = new CountDownLatch(1);
        // todo: fix with awaitBusy
        clientService.register("shield", new LicensesService.TrialLicenseOptions(TimeValue.timeValueHours(10), 100), new LicensesClientService.Listener() {
            @Override
            public void onEnabled() {
                logger.info("got onEnabled from LicensesClientService");
                latch.countDown();
            }

            @Override
            public void onDisabled() {
                fail();
            }
        });
        logger.info("waiting for onEnabled");
        latch.await();
    }

    @Test
    public void testMultipleClientRegistration() {
    }

    private class TestLicenseClientListener implements LicensesClientService.Listener {

        AtomicBoolean shouldBeEnabled = new AtomicBoolean(false);
        AtomicBoolean processed = new AtomicBoolean(false);

        private TestLicenseClientListener(boolean shouldBeEnabled) {
            this.shouldBeEnabled.getAndSet(shouldBeEnabled);
        }

        private void reset() {
            processed.set(false);
        }

        @Override
        public void onEnabled() {
            if (this.shouldBeEnabled.get()) {
                logger.info("onEnabled called from LicensesClientService");
                processed.set(true);
            } else {
                fail("onEnabled should not have been called");
            }

        }

        @Override
        public void onDisabled() {
            if (!this.shouldBeEnabled.get()) {
                logger.info("onEnabled called from LicensesClientService");
                processed.set(true);
            } else {
                fail("onDisabled should not have been called");
            }
        }
    }

    @Test
    public void testClientValidation() throws Exception {
        // start with no trial license
        // feature should be onDisabled
        // then add signed license
        // feature should be onEnabled

        LicensesClientService clientService = licensesClientService();
        final LicensesManagerService managerService = licensesManagerService();
        LicensesManagerService masterLicensesManagerService = masterLicensesManagerService();
        final TestLicenseClientListener testLicenseClientListener = new TestLicenseClientListener(false);
        clientService.register("shield", null, testLicenseClientListener);

        for (String enabledFeature : managerService.enabledFeatures()) {
            assertFalse(enabledFeature.equals("shield"));
        }
        logger.info("pass initial check");

        assertFalse(testLicenseClientListener.processed.get());

        testLicenseClientListener.shouldBeEnabled.set(true);
        Map<String, TestUtils.FeatureAttributes> map = new HashMap<>();
        TestUtils.FeatureAttributes featureAttributes1 =
                new TestUtils.FeatureAttributes("shield", "subscription", "platinum", "foo bar Inc.", "elasticsearch", 2, "2014-12-13", "2015-12-13");
        map.put(TestUtils.SHIELD, featureAttributes1);
        String licenseString = TestUtils.generateESLicenses(map);
        String licenseOutput = TestUtils.runLicenseGenerationTool(licenseString, pubKeyPath, priKeyPath);
        List<ESLicense> licenses = ESLicenses.fromSource(licenseOutput);

        final CountDownLatch latch1 = new CountDownLatch(1);
        // todo: fix with awaitBusy
        masterLicensesManagerService.registerLicenses(new LicensesService.PutLicenseRequestHolder(new PutLicenseRequest().licenses(licenses), "test"), new ActionListener<LicensesUpdateResponse>() {
            @Override
            public void onResponse(LicensesUpdateResponse clusterStateUpdateResponse) {
                if (clusterStateUpdateResponse.isAcknowledged()) {
                    latch1.countDown();
                }
            }

            @Override
            public void onFailure(Throwable e) {

            }
        });

        latch1.await();

        logger.info("waiting for onEnabled");
        assertThat(awaitBusy(new Predicate<Object>() {
            @Override
            public boolean apply(Object o) {
                return managerService.enabledFeatures().contains("shield");
            }
        }, 1, TimeUnit.MINUTES), equalTo(true));

    }

    @Test
    public void testFeatureWithoutLicense() throws Exception {
        LicensesClientService clientService = licensesClientService();
        // todo: fix with awaitBusy
        clientService.register("marvel", null, new LicensesClientService.Listener() {
            @Override
            public void onEnabled() {
                fail();
            }

            @Override
            public void onDisabled() {
            }
        });

        LicensesManagerService managerService = licensesManagerService();
        assertFalse("feature should not be enabled: no licenses registered", managerService.enabledFeatures().contains("marvel"));
    }

    @Test
    public void testLicenseExpiry() throws Exception {
        //TODO, first figure out how to generate a license with a quick expiry in matter of seconds
    }


    private LicensesManagerService masterLicensesManagerService() {
        final InternalTestCluster clients = internalCluster();
        return clients.getInstance(LicensesManagerService.class, clients.getMasterName());
    }

    private ESLicenseManager masterLicenseManager() {
        final InternalTestCluster clients = internalCluster();
        return clients.getInstance(ESLicenseManager.class, clients.getMasterName());
    }

    private LicensesManagerService licensesManagerService() {
        return internalCluster().getInstance(LicensesManagerService.class, node);
    }

    private LicensesClientService licensesClientService() {
        return internalCluster().getInstance(LicensesClientService.class, node);
    }

    private static ClusterService masterClusterService() {
        final InternalTestCluster clients = internalCluster();
        return clients.getInstance(ClusterService.class, clients.getMasterName());
    }

    private void clear() {
        final InternalTestCluster clients = internalCluster();
        LicensesService masterService = clients.getInstance(LicensesService.class, clients.getMasterName());
        masterService.clear();
        if (node != null) {
            LicensesService nodeService = clients.getInstance(LicensesService.class, node);
            nodeService.clear();
        }
    }


}
