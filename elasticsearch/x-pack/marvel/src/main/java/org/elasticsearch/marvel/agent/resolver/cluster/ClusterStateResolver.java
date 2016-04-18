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

import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.marvel.MonitoredSystem;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStateMonitoringDoc;
import org.elasticsearch.marvel.agent.resolver.MonitoringIndexNameResolver;

import java.io.IOException;
import java.util.Locale;

public class ClusterStateResolver extends MonitoringIndexNameResolver.Timestamped<ClusterStateMonitoringDoc> {

    public static final String TYPE = "cluster_state";

    static final String[] FILTERS = {
            "cluster_uuid",
            "timestamp",
            "source_node",
            "cluster_state.version",
            "cluster_state.master_node",
            "cluster_state.state_uuid",
            "cluster_state.status",
            "cluster_state.nodes",
    };

    public ClusterStateResolver(MonitoredSystem id, int version, Settings settings) {
        super(id, version, settings);
    }

    @Override
    public String type(ClusterStateMonitoringDoc document) {
        return TYPE;
    }

    @Override
    public String[] filters() {
        return FILTERS;
    }

    @Override
    protected void buildXContent(ClusterStateMonitoringDoc document, XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject(Fields.CLUSTER_STATE);
        ClusterState clusterState = document.getClusterState();
        if (clusterState != null) {
            builder.field(Fields.STATUS, document.getStatus().name().toLowerCase(Locale.ROOT));
            clusterState.toXContent(builder, params);
        }
        builder.endObject();
    }

    static final class Fields {
        static final String CLUSTER_STATE = new String(TYPE);
        static final String STATUS = "status";
    }
}
