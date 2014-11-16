/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.action.admin.cluster.stats;

import com.carrotsearch.hppc.ObjectObjectOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.elasticsearch.action.admin.indices.stats.CommonStatsFlags;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.index.cache.filter.FilterCacheStats;
import org.elasticsearch.index.cache.id.IdCacheStats;
import org.elasticsearch.index.engine.SegmentsStats;
import org.elasticsearch.index.fielddata.FieldDataStats;
import org.elasticsearch.index.percolator.stats.PercolateStats;
import org.elasticsearch.index.shard.DocsStats;
import org.elasticsearch.index.store.StoreStats;
import org.elasticsearch.search.suggest.completion.CompletionStats;

import java.io.IOException;
import java.util.EnumSet;

public class ClusterStatsIndices implements ToXContent, Streamable {

    // all flags that should be accumulated for primaries
    private static final CommonStatsFlags primariesStats;
    // all flags that should be accumulated for replicas
    private static final CommonStatsFlags replicasStats;

    static {
        EnumSet<CommonStatsFlags.Flag> primariesFlags = EnumSet.allOf(CommonStatsFlags.Flag.class);
        primariesFlags.remove(CommonStatsFlags.Flag.Docs);
        primariesFlags.remove(CommonStatsFlags.Flag.Indexing);
        primariesStats = new CommonStatsFlags(primariesFlags.toArray(new CommonStatsFlags.Flag[primariesFlags.size()]));
        EnumSet<CommonStatsFlags.Flag> replicasFlags = EnumSet.complementOf(primariesFlags);
        replicasStats = new CommonStatsFlags(replicasFlags.toArray(new CommonStatsFlags.Flag[replicasFlags.size()]));
    }

    private int indexCount;
    private ShardStats shards;
    private CommonStats indicesStats;

    private ClusterStatsIndices() {
    }

    public ClusterStatsIndices(ClusterStatsNodeResponse[] nodeResponses) {
        ObjectObjectOpenHashMap<String, ShardStats> countsPerIndex = new ObjectObjectOpenHashMap<>();

        this.indicesStats = new CommonStats(CommonStatsFlags.ALL);

        for (ClusterStatsNodeResponse r : nodeResponses) {
            for (org.elasticsearch.action.admin.indices.stats.ShardStats shardStats : r.shardsStats()) {
                ShardStats indexShardStats = countsPerIndex.get(shardStats.getIndex());
                if (indexShardStats == null) {
                    indexShardStats = new ShardStats();
                    countsPerIndex.put(shardStats.getIndex(), indexShardStats);
                }

                indexShardStats.total++;

                CommonStats shardCommonStats = shardStats.getStats();
                if (shardStats.getShardRouting().primary()) {
                    indexShardStats.primaries++;
                    indicesStats.add(shardCommonStats, primariesStats);
                } else {
                    indicesStats.add(shardCommonStats, replicasStats);
                }
            }
        }

        shards = new ShardStats();
        indexCount = countsPerIndex.size();
        for (ObjectObjectCursor<String, ShardStats> indexCountsCursor : countsPerIndex) {
            shards.addIndexShardCount(indexCountsCursor.value);
        }
    }

    public int getIndexCount() {
        return indexCount;
    }

    public ShardStats getShards() {
        return this.shards;
    }

    public CommonStats getStats() {
        return indicesStats;
    }

    ;

    /** Deprecated, use {@link #getStats()}.getDocs() */
    @Deprecated
    public DocsStats getDocs() {
        return getStats().getDocs();
    }

    /** Deprecated, use {@link #getStats()}.getStore() */
    @Deprecated
    public StoreStats getStore() {
        return getStats().getStore();
    }

    /** Deprecated, use {@link #getStats()}.getFieldData() */
    @Deprecated
    public FieldDataStats getFieldData() {
        return getStats().getFieldData();
    }

    /** Deprecated, use {@link #getStats()}.getFilterCache() */
    @Deprecated
    public FilterCacheStats getFilterCache() {
        return getStats().getFilterCache();
    }

    /** Deprecated, use {@link #getStats()}.getIdCache() */
    @Deprecated
    public IdCacheStats getIdCache() {
        return getStats().getIdCache();
    }

    /** Deprecated, use {@link #getStats()}.getCompletion() */
    @Deprecated
    public CompletionStats getCompletion() {
        return getStats().getCompletion();
    }

    /** Deprecated, use {@link #getStats()}.getSegments() */
    @Deprecated
    public SegmentsStats getSegments() {
        return getStats().getSegments();
    }

