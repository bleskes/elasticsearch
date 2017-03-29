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

package org.elasticsearch.xpack.monitoring.resolver.cluster;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.monitoring.collector.cluster.ClusterStateNodeMonitoringDoc;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringTemplateUtils;
import org.elasticsearch.xpack.monitoring.resolver.MonitoringIndexNameResolverTestCase;

import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.elasticsearch.common.transport.LocalTransportAddress.buildUnique;
import static org.hamcrest.Matchers.equalTo;

public class ClusterStateNodeResolverTests extends
        MonitoringIndexNameResolverTestCase<ClusterStateNodeMonitoringDoc, ClusterStateNodeResolver> {

    @Override
    protected ClusterStateNodeMonitoringDoc newMonitoringDoc() {
        ClusterStateNodeMonitoringDoc doc = new ClusterStateNodeMonitoringDoc(randomMonitoringId(),
                randomAsciiOfLength(2), randomAsciiOfLength(5), 1437580442979L,
                new DiscoveryNode("id", buildUnique(), emptyMap(), emptySet(), Version.CURRENT),
                UUID.randomUUID().toString(), randomAsciiOfLength(5));
        return doc;
    }

    @Override
    protected boolean checkFilters() {
        return false;
    }

    public void testClusterStateNodeResolver() throws Exception {
        ClusterStateNodeMonitoringDoc doc = newMonitoringDoc();

        ClusterStateNodeResolver resolver = newResolver();
        assertThat(resolver.index(doc), equalTo(".monitoring-es-" + MonitoringTemplateUtils.TEMPLATE_VERSION + "-2015.07.22"));

        assertSource(resolver.source(doc, XContentType.JSON),
                Sets.newHashSet(
                        "cluster_uuid",
                        "timestamp",
                        "source_node",
                        "state_uuid",
                        "node.id"), XContentType.JSON);
    }
}
