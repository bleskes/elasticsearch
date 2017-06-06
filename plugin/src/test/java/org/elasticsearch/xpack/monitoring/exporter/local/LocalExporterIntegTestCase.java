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

package org.elasticsearch.xpack.monitoring.exporter.local;

import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.XPackSettings;
import org.elasticsearch.xpack.monitoring.MonitoringSettings;
import org.elasticsearch.xpack.monitoring.cleaner.CleanerService;
import org.elasticsearch.xpack.monitoring.exporter.Exporter;
import org.elasticsearch.xpack.monitoring.test.MonitoringIntegTestCase;
import org.elasticsearch.xpack.security.InternalClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * {@code LocalExporterIntegTestCase} offers a basis for integration tests for the {@link LocalExporter}.
 */
public abstract class LocalExporterIntegTestCase extends MonitoringIntegTestCase {

    protected final String exporterName = "_local";

    private static ThreadPool THREADPOOL;
    private static Boolean ENABLE_WATCHER;

    @BeforeClass
    public static void setupThreadPool() {
        THREADPOOL = new TestThreadPool(LocalExporterIntegTestCase.class.getName());
    }

    @AfterClass
    public static void cleanUpStatic() throws Exception {
        ENABLE_WATCHER = null;

        if (THREADPOOL != null) {
            terminate(THREADPOOL);
        }
    }

    @Override
    protected boolean enableWatcher() {
        if (ENABLE_WATCHER == null) {
            ENABLE_WATCHER = randomBoolean();
        }

        return ENABLE_WATCHER;
    }

    protected Settings localExporterSettings() {
        return Settings.builder()
                       .put(MonitoringSettings.INTERVAL.getKey(), "-1")
                       .put("xpack.monitoring.exporters." + exporterName + ".type", LocalExporter.TYPE)
                       .put("xpack.monitoring.exporters." + exporterName +  ".enabled", false)
                       .put(XPackSettings.WATCHER_ENABLED.getKey(), enableWatcher())
                       .put(NetworkModule.HTTP_ENABLED.getKey(), false)
                       .build();
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                       .put(super.nodeSettings(nodeOrdinal))
                       .put(localExporterSettings())
                       .build();
    }

    /**
     * Create a new {@link LocalExporter}. Expected usage:
     * <pre><code>
     * final Settings settings = Settings.builder().put("xpack.monitoring.exporters._local.type", "local").build();
     * try (LocalExporter exporter = createLocalExporter("_local", settings)) {
     *   // ...
     * }
     * </code></pre>
     *
     * @return Never {@code null}.
     */
    protected LocalExporter createLocalExporter() {
        final Settings settings = localExporterSettings();
        final XPackLicenseState licenseState = new XPackLicenseState();
        final Exporter.Config config =
                new Exporter.Config(exporterName, "local",
                                    settings, settings.getAsSettings("xpack.monitoring.exporters." + exporterName),
                                    clusterService(), licenseState);
        final CleanerService cleanerService =
                new CleanerService(settings, clusterService().getClusterSettings(), THREADPOOL, licenseState);

        return new LocalExporter(config, new InternalClient(settings, THREADPOOL, client(), null), cleanerService);
    }

}
