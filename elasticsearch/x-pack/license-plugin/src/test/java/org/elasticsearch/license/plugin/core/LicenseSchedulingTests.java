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

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.plugin.TestUtils;
import org.elasticsearch.xpack.scheduler.SchedulerEngine;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LicenseSchedulingTests extends AbstractLicenseServiceTestCase {

    public void testSchedulingOnRegistration() throws Exception {
        final License license = TestUtils.generateSignedLicense(TimeValue.timeValueHours(48));
        setInitialState(license);
        licensesService.start();
        int nLicensee = randomIntBetween(1, 3);
        TestUtils.AssertingLicensee[] assertingLicensees = new TestUtils.AssertingLicensee[nLicensee];
        for (int i = 0; i < assertingLicensees.length; i++) {
            assertingLicensees[i] = new TestUtils.AssertingLicensee("testLicenseNotification" + i, logger);
            licensesService.register(assertingLicensees[i]);
        }
        verify(schedulerEngine, times(0)).add(any(SchedulerEngine.Job.class));
        licensesService.stop();
    }

    public void testSchedulingSameLicense() throws Exception {
        final License license = TestUtils.generateSignedLicense(TimeValue.timeValueHours(48));
        setInitialState(license);
        licensesService.start();
        final TestUtils.AssertingLicensee licensee = new TestUtils.AssertingLicensee("testLicenseNotification", logger);
        licensesService.register(licensee);
        licensesService.onUpdate(new LicensesMetaData(license));
        verify(schedulerEngine, times(4)).add(any(SchedulerEngine.Job.class));
        licensesService.stop();
    }

    public void testSchedulingNewLicense() throws Exception {
        final License license = TestUtils.generateSignedLicense(TimeValue.timeValueHours(2));
        setInitialState(null);
        licensesService.start();
        licensesService.onUpdate(new LicensesMetaData(license));
        License newLicense = TestUtils.generateSignedLicense(TimeValue.timeValueHours(2));
        licensesService.onUpdate(new LicensesMetaData(newLicense));
        verify(schedulerEngine, times(8)).add(any(SchedulerEngine.Job.class));
        licensesService.stop();
    }

}