    /** Deprecated, use {@link #getStats()}.getPercolate() */
    @Deprecated
    public PercolateStats getPercolate() {
        return getStats().getPercolate();
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        indexCount = in.readVInt();
        shards = ShardStats.readShardStats(in);
        if (in.getVersion().onOrAfter(Version.V_1_5_0)) {
            indicesStats = CommonStats.readCommonStats(in);
        } else {
            indicesStats = new CommonStats(CommonStatsFlags.ALL);
            indicesStats.getDocs().add(DocsStats.readDocStats(in));
            indicesStats.getStore().add(StoreStats.readStoreStats(in));
            indicesStats.getFieldData().add(FieldDataStats.readFieldDataStats(in));
            indicesStats.getFilterCache().add(FilterCacheStats.readFilterCacheStats(in));
            indicesStats.getIdCache().add(IdCacheStats.readIdCacheStats(in));
            indicesStats.getCompletion().add(CompletionStats.readCompletionStats(in));
            indicesStats.getSegments().add(SegmentsStats.readSegmentsStats(in));
            indicesStats.getPercolate().add(PercolateStats.readPercolateStats(in));
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(indexCount);
        shards.writeTo(out);
        if (out.getVersion().onOrAfter(Version.V_1_5_0)) {
            indicesStats.writeTo(out);
        } else {
            indicesStats.getDocs().writeTo(out);
            indicesStats.getStore().writeTo(out);
            indicesStats.getFieldData().writeTo(out);
            indicesStats.getFilterCache().writeTo(out);
            indicesStats.getIdCache().writeTo(out);
            indicesStats.getCompletion().writeTo(out);
            indicesStats.getSegments().writeTo(out);
            indicesStats.getPercolate().writeTo(out);
        }
    }

    public static ClusterStatsIndices readIndicesStats(StreamInput in) throws IOException {
        ClusterStatsIndices indicesStats = new ClusterStatsIndices();
        indicesStats.readFrom(in);
        return indicesStats;
    }

    static final class Fields {
        static final XContentBuilderString COUNT = new XContentBuilderString("count");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field(Fields.COUNT, indexCount);
        shards.toXContent(builder, params);
        indicesStats.toXContent(builder, params);
        return builder;
    }

    public static class ShardStats implements ToXContent, Streamable {

        int indices;
        int total;
        int primaries;

        // min/max
        int minIndexShards = -1;
        int maxIndexShards = -1;
        int minIndexPrimaryShards = -1;
        int maxIndexPrimaryShards = -1;
        double minIndexReplication = -1;
        double totalIndexReplication = 0;
        double maxIndexReplication = -1;

        public ShardStats() {
        }

        /**
         * number of indices in the cluster
         */
        public int getIndices() {
            return this.indices;
        }

        /**
         * total number of shards in the cluster
         */
        public int getTotal() {
            return this.total;
        }

        /**
         * total number of primary shards in the cluster
         */
        public int getPrimaries() {
            return this.primaries;
        }

        /**
         * returns how many *redundant* copies of the data the cluster holds - running with no replicas will return 0
         */
        public double getReplication() {
            if (primaries == 0) {
                return 0;
            }
            return (((double) (total - primaries)) / primaries);
        }

        /**
         * the maximum number of shards (primary+replicas) an index has
         */
        public int getMaxIndexShards() {
            return this.maxIndexShards;
        }

        /**
         * the minimum number of shards (primary+replicas) an index has
         */
        public int getMinIndexShards() {
            return this.minIndexShards;
        }

        /**
         * average number of shards (primary+replicas) across the indices
         */
        public double getAvgIndexShards() {
            if (this.indices == 0) {
                return -1;
            }
            return ((double) this.total) / this.indices;
        }

        /**
         * the maximum number of primary shards an index has
         */
        public int getMaxIndexPrimaryShards() {
            return this.maxIndexPrimaryShards;
        }

        /**
         * the minimum number of primary shards an index has
         */
        public int getMinIndexPrimaryShards() {
            return this.minIndexPrimaryShards;
        }

        /**
         * the average number primary shards across the indices
         */
        public double getAvgIndexPrimaryShards() {
            if (this.indices == 0) {
                return -1;
            }
            return ((double) this.primaries) / this.indices;
        }

        /**
         * minimum replication factor across the indices. See {@link #getReplication}
         */
        public double getMinIndexReplication() {
            return this.minIndexReplication;
        }

        /**
         * average replication factor across the indices. See {@link #getReplication}
         */
        public double getAvgIndexReplication() {
            if (indices == 0) {
                return -1;
            }
            return this.totalIndexReplication / this.indices;
        }

        /**
         * maximum replication factor across the indices. See {@link #getReplication
         */
        public double getMaxIndexReplication() {
            return this.maxIndexReplication;
        }

        public void addIndexShardCount(ShardStats indexShardCount) {
            this.indices++;
            this.primaries += indexShardCount.primaries;
            this.total += indexShardCount.total;
            this.totalIndexReplication += indexShardCount.getReplication();
            if (this.indices == 1) {
                // first index, uninitialized.
                minIndexPrimaryShards = indexShardCount.primaries;
                maxIndexPrimaryShards = indexShardCount.primaries;
                minIndexShards = indexShardCount.total;
                maxIndexShards = indexShardCount.total;
                minIndexReplication = indexShardCount.getReplication();
                maxIndexReplication = minIndexReplication;
            } else {
                minIndexShards = Math.min(minIndexShards, indexShardCount.total);
                minIndexPrimaryShards = Math.min(minIndexPrimaryShards, indexShardCount.primaries);
                minIndexReplication = Math.min(minIndexReplication, indexShardCount.getReplication());

                maxIndexShards = Math.max(maxIndexShards, indexShardCount.total);
                maxIndexPrimaryShards = Math.max(maxIndexPrimaryShards, indexShardCount.primaries);
                maxIndexReplication = Math.max(maxIndexReplication, indexShardCount.getReplication());
            }
        }

        public static ShardStats readShardStats(StreamInput in) throws IOException {
            ShardStats c = new ShardStats();
            c.readFrom(in);
            return c;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            indices = in.readVInt();
            total = in.readVInt();
            primaries = in.readVInt();
            minIndexShards = in.readVInt();
            maxIndexShards = in.readVInt();
            minIndexPrimaryShards = in.readVInt();
            maxIndexPrimaryShards = in.readVInt();
            minIndexReplication = in.readDouble();
            totalIndexReplication = in.readDouble();
            maxIndexReplication = in.readDouble();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeVInt(indices);
            out.writeVInt(total);
            out.writeVInt(primaries);
            out.writeVInt(minIndexShards);
            out.writeVInt(maxIndexShards);
            out.writeVInt(minIndexPrimaryShards);
            out.writeVInt(maxIndexPrimaryShards);
            out.writeDouble(minIndexReplication);
            out.writeDouble(totalIndexReplication);
            out.writeDouble(maxIndexReplication);
        }

        static final class Fields {
            static final XContentBuilderString SHARDS = new XContentBuilderString("shards");
            static final XContentBuilderString TOTAL = new XContentBuilderString("total");
            static final XContentBuilderString PRIMARIES = new XContentBuilderString("primaries");
            static final XContentBuilderString REPLICATION = new XContentBuilderString("replication");
            static final XContentBuilderString MIN = new XContentBuilderString("min");
            static final XContentBuilderString MAX = new XContentBuilderString("max");
            static final XContentBuilderString AVG = new XContentBuilderString("avg");
            static final XContentBuilderString INDEX = new XContentBuilderString("index");
        }

        private void addIntMinMax(XContentBuilderString field, int min, int max, double avg, XContentBuilder builder) throws IOException {
            builder.startObject(field);
            builder.field(Fields.MIN, min);
            builder.field(Fields.MAX, max);
            builder.field(Fields.AVG, avg);
            builder.endObject();
        }

        private void addDoubleMinMax(XContentBuilderString field, double min, double max, double avg, XContentBuilder builder) throws IOException {
            builder.startObject(field);
            builder.field(Fields.MIN, min);
            builder.field(Fields.MAX, max);
            builder.field(Fields.AVG, avg);
            builder.endObject();
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject(Fields.SHARDS);
            if (indices > 0) {

                builder.field(Fields.TOTAL, total);
                builder.field(Fields.PRIMARIES, primaries);
                builder.field(Fields.REPLICATION, getReplication());

                builder.startObject(Fields.INDEX);
                addIntMinMax(Fields.SHARDS, minIndexShards, maxIndexShards, getAvgIndexShards(), builder);
                addIntMinMax(Fields.PRIMARIES, minIndexPrimaryShards, maxIndexPrimaryShards, getAvgIndexPrimaryShards(), builder);
                addDoubleMinMax(Fields.REPLICATION, minIndexReplication, maxIndexReplication, getAvgIndexReplication(), builder);
                builder.endObject();
            }
            builder.endObject();
            return builder;
        }

        @Override
        public String toString() {
            return "total [" + total + "] primaries [" + primaries + "]";
        }
    }
}
