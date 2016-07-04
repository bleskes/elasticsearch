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

package org.elasticsearch.xpack.monitoring.agent.resolver.node;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.transport.LocalTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.test.VersionUtils;
import org.elasticsearch.xpack.monitoring.agent.collector.cluster.DiscoveryNodeMonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.exporter.MonitoringTemplateUtils;
import org.elasticsearch.xpack.monitoring.agent.resolver.MonitoringIndexNameResolverTestCase;
import org.elasticsearch.xpack.monitoring.agent.resolver.cluster.DiscoveryNodeResolver;

import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;

public class DiscoveryNodeResolverTests extends MonitoringIndexNameResolverTestCase<DiscoveryNodeMonitoringDoc, DiscoveryNodeResolver> {

    @Override
    protected DiscoveryNodeMonitoringDoc newMonitoringDoc() {
        DiscoveryNodeMonitoringDoc doc = new DiscoveryNodeMonitoringDoc(randomMonitoringId(), randomAsciiOfLength(2));
        doc.setClusterUUID(randomAsciiOfLength(5));
        doc.setTimestamp(Math.abs(randomLong()));
        doc.setSourceNode(new DiscoveryNode("id", LocalTransportAddress.buildUnique(), emptyMap(), emptySet(), Version.CURRENT));
        doc.setNode(new DiscoveryNode(randomAsciiOfLength(3), UUID.randomUUID().toString(),
                LocalTransportAddress.buildUnique(), emptyMap(), emptySet(),
                VersionUtils.randomVersionBetween(random(), VersionUtils.getFirstVersion(), Version.CURRENT)));
        return doc;
    }

    @Override
    protected boolean checkFilters() {
        return false;
    }

    public void testDiscoveryNodeResolver() throws Exception {
        DiscoveryNodeMonitoringDoc doc = newMonitoringDoc();
        doc.setTimestamp(1437580442979L);

        DiscoveryNodeResolver resolver = newResolver();
        assertThat(resolver.index(doc), equalTo(".monitoring-data-" + MonitoringTemplateUtils.TEMPLATE_VERSION));
        assertThat(resolver.type(doc), equalTo(DiscoveryNodeResolver.TYPE));
        assertThat(resolver.id(doc), equalTo(doc.getNode().getId()));

        assertSource(resolver.source(doc, XContentType.JSON),
                "cluster_uuid",
                "timestamp",
                "source_node",
                "node.id",
                "node.name",
                "node.transport_address",
                "node.attributes");
    }
}
