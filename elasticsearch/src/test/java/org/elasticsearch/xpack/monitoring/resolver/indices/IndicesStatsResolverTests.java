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

package org.elasticsearch.xpack.monitoring.resolver.indices;

import org.elasticsearch.Version;
import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponseTestUtils;
import org.elasticsearch.action.admin.indices.stats.ShardStats;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.RecoverySource;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.UnassignedInfo;
import org.elasticsearch.common.transport.LocalTransportAddress;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.cache.query.QueryCacheStats;
import org.elasticsearch.index.engine.SegmentsStats;
import org.elasticsearch.index.fielddata.FieldDataStats;
import org.elasticsearch.index.merge.MergeStats;
import org.elasticsearch.index.refresh.RefreshStats;
import org.elasticsearch.index.search.stats.SearchStats;
import org.elasticsearch.index.shard.DocsStats;
import org.elasticsearch.index.shard.IndexingStats;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.shard.ShardPath;
import org.elasticsearch.index.store.StoreStats;
import org.elasticsearch.xpack.monitoring.collector.indices.IndicesStatsMonitoringDoc;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringTemplateUtils;
import org.elasticsearch.xpack.monitoring.resolver.MonitoringIndexNameResolverTestCase;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class IndicesStatsResolverTests extends MonitoringIndexNameResolverTestCase<IndicesStatsMonitoringDoc, IndicesStatsResolver> {

    @Override
    protected IndicesStatsMonitoringDoc newMonitoringDoc() {
        IndicesStatsMonitoringDoc doc = new IndicesStatsMonitoringDoc(randomMonitoringId(), randomAsciiOfLength(2));
        doc.setClusterUUID(randomAsciiOfLength(5));
        doc.setTimestamp(Math.abs(randomLong()));
        doc.setSourceNode(new DiscoveryNode("id", LocalTransportAddress.buildUnique(), emptyMap(), emptySet(), Version.CURRENT));
        doc.setIndicesStats(randomIndicesStats());
        return doc;
    }

    @Override
    protected boolean checkResolvedId() {
        return false;
    }

    public void testIndicesStatsResolver() throws Exception {
        IndicesStatsMonitoringDoc doc = newMonitoringDoc();
        doc.setClusterUUID(randomAsciiOfLength(5));
        doc.setTimestamp(1437580442979L);
        doc.setSourceNode(new DiscoveryNode("id", LocalTransportAddress.buildUnique(), emptyMap(), emptySet(), Version.CURRENT));

        IndicesStatsResolver resolver = newResolver();
        assertThat(resolver.index(doc), equalTo(".monitoring-es-" + MonitoringTemplateUtils.TEMPLATE_VERSION + "-2015.07.22"));
        assertThat(resolver.type(doc), equalTo(IndicesStatsResolver.TYPE));
        assertThat(resolver.id(doc), nullValue());

        assertSource(resolver.source(doc, XContentType.JSON),
                Sets.newHashSet("cluster_uuid", "timestamp", "source_node", "indices_stats"), XContentType.JSON);
    }

    /**
     * @return a random {@link IndicesStatsResponse} object.
     */
    private IndicesStatsResponse randomIndicesStats() {
        Index index = new Index("test", UUID.randomUUID().toString());

        List<ShardStats> shardStats = new ArrayList<>();
        for (int i=0; i < randomIntBetween(2, 5); i++) {
            ShardId shardId = new ShardId(index, i);
            Path path = createTempDir().resolve("indices").resolve(index.getUUID()).resolve(String.valueOf(i));
            ShardRouting shardRouting = ShardRouting.newUnassigned(shardId, true, RecoverySource.StoreRecoverySource.EMPTY_STORE_INSTANCE,
                    new UnassignedInfo(UnassignedInfo.Reason.INDEX_CREATED, null));
            shardRouting = shardRouting.initialize("node-0", null, ShardRouting.UNAVAILABLE_EXPECTED_SHARD_SIZE);
            shardRouting = shardRouting.moveToStarted();
            CommonStats stats = new CommonStats();
            stats.fieldData = new FieldDataStats();
            stats.queryCache = new QueryCacheStats();
            stats.docs = new DocsStats();
            stats.store = new StoreStats();
            stats.indexing = new IndexingStats();
            stats.search = new SearchStats();
            stats.segments = new SegmentsStats();
            stats.merge = new MergeStats();
            stats.refresh = new RefreshStats();
            shardStats.add(new ShardStats(shardRouting, new ShardPath(false, path, path, shardId), stats, null));
        }
        return IndicesStatsResponseTestUtils.newIndicesStatsResponse(shardStats.toArray(new ShardStats[shardStats.size()]),
                shardStats.size(), shardStats.size(), 0, emptyList());
    }
}
