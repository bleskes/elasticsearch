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

package org.elasticsearch.xpack.monitoring.collector.ml;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.xpack.ml.action.GetJobsStatsAction.Response.JobStats;
import org.elasticsearch.xpack.monitoring.MonitoredSystem;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;

import java.util.Objects;

/**
 * Monitoring document collected by {@link JobStatsCollector}.
 * <p>
 * The collected details provide stats for an individual Machine Learning Job rather than the entire payload.
 */
public class JobStatsMonitoringDoc extends MonitoringDoc {

    public static final String TYPE = "job_stats";

    private final JobStats jobStats;

    public JobStatsMonitoringDoc(final String clusterUuid, final long timestamp, final DiscoveryNode node,
                                 final JobStats jobStats) {
        super(MonitoredSystem.ES.getSystem(), Version.CURRENT.toString(), TYPE, null, clusterUuid, timestamp, node);

        this.jobStats = Objects.requireNonNull(jobStats);
    }

    public JobStats getJobStats() {
        return jobStats;
    }

}
