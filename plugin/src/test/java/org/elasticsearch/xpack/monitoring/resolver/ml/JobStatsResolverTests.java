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

package org.elasticsearch.xpack.monitoring.resolver.ml;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.transport.LocalTransportAddress;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.ml.action.GetJobsStatsAction.Response.JobStats;
import org.elasticsearch.xpack.ml.job.config.JobState;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCounts;
import org.elasticsearch.xpack.monitoring.collector.ml.JobStatsMonitoringDoc;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringTemplateUtils;
import org.elasticsearch.xpack.monitoring.resolver.MonitoringIndexNameResolverTestCase;

import static org.hamcrest.Matchers.startsWith;

public class JobStatsResolverTests extends MonitoringIndexNameResolverTestCase<JobStatsMonitoringDoc, JobStatsResolver> {

    @Override
    protected JobStatsMonitoringDoc newMonitoringDoc() {
        final JobStats jobStats =
                new JobStats("fake-job1", new DataCounts("fake-job1"),
                             null, JobState.OPENED, null, null, null);

        return new JobStatsMonitoringDoc(randomAlphaOfLength(5),
                                         Math.abs(randomLong()),
                                         new DiscoveryNode("id", LocalTransportAddress.buildUnique(), Version.CURRENT),
                                         jobStats);
    }

    @Override
    protected boolean checkFilters() {
        return false;
    }

    public void testClusterInfoResolver() throws Exception {
        JobStatsMonitoringDoc doc = newMonitoringDoc();
        JobStatsResolver resolver = newResolver();

        assertThat(resolver.index(doc), startsWith(".monitoring-es-" + MonitoringTemplateUtils.TEMPLATE_VERSION));

        assertSource(resolver.source(doc, XContentType.JSON),
                     Sets.newHashSet(
                         "cluster_uuid",
                         "timestamp",
                         "type",
                         "source_node",
                         "job_stats"),
                     XContentType.JSON);
    }

}
