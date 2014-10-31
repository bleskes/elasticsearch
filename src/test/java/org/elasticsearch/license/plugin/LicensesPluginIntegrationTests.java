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

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.core.ESLicense;
import org.elasticsearch.license.plugin.consumer.TestConsumerPlugin1;
import org.elasticsearch.license.plugin.consumer.TestPluginService1;
import org.elasticsearch.license.plugin.action.put.PutLicenseRequestBuilder;
import org.elasticsearch.license.plugin.action.put.PutLicenseResponse;
import org.elasticsearch.license.plugin.core.LicensesStatus;
import org.junit.After;
import org.junit.Test;

import static org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope.TEST;
import static org.hamcrest.CoreMatchers.equalTo;

@ClusterScope(scope = TEST, numDataNodes = 10, numClientNodes = 0)
public class LicensesPluginIntegrationTests extends AbstractLicensesIntegrationTests {

    private final int trialLicenseDurationInSeconds = 2;
    
    private final String FEATURE_NAME = TestPluginService1.FEATURE_NAME;
    
    protected Settings nodeSettings(int nodeOrdinal) {
        return ImmutableSettings.settingsBuilder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(TestConsumerPlugin1.NAME + ".trial_license_duration_in_seconds", trialLicenseDurationInSeconds)
                .put("plugin.types", LicensePlugin.class.getName() + "," + TestConsumerPlugin1.class.getName())
                .build();
    }

    @After
    public void beforeTest() throws Exception {
        wipeAllLicenses();
    }

    @Test
    public void testTrialLicenseAndSignedLicenseNotification() throws Exception {
        logger.info(" --> trial license generated");
        // managerService should report feature to be enabled on all data nodes
        assertLicenseManagerEnabledFeatureFor(FEATURE_NAME);
        // consumer plugin service should return enabled on all data nodes
        assertConsumerPlugin1EnableNotification(1);

        logger.info(" --> check trial license expiry notification");
        // consumer plugin should notify onDisabled on all data nodes (expired trial license)
        assertConsumerPlugin1DisableNotification(trialLicenseDurationInSeconds * 2);
        assertLicenseManagerDisabledFeatureFor(FEATURE_NAME);

        logger.info(" --> put signed license");
        ESLicense license = generateSignedLicense(FEATURE_NAME, TimeValue.timeValueSeconds(trialLicenseDurationInSeconds));
        final PutLicenseResponse putLicenseResponse = new PutLicenseRequestBuilder(client().admin().cluster()).setLicense(Lists.newArrayList(license)).get();
        assertThat(putLicenseResponse.isAcknowledged(), equalTo(true));
        assertThat(putLicenseResponse.status(), equalTo(LicensesStatus.VALID));

        logger.info(" --> check signed license enabled notification");
        // consumer plugin should notify onEnabled on all data nodes (signed license)
        assertConsumerPlugin1EnableNotification(1);
        assertLicenseManagerEnabledFeatureFor(FEATURE_NAME);

        logger.info(" --> check signed license expiry notification");
        // consumer plugin should notify onDisabled on all data nodes (expired signed license)
        assertConsumerPlugin1DisableNotification(trialLicenseDurationInSeconds * 2);
        assertLicenseManagerDisabledFeatureFor(FEATURE_NAME);
    }

    @Test
    public void testTrialLicenseNotification() throws Exception {
        logger.info(" --> check onEnabled for trial license");
        // managerService should report feature to be enabled on all data nodes
        assertLicenseManagerEnabledFeatureFor(FEATURE_NAME);
        // consumer plugin service should return enabled on all data nodes
        assertConsumerPlugin1EnableNotification(1);

        logger.info(" --> sleep for rest of trailLicense duration");
        Thread.sleep(trialLicenseDurationInSeconds * 1000l);

        logger.info(" --> check trial license expiry notification");
        // consumer plugin should notify onDisabled on all data nodes (expired signed license)
        assertConsumerPlugin1DisableNotification(trialLicenseDurationInSeconds);
        assertLicenseManagerDisabledFeatureFor(FEATURE_NAME);
    }

    @Test
    public void testOverlappingTrialAndSignedLicenseNotification() throws Exception {
        logger.info(" --> check onEnabled for trial license");
        // managerService should report feature to be enabled on all data nodes
        assertLicenseManagerEnabledFeatureFor(FEATURE_NAME);
        // consumer plugin service should return enabled on all data nodes
        assertConsumerPlugin1EnableNotification(1);

        logger.info(" --> put signed license while trial license is in effect");
        ESLicense license = generateSignedLicense(FEATURE_NAME, TimeValue.timeValueSeconds(trialLicenseDurationInSeconds * 2));
        final PutLicenseResponse putLicenseResponse = new PutLicenseRequestBuilder(client().admin().cluster()).setLicense(Lists.newArrayList(license)).get();
        assertThat(putLicenseResponse.isAcknowledged(), equalTo(true));
        assertThat(putLicenseResponse.status(), equalTo(LicensesStatus.VALID));

        logger.info(" --> check signed license enabled notification");
        // consumer plugin should notify onEnabled on all data nodes (signed license)
        assertConsumerPlugin1EnableNotification(1);
        assertLicenseManagerEnabledFeatureFor(FEATURE_NAME);

        logger.info(" --> sleep for rest of trailLicense duration");
        Thread.sleep(trialLicenseDurationInSeconds * 1000l);

        logger.info(" --> check consumer is still enabled [signed license]");
        // consumer plugin should notify onEnabled on all data nodes (signed license)
        assertConsumerPlugin1EnableNotification(1);
        assertLicenseManagerEnabledFeatureFor(FEATURE_NAME);

        logger.info(" --> check signed license expiry notification");
        // consumer plugin should notify onDisabled on all data nodes (expired signed license)
        assertConsumerPlugin1DisableNotification(trialLicenseDurationInSeconds * 2 * 2);
        assertLicenseManagerDisabledFeatureFor(FEATURE_NAME);
    }
}
