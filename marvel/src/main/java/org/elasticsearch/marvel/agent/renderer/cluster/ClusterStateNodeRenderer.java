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

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStateNodeMarvelDoc;
import org.elasticsearch.marvel.agent.renderer.AbstractRenderer;

import java.io.IOException;

public class ClusterStateNodeRenderer extends AbstractRenderer<ClusterStateNodeMarvelDoc> {

    public ClusterStateNodeRenderer() {
        super(null, false);
    }

    @Override
    protected void doRender(ClusterStateNodeMarvelDoc marvelDoc, XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.field(Fields.STATE_UUID, marvelDoc.getStateUUID());
        builder.startObject(Fields.NODE);
        builder.field(Fields.ID, marvelDoc.getNodeId());
        builder.endObject();
    }

    static final class Fields {
        static final XContentBuilderString STATE_UUID = new XContentBuilderString("state_uuid");
        static final XContentBuilderString NODE = new XContentBuilderString("node");
        static final XContentBuilderString ID = new XContentBuilderString("id");
    }
}
