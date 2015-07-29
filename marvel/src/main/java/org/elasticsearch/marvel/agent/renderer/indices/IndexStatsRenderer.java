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

package org.elasticsearch.marvel.agent.renderer.indices;

import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.marvel.agent.collector.indices.IndexStatsMarvelDoc;
import org.elasticsearch.marvel.agent.renderer.AbstractRenderer;

import java.io.IOException;

public class IndexStatsRenderer extends AbstractRenderer<IndexStatsMarvelDoc> {

    private static final String[] FILTERS = {
            "index_stats.index",
            "index_stats.total.docs.count",
            "index_stats.total.store.size_in_bytes",
            "index_stats.total.store.throttle_time_in_millis",
            "index_stats.total.indexing.throttle_time_in_millis",
    };

    public IndexStatsRenderer() {
        super(FILTERS, true);
    }

    @Override
    protected void doRender(IndexStatsMarvelDoc marvelDoc, XContentBuilder builder, ToXContent.Params params) throws IOException {
        IndexStatsMarvelDoc.Payload payload = marvelDoc.payload();

        builder.startObject(Fields.INDEX_STATS);
        if (payload != null) {
            IndexStats indexStats = payload.getIndexStats();
            if (indexStats != null) {
                builder.field(Fields.INDEX, indexStats.getIndex());

                builder.startObject(Fields.TOTAL);
                if (indexStats.getTotal() != null) {
                    indexStats.getTotal().toXContent(builder, params);
                }
                builder.endObject();
            }
        }
        builder.endObject();
    }

    static final class Fields {
        static final XContentBuilderString INDEX_STATS = new XContentBuilderString("index_stats");
        static final XContentBuilderString INDEX = new XContentBuilderString("index");
        static final XContentBuilderString TOTAL = new XContentBuilderString("total");
    }
}
