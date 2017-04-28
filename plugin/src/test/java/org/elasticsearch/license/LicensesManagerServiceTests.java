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

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ack.ClusterStateUpdateResponse;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.XPackSettings;
import org.elasticsearch.xpack.XPackSingleNodeTestCase;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@TestLogging("_root:DEBUG") // So we can get more logging for https://github.com/elastic/x-pack-elasticsearch/issues/222
public class LicensesManagerServiceTests extends XPackSingleNodeTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> getPlugins() {
        return Collections.singletonList(XPackPlugin.class);
    }

    @Override
    protected Settings nodeSettings() {
        return Settings.builder()
            .put(super.nodeSettings())
            .put(XPackSettings.SECURITY_ENABLED.getKey(), false)
            .put(XPackSettings.MONITORING_ENABLED.getKey(), false)
            .put(XPackSettings.WATCHER_ENABLED.getKey(), false)
            .put(XPackSettings.GRAPH_ENABLED.getKey(), false)
            .put(XPackSettings.MACHINE_LEARNING_ENABLED.getKey(), false)
            .build();
    }

    @Override
    protected boolean resetNodeAfterTest() {
        return true;
    }

    public void testStoreAndGetLicenses() throws Exception {
        LicenseService licenseService = getInstanceFromNode(LicenseService.class);
        ClusterService clusterService = getInstanceFromNode(ClusterService.class);
        License goldLicense = TestUtils.generateSignedLicense("gold", TimeValue.timeValueHours(1));
        TestUtils.registerAndAckSignedLicenses(licenseService, goldLicense, LicensesStatus.VALID);
        License silverLicense = TestUtils.generateSignedLicense("silver", TimeValue.timeValueHours(2));
        TestUtils.registerAndAckSignedLicenses(licenseService, silverLicense, LicensesStatus.VALID);
        License platinumLicense = TestUtils.generateSignedLicense("platinum", TimeValue.timeValueHours(1));
        TestUtils.registerAndAckSignedLicenses(licenseService, platinumLicense, LicensesStatus.VALID);
        License basicLicense = TestUtils.generateSignedLicense("basic", TimeValue.timeValueHours(3));
        TestUtils.registerAndAckSignedLicenses(licenseService, basicLicense, LicensesStatus.VALID);
        LicensesMetaData licensesMetaData = clusterService.state().metaData().custom(LicensesMetaData.TYPE);
        assertThat(licensesMetaData.getLicense(), equalTo(basicLicense));
        final License getLicenses = licenseService.getLicense();
        assertThat(getLicenses, equalTo(basicLicense));
    }

    public void testEffectiveLicenses() throws Exception {
        final LicenseService licenseService = getInstanceFromNode(LicenseService.class);
        final ClusterService clusterService = getInstanceFromNode(ClusterService.class);
        License goldLicense = TestUtils.generateSignedLicense("gold", TimeValue.timeValueSeconds(5));
        // put gold license
        TestUtils.registerAndAckSignedLicenses(licenseService, goldLicense, LicensesStatus.VALID);
        LicensesMetaData licensesMetaData = clusterService.state().metaData().custom(LicensesMetaData.TYPE);
        assertThat(licenseService.getLicense(licensesMetaData), equalTo(goldLicense));

        License platinumLicense = TestUtils.generateSignedLicense("platinum", TimeValue.timeValueSeconds(3));
        // put platinum license
        TestUtils.registerAndAckSignedLicenses(licenseService, platinumLicense, LicensesStatus.VALID);
        licensesMetaData = clusterService.state().metaData().custom(LicensesMetaData.TYPE);
        assertThat(licenseService.getLicense(licensesMetaData), equalTo(platinumLicense));

        License basicLicense = TestUtils.generateSignedLicense("basic", TimeValue.timeValueSeconds(3));
        // put basic license
        TestUtils.registerAndAckSignedLicenses(licenseService, basicLicense, LicensesStatus.VALID);
        licensesMetaData = clusterService.state().metaData().custom(LicensesMetaData.TYPE);
        assertThat(licenseService.getLicense(licensesMetaData), equalTo(basicLicense));
    }

    public void testInvalidLicenseStorage() throws Exception {
        LicenseService licenseService = getInstanceFromNode(LicenseService.class);
        ClusterService clusterService = getInstanceFromNode(ClusterService.class);
        License signedLicense = TestUtils.generateSignedLicense(TimeValue.timeValueMinutes(2));

        // modify content of signed license
        License tamperedLicense = License.builder()
                .fromLicenseSpec(signedLicense, signedLicense.signature())
                .expiryDate(signedLicense.expiryDate() + 10 * 24 * 60 * 60 * 1000L)
                .validate()
                .build();

        TestUtils.registerAndAckSignedLicenses(licenseService, tamperedLicense, LicensesStatus.INVALID);

        // ensure that the invalid license never made it to cluster state
        ClusterState state = clusterService.state();
        assertNotNull(state);
        assertNotNull(state.metaData());
        assertNotNull(state.metaData().custom(LicensesMetaData.TYPE));
        LicensesMetaData licensesMetaData = state.metaData().custom(LicensesMetaData.TYPE);
        assertThat(licensesMetaData.getLicense(), not(equalTo(tamperedLicense)));
    }

    public void testRemoveLicenses() throws Exception {
        LicenseService licenseService = getInstanceFromNode(LicenseService.class);
        ClusterService clusterService = getInstanceFromNode(ClusterService.class);

        // generate signed licenses
        License license = TestUtils.generateSignedLicense(TimeValue.timeValueHours(1));
        TestUtils.registerAndAckSignedLicenses(licenseService, license, LicensesStatus.VALID);
        LicensesMetaData licensesMetaData = clusterService.state().metaData().custom(LicensesMetaData.TYPE);
        assertThat(licensesMetaData.getLicense(), not(LicensesMetaData.LICENSE_TOMBSTONE));

        // remove signed licenses
        removeAndAckSignedLicenses(licenseService);
        licensesMetaData = clusterService.state().metaData().custom(LicensesMetaData.TYPE);
        assertThat(licensesMetaData.getLicense(), equalTo(LicensesMetaData.LICENSE_TOMBSTONE));
    }

    private void removeAndAckSignedLicenses(final LicenseService licenseService) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean success = new AtomicBoolean(false);
        licenseService.removeLicense(new DeleteLicenseRequest(), new ActionListener<ClusterStateUpdateResponse>() {
            @Override
            public void onResponse(ClusterStateUpdateResponse clusterStateUpdateResponse) {
                if (clusterStateUpdateResponse.isAcknowledged()) {
                    success.set(true);
                }
                latch.countDown();
            }

            @Override
            public void onFailure(Exception throwable) {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertThat("remove license(s) failed", success.get(), equalTo(true));
    }
}