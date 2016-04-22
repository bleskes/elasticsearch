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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.marvel.MonitoredSystem;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStateNodeMonitoringDoc;
import org.elasticsearch.marvel.agent.resolver.MonitoringIndexNameResolver;

import java.io.IOException;

public class ClusterStateNodeResolver extends MonitoringIndexNameResolver.Timestamped<ClusterStateNodeMonitoringDoc> {

    public static final String TYPE = "node";

    public ClusterStateNodeResolver(MonitoredSystem id, Settings settings) {
        super(id, settings);
    }

    @Override
    public String type(ClusterStateNodeMonitoringDoc document) {
        return TYPE;
    }

    @Override
    protected void buildXContent(ClusterStateNodeMonitoringDoc document,
                                 XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.field(Fields.STATE_UUID, document.getStateUUID());
        builder.startObject(Fields.NODE);
        builder.field(Fields.ID, document.getNodeId());
        builder.endObject();
    }

    static final class Fields {
        static final String STATE_UUID = "state_uuid";
        static final String NODE = "node";
        static final String ID = "id";
    }
}
