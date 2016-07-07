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
package org.elasticsearch.license.plugin.core;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.ack.ClusterStateUpdateResponse;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xpack.graph.Graph;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.plugin.TestUtils;
import org.elasticsearch.license.plugin.action.delete.DeleteLicenseRequest;
import org.elasticsearch.xpack.monitoring.Monitoring;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.xpack.security.Security;
import org.elasticsearch.test.ESSingleNodeTestCase;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.watcher.Watcher;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.elasticsearch.license.plugin.TestUtils.generateSignedLicense;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class LicensesManagerServiceTests extends ESSingleNodeTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> getPlugins() {
        return Collections.singletonList(XPackPlugin.class);
    }

    @Override
    protected Settings nodeSettings() {
        return Settings.builder().
                put(XPackPlugin.featureEnabledSetting(Security.NAME), false)
                .put(XPackPlugin.featureEnabledSetting(Monitoring.NAME), false)
                .put(XPackPlugin.featureEnabledSetting(Watcher.NAME), false)
                .put(XPackPlugin.featureEnabledSetting(Graph.NAME), false)
                .build();
    }

    @Override
    protected boolean resetNodeAfterTest() {
        return true;
    }

    public void testStoreAndGetLicenses() throws Exception {
        LicensesService licensesService = getInstanceFromNode(LicensesService.class);
        ClusterService clusterService = getInstanceFromNode(ClusterService.class);
        License goldLicense = generateSignedLicense("gold", TimeValue.timeValueHours(1));
        TestUtils.registerAndAckSignedLicenses(licensesService, goldLicense, LicensesStatus.VALID);
        License silverLicense = generateSignedLicense("silver", TimeValue.timeValueHours(2));
        TestUtils.registerAndAckSignedLicenses(licensesService, silverLicense, LicensesStatus.VALID);
        License platinumLicense = generateSignedLicense("platinum", TimeValue.timeValueHours(1));
        TestUtils.registerAndAckSignedLicenses(licensesService, platinumLicense, LicensesStatus.VALID);
        License basicLicense = generateSignedLicense("basic", TimeValue.timeValueHours(3));
        TestUtils.registerAndAckSignedLicenses(licensesService, basicLicense, LicensesStatus.VALID);
        LicensesMetaData licensesMetaData = clusterService.state().metaData().custom(LicensesMetaData.TYPE);
        assertThat(licensesMetaData.getLicense(), equalTo(basicLicense));
        final License getLicenses = licensesService.getLicense();
        assertThat(getLicenses, equalTo(basicLicense));
    }

    public void testEffectiveLicenses() throws Exception {
        final LicensesService licensesService = getInstanceFromNode(LicensesService.class);
        final ClusterService clusterService = getInstanceFromNode(ClusterService.class);
        License goldLicense = generateSignedLicense("gold", TimeValue.timeValueSeconds(5));
        // put gold license
        TestUtils.registerAndAckSignedLicenses(licensesService, goldLicense, LicensesStatus.VALID);
        LicensesMetaData licensesMetaData = clusterService.state().metaData().custom(LicensesMetaData.TYPE);
        assertThat(licensesService.getLicense(licensesMetaData), equalTo(goldLicense));

        License platinumLicense = generateSignedLicense("platinum", TimeValue.timeValueSeconds(3));
        // put platinum license
        TestUtils.registerAndAckSignedLicenses(licensesService, platinumLicense, LicensesStatus.VALID);
        licensesMetaData = clusterService.state().metaData().custom(LicensesMetaData.TYPE);
        assertThat(licensesService.getLicense(licensesMetaData), equalTo(platinumLicense));

        License basicLicense = generateSignedLicense("basic", TimeValue.timeValueSeconds(3));
        // put basic license
        TestUtils.registerAndAckSignedLicenses(licensesService, basicLicense, LicensesStatus.VALID);
        licensesMetaData = clusterService.state().metaData().custom(LicensesMetaData.TYPE);
        assertThat(licensesService.getLicense(licensesMetaData), equalTo(basicLicense));
    }

    public void testInvalidLicenseStorage() throws Exception {
        LicensesService licensesService = getInstanceFromNode(LicensesService.class);
        ClusterService clusterService = getInstanceFromNode(ClusterService.class);
        License signedLicense = generateSignedLicense(TimeValue.timeValueMinutes(2));

        // modify content of signed license
        License tamperedLicense = License.builder()
                .fromLicenseSpec(signedLicense, signedLicense.signature())
                .expiryDate(signedLicense.expiryDate() + 10 * 24 * 60 * 60 * 1000L)
                .validate()
                .build();

        TestUtils.registerAndAckSignedLicenses(licensesService, tamperedLicense, LicensesStatus.INVALID);

        // ensure that the invalid license never made it to cluster state
        LicensesMetaData licensesMetaData = clusterService.state().metaData().custom(LicensesMetaData.TYPE);
        assertThat(licensesMetaData.getLicense(), not(equalTo(tamperedLicense)));
    }

    public void testRemoveLicenses() throws Exception {
        LicensesService licensesService = getInstanceFromNode(LicensesService.class);
        ClusterService clusterService = getInstanceFromNode(ClusterService.class);

        // generate a trial license for one feature
        licensesService.register(new TestUtils.AssertingLicensee("", logger));

        // generate signed licenses
        License license = generateSignedLicense(TimeValue.timeValueHours(1));
        TestUtils.registerAndAckSignedLicenses(licensesService, license, LicensesStatus.VALID);
        LicensesMetaData licensesMetaData = clusterService.state().metaData().custom(LicensesMetaData.TYPE);
        assertThat(licensesMetaData.getLicense(), not(LicensesMetaData.LICENSE_TOMBSTONE));

        // remove signed licenses
        removeAndAckSignedLicenses(licensesService);
        licensesMetaData = clusterService.state().metaData().custom(LicensesMetaData.TYPE);
        assertThat(licensesMetaData.getLicense(), equalTo(LicensesMetaData.LICENSE_TOMBSTONE));
    }

    public void testRemoveLicensesAndLicenseeNotification() throws Exception {
        LicensesService licensesService = getInstanceFromNode(LicensesService.class);
        licensesService.start();
        ClusterService clusterService = getInstanceFromNode(ClusterService.class);

        // generate a trial license for one feature
        TestUtils.AssertingLicensee licensee = new TestUtils.AssertingLicensee("", logger);
        licensesService.register(licensee);

        // we should get a trial license to begin with
        assertBusy(new Runnable() {
            @Override
            public void run() {
                assertThat(licensee.statuses, hasSize(1));
                assertThat(licensee.statuses.get(0).getMode(), is(License.OperationMode.TRIAL));
                assertThat(licensee.statuses.get(0).getLicenseState(), is(LicenseState.ENABLED));
            }
        });


        // generate signed licenses
        License license = generateSignedLicense("gold", TimeValue.timeValueHours(1));
        TestUtils.registerAndAckSignedLicenses(licensesService, license, LicensesStatus.VALID);
        assertBusy(new Runnable() {
            @Override
            public void run() {
                assertThat(licensee.statuses, hasSize(2));
                assertThat(licensee.statuses.get(1).getMode(), not(License.OperationMode.TRIAL));
                assertThat(licensee.statuses.get(1).getLicenseState(), is(LicenseState.ENABLED));
            }
        });

        // remove signed licenses
        removeAndAckSignedLicenses(licensesService);
        assertBusy(new Runnable() {
            @Override
            public void run() {
                assertThat(licensee.statuses, hasSize(3));
            }
        });
        LicensesMetaData licensesMetaData = clusterService.state().metaData().custom(LicensesMetaData.TYPE);
        assertThat(licensesMetaData.getLicense(), is(LicensesMetaData.LICENSE_TOMBSTONE));
        assertThat(licensee.statuses, hasSize(3));
        assertThat(licensee.statuses.get(2).getLicenseState(), is(LicenseState.DISABLED));
        assertThat(licensee.statuses.get(2).getMode(), is(License.OperationMode.MISSING));
    }

    private void removeAndAckSignedLicenses(final LicensesService licensesService) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean success = new AtomicBoolean(false);
        licensesService.removeLicense(new DeleteLicenseRequest(), new ActionListener<ClusterStateUpdateResponse>() {
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