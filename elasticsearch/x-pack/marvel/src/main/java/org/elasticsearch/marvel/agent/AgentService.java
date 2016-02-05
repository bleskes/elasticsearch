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

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.CollectionUtils;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.marvel.agent.collector.Collector;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStatsCollector;
import org.elasticsearch.marvel.agent.exporter.ExportBulk;
import org.elasticsearch.marvel.agent.exporter.Exporter;
import org.elasticsearch.marvel.agent.exporter.Exporters;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AgentService extends AbstractLifecycleComponent<AgentService> {

    private volatile ExportingWorker exportingWorker;

    private volatile Thread workerThread;
    private volatile long samplingInterval;
    private final Collection<Collector> collectors;
    private final String[] settingsCollectors;
    private final Exporters exporters;

    @Inject
    public AgentService(Settings settings, ClusterSettings clusterSettings, Set<Collector> collectors, Exporters exporters) {
        super(settings);
        samplingInterval = MarvelSettings.INTERVAL_SETTING.get(settings).millis();
        settingsCollectors = MarvelSettings.COLLECTORS_SETTING.get(settings).toArray(new String[0]);
        clusterSettings.addSettingsUpdateConsumer(MarvelSettings.INTERVAL_SETTING, this::setInterval);
        this.collectors = Collections.unmodifiableSet(filterCollectors(collectors, settingsCollectors));
        this.exporters = exporters;
    }

    private void setInterval(TimeValue interval) {
        this.samplingInterval = interval.millis();
        applyIntervalSettings();
    }

    protected Set<Collector> filterCollectors(Set<Collector> collectors, String[] filters) {
        if (CollectionUtils.isEmpty(filters)) {
            return collectors;
        }

        Set<Collector> list = new HashSet<>();
        for (Collector collector : collectors) {
            if (Regex.simpleMatch(filters, collector.name().toLowerCase(Locale.ROOT))) {
                list.add(collector);
            } else if (collector instanceof ClusterStatsCollector) {
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

    public void stopCollection() {
        if (exportingWorker != null) {
            exportingWorker.collecting = false;
        }
    }

    public void startCollection() {
        if (exportingWorker != null) {
            exportingWorker.collecting = true;
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

    public TimeValue getSamplingInterval() {
        return new TimeValue(samplingInterval, TimeUnit.MILLISECONDS);
    }

    public String[] collectors() {
        return settingsCollectors;
    }

    class ExportingWorker implements Runnable {

        volatile boolean closed = false;
        volatile boolean collecting = true;

        @Override
        public void run() {
            while (!closed) {
                // sleep first to allow node to complete initialization before collecting the first start
                try {
                    Thread.sleep(samplingInterval);

                    if (closed) {
                        continue;
                    }

                    ExportBulk bulk = exporters.openBulk();
                    if (bulk == null) { // exporters are either not ready or faulty
                        continue;
                    }
                    try {
                        if (logger.isTraceEnabled()) {
                            logger.trace("collecting data - collectors [{}]", Strings.collectionToCommaDelimitedString(collectors));
                        }
                        for (Collector collector : collectors) {
                            if (collecting) {
                                Collection<MarvelDoc> docs = collector.collect();
                                if (docs != null) {
                                    logger.trace("bulk [{}] - adding [{}] collected docs from [{}] collector", bulk, docs.size(),
                                            collector.name());
                                    bulk.add(docs);
                                } else {
                                    logger.trace("bulk [{}] - skipping collected docs from [{}] collector", bulk, collector.name());
                                }
                            }
                            if (closed) {
                                // Stop collecting if the worker is marked as closed
                                break;
                            }
                        }
                    } finally {
                        bulk.close(!closed && collecting);
                    }

                } catch (InterruptedException e) {
                    logger.trace("interrupted");
                    Thread.currentThread().interrupt();
                } catch (Throwable t) {
                    logger.error("background thread had an uncaught exception", t);
                }
            }
            logger.debug("worker shutdown");
        }
    }
}
