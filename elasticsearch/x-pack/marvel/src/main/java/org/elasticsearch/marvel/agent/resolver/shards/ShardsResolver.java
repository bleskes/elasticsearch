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

package org.elasticsearch.marvel.agent.resolver.shards;

import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.marvel.MonitoringIds;
import org.elasticsearch.marvel.agent.collector.shards.ShardMonitoringDoc;
import org.elasticsearch.marvel.agent.exporter.MarvelTemplateUtils;
import org.elasticsearch.marvel.agent.resolver.MonitoringIndexNameResolver;

import java.io.IOException;

public class ShardsResolver extends MonitoringIndexNameResolver.Timestamped<ShardMonitoringDoc> {

    public static final String TYPE = "shards";

    private static final String[] FILTERS = {
            "cluster_uuid",
            "timestamp",
            "source_node",
            "state_uuid",
            "shard.state",
            "shard.primary",
            "shard.node",
            "shard.relocating_node",
            "shard.shard",
            "shard.index",
    };

    public ShardsResolver(Settings settings) {
        super(MonitoringIds.ES.getId(), MarvelTemplateUtils.TEMPLATE_VERSION, settings);
    }

    @Override
    public String type(ShardMonitoringDoc document) {
        return TYPE;
    }

    @Override
    public String id(ShardMonitoringDoc document) {
        return id(document.getClusterStateUUID(), document.getShardRouting());
    }

    @Override
    public String[] filters() {
        return FILTERS;
    }

    @Override
    protected void buildXContent(ShardMonitoringDoc document, XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.field(Fields.STATE_UUID, document.getClusterStateUUID());

        ShardRouting shardRouting = document.getShardRouting();
        if (shardRouting != null) {
            // ShardRouting is rendered inside a startObject() / endObject() but without a name,
            // so we must use XContentBuilder.field(String, ToXContent, ToXContent.Params) here
            builder.field(Fields.SHARD.underscore().toString(), shardRouting, params);
        }
    }

    static final class Fields {
        static final XContentBuilderString SHARD = new XContentBuilderString("shard");
        static final XContentBuilderString STATE_UUID = new XContentBuilderString("state_uuid");
    }

    /**
     * Compute an id that has the format:
     *
     * {state_uuid}:{node_id || '_na'}:{index}:{shard}:{'p' || 'r'}
     */
    static String id(String stateUUID, ShardRouting shardRouting) {
        StringBuilder builder = new StringBuilder();
        builder.append(stateUUID);
        builder.append(':');
        if (shardRouting.assignedToNode()) {
            builder.append(shardRouting.currentNodeId());
        } else {
            builder.append("_na");
        }
        builder.append(':');
        builder.append(shardRouting.getIndexName());
        builder.append(':');
        builder.append(Integer.valueOf(shardRouting.id()));
        builder.append(':');
        if (shardRouting.primary()) {
            builder.append("p");
        } else {
            builder.append("r");
        }
        return builder.toString();
    }
}
