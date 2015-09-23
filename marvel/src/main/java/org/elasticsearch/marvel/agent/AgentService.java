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

package org.elasticsearch.marvel.agent;

import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.CollectionUtils;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.marvel.agent.collector.Collector;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterInfoCollector;
import org.elasticsearch.marvel.agent.exporter.Exporter;
import org.elasticsearch.marvel.agent.exporter.Exporters;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.marvel.license.LicenseService;
import org.elasticsearch.node.settings.NodeSettingsService;

import java.util.*;

public class AgentService extends AbstractLifecycleComponent<AgentService> implements NodeSettingsService.Listener {

    private volatile ExportingWorker exportingWorker;

    private volatile Thread workerThread;
    private volatile long samplingInterval;

    private final MarvelSettings marvelSettings;

    private final Collection<Collector> collectors;
    private final Exporters exporters;

    @Inject
    public AgentService(Settings settings, NodeSettingsService nodeSettingsService,
                        LicenseService licenseService, MarvelSettings marvelSettings,
                        Set<Collector> collectors, Exporters exporters) {
        super(settings);
        this.marvelSettings = marvelSettings;
        this.samplingInterval = marvelSettings.interval().millis();

        this.collectors = Collections.unmodifiableSet(filterCollectors(collectors, marvelSettings.collectors()));
        this.exporters = exporters;

        nodeSettingsService.addListener(this);
        logger.trace("marvel is running in [{}] mode", licenseService.mode());
    }

    protected Set<Collector> filterCollectors(Set<Collector> collectors, String[] filters) {
        if (CollectionUtils.isEmpty(filters)) {
            return collectors;
        }

        Set<Collector> list = new HashSet<>();
        for (Collector collector : collectors) {
            if (Regex.simpleMatch(filters, collector.name().toLowerCase(Locale.ROOT))) {
                list.add(collector);
            } else if (collector instanceof ClusterInfoCollector) {
                list.add(collector);
            }
        }
        return list;
    }

    protected void applyIntervalSettings() {
        if (samplingInterval <= 0) {
            logger.info("data sampling is disabled due to interval settings [{}]", samplingInterval);
            if (workerThread != null) {

                // notify  worker to stop on its leisure, not to disturb an exporting operation
                exportingWorker.closed = true;

                exportingWorker = null;
                workerThread = null;
            }
        } else if (workerThread == null || !workerThread.isAlive()) {

            exportingWorker = new ExportingWorker();
            workerThread = new Thread(exportingWorker, EsExecutors.threadName(settings, "marvel.exporters"));
            workerThread.setDaemon(true);
            workerThread.start();

        }
    }

    @Override
    protected void doStart() {
        for (Collector collector : collectors) {
            collector.start();
        }
        exporters.start();
        applyIntervalSettings();
    }

    @Override
    protected void doStop() {
        if (workerThread != null && workerThread.isAlive()) {
            exportingWorker.closed = true;
            workerThread.interrupt();
            try {
                workerThread.join(60000);
            } catch (InterruptedException e) {
                // we don't care...
            }

        }

        for (Collector collector : collectors) {
            collector.stop();
        }

        exporters.stop();
    }

    @Override
    protected void doClose() {
        for (Collector collector : collectors) {
            collector.close();
        }

        for (Exporter exporter : exporters) {
            exporter.close();
        }
    }

    @Override
    public void onRefreshSettings(Settings settings) {
        TimeValue newSamplingInterval = settings.getAsTime(MarvelSettings.INTERVAL, null);
        if (newSamplingInterval != null && newSamplingInterval.millis() != samplingInterval) {
            logger.info("sampling interval updated to [{}]", newSamplingInterval);
            samplingInterval = newSamplingInterval.millis();
            applyIntervalSettings();
        }
    }

    class ExportingWorker implements Runnable {

        volatile boolean closed = false;

        @Override
        public void run() {
            boolean firstRun = true;

            while (!closed) {
                // sleep first to allow node to complete initialization before collecting the first start
                try {
                    long interval = (firstRun && (marvelSettings.startUpDelay() != null)) ? marvelSettings.startUpDelay().millis() : samplingInterval;
                    Thread.sleep(interval);

                    if (closed) {
                        continue;
                    }

                    for (Collector collector : collectors) {
                        logger.trace("collecting {}", collector.name());
                        Collection<MarvelDoc> results = collector.collect();

                        if (results != null && !results.isEmpty()) {
                            for (Exporter exporter : exporters) {
                                exporter.export(results);
                            }
                        }

                        if (closed) {
                            // Stop collecting if the worker is marked as closed
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Throwable t) {
                    logger.error("Background thread had an uncaught exception:", t);
                } finally {
                    firstRun = false;
                }
            }
            logger.debug("worker shutdown");
        }
    }
}
