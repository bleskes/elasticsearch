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

package org.elasticsearch.xpack.monitoring;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.xpack.monitoring.exporter.ExportException;
import org.elasticsearch.xpack.monitoring.exporter.Exporters;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;
import org.junit.After;
import org.junit.Before;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MonitoringServiceTests extends ESTestCase {

    TestThreadPool threadPool;
    MonitoringService monitoringService;
    XPackLicenseState licenseState = mock(XPackLicenseState.class);
    ClusterService clusterService;
    ClusterSettings clusterSettings;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        threadPool = new TestThreadPool(getTestName());
        clusterService = mock(ClusterService.class);
        clusterSettings = new ClusterSettings(Settings.EMPTY, new HashSet<>(MonitoringSettings.getSettings()));
        when(clusterService.getClusterSettings()).thenReturn(clusterSettings);
    }

    @After
    public void terminate() throws Exception {
        if (monitoringService != null) {
            monitoringService.close();
        }
        terminate(threadPool);
    }

    public void testIsMonitoringActive() throws Exception {
        monitoringService = new MonitoringService(Settings.EMPTY, clusterSettings, threadPool, emptySet(), new CountingExporter());

        monitoringService.start();
        assertBusy(() -> assertTrue(monitoringService.isStarted()));
        assertTrue(monitoringService.isMonitoringActive());

        monitoringService.stop();
        assertBusy(() -> assertFalse(monitoringService.isStarted()));
        assertFalse(monitoringService.isMonitoringActive());

        monitoringService.start();
        assertBusy(() -> assertTrue(monitoringService.isStarted()));
        assertTrue(monitoringService.isMonitoringActive());

        monitoringService.close();
        assertBusy(() -> assertFalse(monitoringService.isStarted()));
        assertFalse(monitoringService.isMonitoringActive());
    }

    public void testInterval() throws Exception {
        Settings settings = Settings.builder().put(MonitoringSettings.INTERVAL.getKey(), TimeValue.MINUS_ONE).build();

        CountingExporter exporter = new CountingExporter();
        monitoringService = new MonitoringService(settings, clusterSettings, threadPool, emptySet(), exporter);

        monitoringService.start();
        assertBusy(() -> assertTrue(monitoringService.isStarted()));
        assertFalse("interval -1 does not start the monitoring execution", monitoringService.isMonitoringActive());
        assertEquals(0, exporter.getExportsCount());

        monitoringService.setInterval(TimeValue.timeValueSeconds(1));
        assertTrue(monitoringService.isMonitoringActive());
        assertBusy(() -> assertThat(exporter.getExportsCount(), greaterThan(0)));

        monitoringService.setInterval(TimeValue.timeValueMillis(100));
        assertFalse(monitoringService.isMonitoringActive());

        monitoringService.setInterval(TimeValue.MINUS_ONE);
        assertFalse(monitoringService.isMonitoringActive());
    }

    public void testSkipExecution() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final BlockingExporter exporter = new BlockingExporter(latch);

        Settings settings = Settings.builder().put(MonitoringSettings.INTERVAL.getKey(), MonitoringSettings.MIN_INTERVAL).build();
        monitoringService = new MonitoringService(settings, clusterSettings, threadPool, emptySet(), exporter);

        monitoringService.start();
        assertBusy(() -> assertTrue(monitoringService.isStarted()));

        assertBusy(() -> assertThat(exporter.getExportsCount(), equalTo(1)));

        monitoringService.cancelExecution();

        latch.countDown();

        assertThat(exporter.getExportsCount(), equalTo(1));
    }

    class CountingExporter extends Exporters {

        private final AtomicInteger exports = new AtomicInteger(0);

        CountingExporter() {
            super(Settings.EMPTY, Collections.emptyMap(), clusterService, licenseState, threadPool.getThreadContext());
        }

        @Override
        public void export(Collection<MonitoringDoc> docs, ActionListener<Void> listener) {
            exports.incrementAndGet();
            listener.onResponse(null);
        }

        int getExportsCount() {
            return exports.get();
        }

        @Override
        protected void doStart() {
        }

        @Override
        protected void doStop() {
        }

        @Override
        protected void doClose() {
        }
    }

    class BlockingExporter extends CountingExporter {

        private final CountDownLatch latch;

        BlockingExporter(CountDownLatch latch) {
            super();
            this.latch = latch;
        }

        @Override
        public void export(Collection<MonitoringDoc> docs, ActionListener<Void> listener) {
            super.export(docs, ActionListener.wrap(r -> {
                try {
                    latch.await();
                    listener.onResponse(null);
                } catch (InterruptedException e) {
                    listener.onFailure(new ExportException("BlockingExporter failed", e));
                }
            }, listener::onFailure));

        }

        @Override
        protected void doStart() {
        }

        @Override
        protected void doStop() {
        }

        @Override
        protected void doClose() {
        }
    }
}
