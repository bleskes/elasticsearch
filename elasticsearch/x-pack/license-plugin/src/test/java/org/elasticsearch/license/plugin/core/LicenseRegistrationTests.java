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

import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateUpdateTask;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.plugin.TestUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LicenseRegistrationTests extends AbstractLicenseServiceTestCase {

    public void testTrialLicenseRequestOnEmptyLicenseState() throws Exception {
        TestUtils.AssertingLicensee licensee = new TestUtils.AssertingLicensee(
            "testTrialLicenseRequestOnEmptyLicenseState", logger);
        setInitialState(null, licensee);
        when(discoveryNodes.isLocalNodeElectedMaster()).thenReturn(true);
        licenseService.start();

        ClusterState state = ClusterState.builder(new ClusterName("a")).build();
        ArgumentCaptor<ClusterStateUpdateTask> stateUpdater = ArgumentCaptor.forClass(ClusterStateUpdateTask.class);
        verify(clusterService, Mockito.times(1)).submitStateUpdateTask(any(), stateUpdater.capture());
        ClusterState stateWithLicense = stateUpdater.getValue().execute(state);
        LicensesMetaData licenseMetaData = stateWithLicense.metaData().custom(LicensesMetaData.TYPE);
        assertNotNull(licenseMetaData);
        assertNotNull(licenseMetaData.getLicense());
        assertEquals(clock.millis() + LicenseService.TRIAL_LICENSE_DURATION.millis(), licenseMetaData.getLicense().expiryDate());
    }

    public void testNotificationOnRegistration() throws Exception {
        TestUtils.AssertingLicensee licensee = new TestUtils.AssertingLicensee(
                "testNotificationOnRegistration", logger);
        setInitialState(TestUtils.generateSignedLicense(TimeValue.timeValueHours(2)), licensee);
        licenseService.start();
        assertThat(licensee.statuses.size(), equalTo(1));
        assertTrue(licensee.statuses.get(0).isActive());
    }
}