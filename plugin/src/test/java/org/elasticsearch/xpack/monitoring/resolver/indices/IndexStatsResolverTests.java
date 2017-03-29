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
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.ShardStats;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.RecoverySource;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.UnassignedInfo;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.cache.query.QueryCacheStats;
import org.elasticsearch.index.cache.request.RequestCacheStats;
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
import org.elasticsearch.xpack.monitoring.collector.indices.IndexStatsMonitoringDoc;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringTemplateUtils;
import org.elasticsearch.xpack.monitoring.resolver.MonitoringIndexNameResolverTestCase;

import java.nio.file.Path;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.elasticsearch.common.transport.LocalTransportAddress.buildUnique;
import static org.hamcrest.Matchers.equalTo;

public class IndexStatsResolverTests extends MonitoringIndexNameResolverTestCase<IndexStatsMonitoringDoc, IndexStatsResolver> {

    @Override
    protected IndexStatsMonitoringDoc newMonitoringDoc() {
        IndexStatsMonitoringDoc doc = new IndexStatsMonitoringDoc(randomMonitoringId(),
                randomAsciiOfLength(2), randomAsciiOfLength(5), 1437580442979L,
                new DiscoveryNode("id", buildUnique(), emptyMap(), emptySet(), Version.CURRENT),
                randomIndexStats());
        return doc;
    }

    public void testIndexStatsResolver() throws Exception {
        IndexStatsMonitoringDoc doc = newMonitoringDoc();

        IndexStatsResolver resolver = newResolver();
        assertThat(resolver.index(doc), equalTo(".monitoring-es-" + MonitoringTemplateUtils.TEMPLATE_VERSION + "-2015.07.22"));

        assertSource(resolver.source(doc, XContentType.JSON),
                Sets.newHashSet(
                        "cluster_uuid",
                        "timestamp",
                        "source_node",
                        "index_stats"), XContentType.JSON);
    }

    /**
     * @return a random {@link IndexStats} object.
     */
    private IndexStats randomIndexStats() {
        Index index = new Index("test-" + randomIntBetween(0, 5), UUID.randomUUID().toString());
        ShardId shardId = new ShardId(index, 0);
        Path path = createTempDir().resolve("indices").resolve(index.getUUID()).resolve("0");
        ShardRouting shardRouting = ShardRouting.newUnassigned(shardId, true, RecoverySource.StoreRecoverySource.EMPTY_STORE_INSTANCE,
                new UnassignedInfo(UnassignedInfo.Reason.INDEX_CREATED, null));
        shardRouting = shardRouting.initialize("node-0", null, ShardRouting.UNAVAILABLE_EXPECTED_SHARD_SIZE);
        shardRouting = shardRouting.moveToStarted();
        CommonStats stats = new CommonStats();
        stats.fieldData = new FieldDataStats();
        stats.queryCache = new QueryCacheStats();
        stats.requestCache = new RequestCacheStats();
        stats.docs = new DocsStats();
        stats.store = new StoreStats();
        stats.indexing = new IndexingStats();
        stats.search = new SearchStats();
        stats.segments = new SegmentsStats();
        stats.merge = new MergeStats();
        stats.refresh = new RefreshStats();
        ShardStats shardStats = new ShardStats(shardRouting, new ShardPath(false, path, path, shardId), stats, null);
        return new IndexStats(index.getName(), new ShardStats[]{shardStats});
    }
}
