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
import org.elasticsearch.common.transport.LocalTransportAddress;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.action.GetJobsStatsAction.Response.JobStats;
import org.elasticsearch.xpack.monitoring.MonitoredSystem;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link JobStatsMonitoringDocTests}.
 */
public class JobStatsMonitoringDocTests extends ESTestCase {

    private final String clusterUuid = randomAlphaOfLength(5);
    private final long timestamp = System.currentTimeMillis();
    private final String nodeUuid = randomAlphaOfLength(5);
    private final DiscoveryNode node =
            new DiscoveryNode(nodeUuid, LocalTransportAddress.buildUnique(), Version.CURRENT);
    private final JobStats jobStats = mock(JobStats.class);

    private final JobStatsMonitoringDoc doc = new JobStatsMonitoringDoc(clusterUuid, timestamp, node, jobStats);

    public void testConstructorJobStatsMustNotBeNull() {
        expectThrows(NullPointerException.class,
                     () -> new JobStatsMonitoringDoc(clusterUuid, timestamp, node, null));
    }

    public void testConstructor() {
        assertThat(doc.getMonitoringId(), is(MonitoredSystem.ES.getSystem()));
        assertThat(doc.getMonitoringVersion(), is(Version.CURRENT.toString()));
        assertThat(doc.getType(), is(JobStatsMonitoringDoc.TYPE));
        assertThat(doc.getId(), nullValue());
        assertThat(doc.getClusterUUID(), is(clusterUuid));
        assertThat(doc.getTimestamp(), is(timestamp));
        assertThat(doc.getSourceNode(), notNullValue());
        assertThat(doc.getSourceNode().getUUID(), is(nodeUuid));
        assertThat(doc.getJobStats(), is(jobStats));
    }

}
