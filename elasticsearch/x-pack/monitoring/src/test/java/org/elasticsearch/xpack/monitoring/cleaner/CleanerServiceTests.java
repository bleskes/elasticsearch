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

package org.elasticsearch.xpack.monitoring.cleaner;

import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.plugin.core.XPackLicenseState;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.monitoring.MonitoringSettings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CleanerServiceTests extends ESTestCase {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final XPackLicenseState licenseState = mock(XPackLicenseState.class);
    private ClusterSettings clusterSettings;
    private ThreadPool threadPool;

    @Before
    public void start() {
        clusterSettings = new ClusterSettings(Settings.EMPTY, Collections.singleton(MonitoringSettings.HISTORY_DURATION));
        threadPool = new TestThreadPool("CleanerServiceTests");
    }

    @After
    public void stop() throws InterruptedException {
        terminate(threadPool);
    }

    public void testConstructorWithInvalidRetention() {
        // invalid setting
        expectedException.expect(IllegalArgumentException.class);

        TimeValue expected = TimeValue.timeValueHours(1);
        Settings settings = Settings.builder().put(MonitoringSettings.HISTORY_DURATION.getKey(), expected.getStringRep()).build();

        new CleanerService(settings, clusterSettings, threadPool, licenseState);
    }

    public void testGetRetentionWithSettingWithUpdatesAllowed() {
        TimeValue expected = TimeValue.timeValueHours(25);
        Settings settings = Settings.builder().put(MonitoringSettings.HISTORY_DURATION.getKey(), expected.getStringRep()).build();

        when(licenseState.isUpdateRetentionAllowed()).thenReturn(true);

        assertEquals(expected, new CleanerService(settings, clusterSettings, threadPool, licenseState).getRetention());

        verify(licenseState).isUpdateRetentionAllowed();
    }

    public void testGetRetentionDefaultValueWithNoSettings() {
        when(licenseState.isUpdateRetentionAllowed()).thenReturn(true);

        assertEquals(MonitoringSettings.HISTORY_DURATION.get(Settings.EMPTY),
                     new CleanerService(Settings.EMPTY, clusterSettings, threadPool, licenseState).getRetention());

        verify(licenseState).isUpdateRetentionAllowed();
    }

    public void testGetRetentionDefaultValueWithSettingsButUpdatesNotAllowed() {
        TimeValue notExpected = TimeValue.timeValueHours(25);
        Settings settings = Settings.builder().put(MonitoringSettings.HISTORY_DURATION.getKey(), notExpected.getStringRep()).build();

        when(licenseState.isUpdateRetentionAllowed()).thenReturn(false);

        assertEquals(MonitoringSettings.HISTORY_DURATION.get(Settings.EMPTY),
                     new CleanerService(settings, clusterSettings, threadPool, licenseState).getRetention());

        verify(licenseState).isUpdateRetentionAllowed();
    }

    public void testSetGlobalRetention() {
        // Note: I used this value to ensure we're not double-validating the setter; the cluster state should be the
        // only thing calling this method and it will use the settings object to validate the time value
        TimeValue expected = TimeValue.timeValueHours(2);

        when(licenseState.isUpdateRetentionAllowed()).thenReturn(true);

        CleanerService service = new CleanerService(Settings.EMPTY, clusterSettings, threadPool, licenseState);

        service.setGlobalRetention(expected);

        assertEquals(expected, service.getRetention());

        verify(licenseState, times(2)).isUpdateRetentionAllowed(); // once by set, once by get
    }

    public void testSetGlobalRetentionAppliesEvenIfLicenseDisallows() {
        // Note: I used this value to ensure we're not double-validating the setter; the cluster state should be the
        // only thing calling this method and it will use the settings object to validate the time value
        TimeValue expected = TimeValue.timeValueHours(2);

        // required to be true on the second call for it to see it take effect
        when(licenseState.isUpdateRetentionAllowed()).thenReturn(false).thenReturn(true);

        CleanerService service = new CleanerService(Settings.EMPTY, clusterSettings, threadPool, licenseState);

        // uses allow=false
        service.setGlobalRetention(expected);

        // uses allow=true
        assertEquals(expected, service.getRetention());

        verify(licenseState, times(2)).isUpdateRetentionAllowed();
    }

    public void testNextExecutionDelay() {
        CleanerService.ExecutionScheduler scheduler = new CleanerService.DefaultExecutionScheduler();

        DateTime now = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
        assertThat(scheduler.nextExecutionDelay(now).millis(), equalTo(TimeValue.timeValueHours(1).millis()));

        now = new DateTime(2015, 1, 1, 1, 0, DateTimeZone.UTC);
        assertThat(scheduler.nextExecutionDelay(now).millis(), equalTo(TimeValue.timeValueHours(24).millis()));

        now = new DateTime(2015, 1, 1, 0, 59, DateTimeZone.UTC);
        assertThat(scheduler.nextExecutionDelay(now).millis(), equalTo(TimeValue.timeValueMinutes(1).millis()));

        now = new DateTime(2015, 1, 1, 23, 59, DateTimeZone.UTC);
        assertThat(scheduler.nextExecutionDelay(now).millis(), equalTo(TimeValue.timeValueMinutes(60 + 1).millis()));

        now = new DateTime(2015, 1, 1, 12, 34, 56);
        assertThat(scheduler.nextExecutionDelay(now).millis(), equalTo(new DateTime(2015, 1, 2, 1, 0, 0).getMillis() - now.getMillis()));

    }

    public void testExecution() throws InterruptedException {
        final int nbExecutions = randomIntBetween(1, 3);
        CountDownLatch latch = new CountDownLatch(nbExecutions);

        logger.debug("--> creates a cleaner service that cleans every second");
        XPackLicenseState licenseState = mock(XPackLicenseState.class);
        when(licenseState.isMonitoringAllowed()).thenReturn(true);
        CleanerService service = new CleanerService(Settings.EMPTY, clusterSettings, licenseState, threadPool,
                new TestExecutionScheduler(1_000));

        logger.debug("--> registers cleaning listener");
        TestListener listener = new TestListener(latch);
        service.add(listener);

        try {
            logger.debug("--> starts cleaning service");
            service.start();

            logger.debug("--> waits for listener to be executed");
            if (!latch.await(10, TimeUnit.SECONDS)) {
                fail("waiting too long for test to complete. Expected listener was not executed");
            }
        } finally {
            service.stop();
        }
        assertThat(latch.getCount(), equalTo(0L));
    }

    class TestListener implements CleanerService.Listener {

        final CountDownLatch latch;

        TestListener(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onCleanUpIndices(TimeValue retention) {
            latch.countDown();
        }
    }

    class TestExecutionScheduler implements CleanerService.ExecutionScheduler {

        final long offset;

        TestExecutionScheduler(long offset) {
            this.offset = offset;
        }

        @Override
        public TimeValue nextExecutionDelay(DateTime now) {
            return TimeValue.timeValueMillis(offset);
        }
    }
}
