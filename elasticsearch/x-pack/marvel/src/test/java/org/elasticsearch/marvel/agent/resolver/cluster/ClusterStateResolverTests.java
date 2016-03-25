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

package org.elasticsearch.marvel.agent.resolver.cluster;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.transport.DummyTransportAddress;
import org.elasticsearch.common.transport.LocalTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStateMonitoringDoc;
import org.elasticsearch.marvel.agent.exporter.MarvelTemplateUtils;
import org.elasticsearch.marvel.agent.resolver.MonitoringIndexNameResolverTestCase;

import java.io.IOException;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class ClusterStateResolverTests extends MonitoringIndexNameResolverTestCase<ClusterStateMonitoringDoc, ClusterStateResolver> {

    @Override
    protected ClusterStateMonitoringDoc newMarvelDoc() {
        ClusterStateMonitoringDoc doc = new ClusterStateMonitoringDoc(randomMonitoringId(), randomAsciiOfLength(2));
        doc.setClusterUUID(randomAsciiOfLength(5));
        doc.setTimestamp(Math.abs(randomLong()));
        doc.setSourceNode(new DiscoveryNode("id", DummyTransportAddress.INSTANCE, emptyMap(), emptySet(), Version.CURRENT));
        doc.setStatus(randomFrom(ClusterHealthStatus.values()));

        DiscoveryNode masterNode = new DiscoveryNode("master", new LocalTransportAddress("master"),
                emptyMap(), emptySet(), Version.CURRENT);
        DiscoveryNode otherNode = new DiscoveryNode("other", new LocalTransportAddress("other"), emptyMap(), emptySet(), Version.CURRENT);
        DiscoveryNodes discoveryNodes = DiscoveryNodes.builder().put(masterNode).put(otherNode).masterNodeId(masterNode.id()).build();
        ClusterState clusterState = ClusterState.builder(new ClusterName("test")).nodes(discoveryNodes).build();
        doc.setClusterState(clusterState);
        return doc;
    }

    @Override
    protected boolean checkResolvedId() {
        return false;
    }

    public void testClusterStateResolver() throws IOException {
        ClusterStateMonitoringDoc doc = newMarvelDoc();
        doc.setTimestamp(1437580442979L);

        ClusterStateResolver resolver = newResolver();
        assertThat(resolver.index(doc), equalTo(".monitoring-es-" + MarvelTemplateUtils.TEMPLATE_VERSION + "-2015.07.22"));
        assertThat(resolver.type(doc), equalTo(ClusterStateResolver.TYPE));
        assertThat(resolver.id(doc), nullValue());

        assertSource(resolver.source(doc, XContentType.JSON),
                "cluster_uuid",
                "timestamp",
                "source_node",
                "cluster_state");
    }
}
