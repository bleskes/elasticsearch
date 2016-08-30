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

package org.elasticsearch.xpack.monitoring.agent.resolver.cluster;

import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.monitoring.MonitoredSystem;
import org.elasticsearch.xpack.monitoring.agent.collector.cluster.ClusterStateMonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.resolver.MonitoringIndexNameResolver;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

public class ClusterStateResolver extends MonitoringIndexNameResolver.Timestamped<ClusterStateMonitoringDoc> {

    public static final String TYPE = "cluster_state";

    static final Set<String> FILTERS;
    static {
        Set<String> filters = Sets.newHashSet(
                "cluster_uuid",
                "timestamp",
                "source_node",
                "cluster_state.version",
                "cluster_state.master_node",
                "cluster_state.state_uuid",
                "cluster_state.status",
                "cluster_state.nodes");
        FILTERS = Collections.unmodifiableSet(filters);
    }

    public ClusterStateResolver(MonitoredSystem id, Settings settings) {
        super(id, settings);
    }

    @Override
    public String type(ClusterStateMonitoringDoc document) {
        return TYPE;
    }

    @Override
    public Set<String> filters() {
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
        static final String CLUSTER_STATE = TYPE;
        static final String STATUS = "status";
    }
}
