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

import org.elasticsearch.action.admin.indices.recovery.RecoveryResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.recovery.RecoveryState;
import org.elasticsearch.xpack.monitoring.MonitoredSystem;
import org.elasticsearch.xpack.monitoring.collector.indices.IndexRecoveryMonitoringDoc;
import org.elasticsearch.xpack.monitoring.resolver.MonitoringIndexNameResolver;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class IndexRecoveryResolver extends MonitoringIndexNameResolver.Timestamped<IndexRecoveryMonitoringDoc> {

    public static final String TYPE = "index_recovery";

    public IndexRecoveryResolver(MonitoredSystem id, Settings settings) {
        super(id, settings);
    }

    @Override
    protected void buildXContent(IndexRecoveryMonitoringDoc document,
                                 XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject(Fields.INDEX_RECOVERY);

        RecoveryResponse recovery = document.getRecoveryResponse();
        if (recovery != null) {
            builder.startArray(Fields.SHARDS);
            Map<String, List<RecoveryState>> shards = recovery.shardRecoveryStates();
            if (shards != null) {
                for (Map.Entry<String, List<RecoveryState>> shard : shards.entrySet()) {

                    List<RecoveryState> indexShards = shard.getValue();
                    if (indexShards != null) {
                        for (RecoveryState indexShard : indexShards) {
                            builder.startObject();
                            builder.field(Fields.INDEX_NAME, shard.getKey());
                            indexShard.toXContent(builder, params);
                            builder.endObject();
                        }
                    }
                }
            }
            builder.endArray();
        }

        builder.endObject();
    }

    static final class Fields {
        static final String INDEX_RECOVERY = TYPE;
        static final String SHARDS = "shards";
        static final String INDEX_NAME = "index_name";
    }
}
