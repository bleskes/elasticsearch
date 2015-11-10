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

package org.elasticsearch.marvel.agent.renderer.cluster;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.marvel.agent.collector.cluster.DiscoveryNodeMarvelDoc;
import org.elasticsearch.marvel.agent.renderer.AbstractRenderer;

import java.io.IOException;

public class DiscoveryNodeRenderer extends AbstractRenderer<DiscoveryNodeMarvelDoc> {

    public DiscoveryNodeRenderer() {
        super(null, false);
    }

    @Override
    protected void doRender(DiscoveryNodeMarvelDoc marvelDoc, XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject(Fields.NODE);

        DiscoveryNode node = marvelDoc.getNode();
        if (node != null) {
            builder.field(Fields.NAME, node.getName());
            builder.field(Fields.TRANSPORT_ADDRESS, node.getAddress().toString());

            builder.startObject(Fields.ATTRIBUTES);
            for (ObjectObjectCursor<String, String> attr : node.getAttributes()) {
                builder.field(attr.key, attr.value);
            }
            builder.endObject();
            builder.field(Fields.ID, node.getId());
        }

        builder.endObject();
    }

    static final class Fields {
        static final XContentBuilderString NODE = new XContentBuilderString("node");
        static final XContentBuilderString NAME = new XContentBuilderString("name");
        static final XContentBuilderString TRANSPORT_ADDRESS = new XContentBuilderString("transport_address");
        static final XContentBuilderString ATTRIBUTES = new XContentBuilderString("attributes");
        static final XContentBuilderString ID = new XContentBuilderString("id");
    }
}



