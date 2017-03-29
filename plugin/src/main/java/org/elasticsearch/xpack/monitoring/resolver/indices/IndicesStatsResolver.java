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

import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.monitoring.MonitoredSystem;
import org.elasticsearch.xpack.monitoring.collector.indices.IndicesStatsMonitoringDoc;
import org.elasticsearch.xpack.monitoring.resolver.MonitoringIndexNameResolver;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public class IndicesStatsResolver extends MonitoringIndexNameResolver.Timestamped<IndicesStatsMonitoringDoc> {

    public static final String TYPE = "indices_stats";

    static final Set<String> FILTERS;
    static {
        Set<String> filters = Sets.newHashSet(
            "cluster_uuid",
            "timestamp",
            "source_node",
            "indices_stats._all.primaries.docs.count",
            "indices_stats._all.primaries.indexing.index_time_in_millis",
            "indices_stats._all.primaries.indexing.index_total",
            "indices_stats._all.primaries.indexing.is_throttled",
            "indices_stats._all.primaries.indexing.throttle_time_in_millis",
            "indices_stats._all.primaries.search.query_time_in_millis",
            "indices_stats._all.primaries.search.query_total",
            "indices_stats._all.primaries.store.size_in_bytes",
            "indices_stats._all.total.docs.count",
            "indices_stats._all.total.indexing.index_time_in_millis",
            "indices_stats._all.total.indexing.index_total",
            "indices_stats._all.total.indexing.is_throttled",
            "indices_stats._all.total.indexing.throttle_time_in_millis",
            "indices_stats._all.total.search.query_time_in_millis",
            "indices_stats._all.total.search.query_total",
            "indices_stats._all.total.store.size_in_bytes"
        );
        FILTERS = Collections.unmodifiableSet(filters);
    }

    public IndicesStatsResolver(MonitoredSystem id, Settings settings) {
        super(id, settings);
    }

    @Override
    public Set<String> filters() {
        return FILTERS;
    }

    @Override
    protected void buildXContent(IndicesStatsMonitoringDoc document, XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject(Fields.INDICES_STATS);
        IndicesStatsResponse indicesStats = document.getIndicesStats();
        if (indicesStats != null) {
            indicesStats.toXContent(builder, params);
        }
        builder.endObject();
    }

    static final class Fields {
        static final String INDICES_STATS = TYPE;
    }
}
