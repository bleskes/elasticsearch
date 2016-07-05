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

package org.elasticsearch.xpack.monitoring.agent.resolver.indices;

import org.elasticsearch.Version;
import org.elasticsearch.action.admin.indices.recovery.RecoveryResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.transport.LocalTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.indices.recovery.RecoveryState;
import org.elasticsearch.xpack.monitoring.agent.collector.indices.IndexRecoveryMonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.exporter.MonitoringTemplateUtils;
import org.elasticsearch.xpack.monitoring.agent.resolver.MonitoringIndexNameResolverTestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class IndexRecoveryResolverTests extends MonitoringIndexNameResolverTestCase<IndexRecoveryMonitoringDoc, IndexRecoveryResolver> {

    @Override
    protected IndexRecoveryMonitoringDoc newMonitoringDoc() {
        IndexRecoveryMonitoringDoc doc = new IndexRecoveryMonitoringDoc(randomMonitoringId(), randomAsciiOfLength(2));
        doc.setClusterUUID(randomAsciiOfLength(5));
        doc.setTimestamp(Math.abs(randomLong()));
        doc.setSourceNode(new DiscoveryNode("id", LocalTransportAddress.buildUnique(), emptyMap(), emptySet(), Version.CURRENT));

        DiscoveryNode localNode = new DiscoveryNode("foo", LocalTransportAddress.buildUnique(), emptyMap(), emptySet(), Version.CURRENT);
        Map<String, java.util.List<RecoveryState>> shardRecoveryStates = new HashMap<>();
        shardRecoveryStates.put("test", Collections.singletonList(new RecoveryState(new ShardId("test", "uuid", 0), true,
                RecoveryState.Type.STORE, localNode, localNode)));
        doc.setRecoveryResponse(new RecoveryResponse(10, 10, 0, false, shardRecoveryStates, Collections.emptyList()));
        return doc;
    }

    @Override
    protected boolean checkFilters() {
        return false;
    }

    @Override
    protected boolean checkResolvedId() {
        return false;
    }

    public void testIndexRecoveryResolver() throws Exception {
        IndexRecoveryMonitoringDoc doc = newMonitoringDoc();
        doc.setTimestamp(1437580442979L);

        IndexRecoveryResolver resolver = newResolver();
        assertThat(resolver.index(doc), equalTo(".monitoring-es-" + MonitoringTemplateUtils.TEMPLATE_VERSION + "-2015.07.22"));
        assertThat(resolver.type(doc), equalTo(IndexRecoveryResolver.TYPE));
        assertThat(resolver.id(doc), nullValue());

        assertSource(resolver.source(doc, XContentType.JSON),
                "cluster_uuid",
                "timestamp",
                "source_node",
                "index_recovery");
    }
}
