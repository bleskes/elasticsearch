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

package org.elasticsearch.xpack.monitoring.resolver.node;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.transport.LocalTransportAddress;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.test.VersionUtils;
import org.elasticsearch.xpack.monitoring.collector.cluster.DiscoveryNodeMonitoringDoc;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringTemplateUtils;
import org.elasticsearch.xpack.monitoring.resolver.MonitoringIndexNameResolverTestCase;
import org.elasticsearch.xpack.monitoring.resolver.cluster.DiscoveryNodeResolver;

import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.elasticsearch.test.VersionUtils.randomVersionBetween;
import static org.hamcrest.Matchers.equalTo;

public class DiscoveryNodeResolverTests extends MonitoringIndexNameResolverTestCase<DiscoveryNodeMonitoringDoc, DiscoveryNodeResolver> {

    @Override
    protected DiscoveryNodeMonitoringDoc newMonitoringDoc() {
        DiscoveryNodeMonitoringDoc doc = new DiscoveryNodeMonitoringDoc(randomMonitoringId(),
                randomAlphaOfLength(2), randomAlphaOfLength(5), 1437580442979L,
                new DiscoveryNode(randomAlphaOfLength(3), UUID.randomUUID().toString(),
                    LocalTransportAddress.buildUnique(), emptyMap(), emptySet(),
                    randomVersionBetween(random(), VersionUtils.getFirstVersion(), Version.CURRENT)));
        return doc;
    }

    @Override
    protected boolean checkFilters() {
        return false;
    }

    public void testDiscoveryNodeResolver() throws Exception {
        DiscoveryNodeMonitoringDoc doc = newMonitoringDoc();

        DiscoveryNodeResolver resolver = newResolver();
        assertThat(resolver.index(doc), equalTo(".monitoring-data-" + MonitoringTemplateUtils.TEMPLATE_VERSION));

        assertSource(resolver.source(doc, XContentType.JSON),
                Sets.newHashSet(
                        "cluster_uuid",
                        "timestamp",
                        "source_node",
                        "node.id",
                        "node.name",
                        "node.transport_address",
                        "node.attributes"), XContentType.JSON);
    }
}
