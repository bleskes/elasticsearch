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

package org.elasticsearch.shield.action.authc.cache;

import org.elasticsearch.action.support.nodes.BaseNodeResponse;
import org.elasticsearch.action.support.nodes.BaseNodesResponse;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;

/**
 *
 */
public class ClearRealmCacheResponse extends BaseNodesResponse<ClearRealmCacheResponse.Node> implements ToXContent {

    public ClearRealmCacheResponse() {
    }

    public ClearRealmCacheResponse(ClusterName clusterName, Node[] nodes) {
        super(clusterName, nodes);
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        nodes = new Node[in.readVInt()];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = Node.readNodeResponse(in);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(nodes.length);
        for (Node node : nodes) {
            node.writeTo(out);
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("cluster_name", getClusterName().value());
        builder.startObject("nodes");
        for (ClearRealmCacheResponse.Node node: getNodes()) {
            builder.startObject(node.getNode().id());
            builder.field("name", node.getNode().name());
            builder.endObject();
        }
        builder.endObject();
        return builder.endObject();
    }

    @Override
    public String toString() {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().prettyPrint();
            builder.startObject();
            toXContent(builder, EMPTY_PARAMS);
            builder.endObject();
            return builder.string();
        } catch (IOException e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }

    public static class Node extends BaseNodeResponse {

        Node() {
        }

        Node(DiscoveryNode node) {
            super(node);
        }

        public static Node readNodeResponse(StreamInput in) throws IOException {
            Node node = new Node();
            node.readFrom(in);
            return node;
        }
    }

}
