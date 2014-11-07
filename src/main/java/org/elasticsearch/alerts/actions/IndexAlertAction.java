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

package org.elasticsearch.alerts.actions;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.alerts.Alert;
import org.elasticsearch.alerts.triggers.TriggerResult;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;

public class IndexAlertAction implements AlertAction, ToXContent {
    private String index;
    private String type;
    private Client client = null;
    ESLogger logger = Loggers.getLogger(IndexAlertAction.class);

    public IndexAlertAction(String index, String type, Client client){
        this.index = index;
        this.type = type;
        this.client = client;
    }


    @Override
    public String getActionName() {
        return "index";
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("index", index);
        builder.field("type", type);
        builder.endObject();
        return builder;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(index);
        out.writeString(type);
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        index = in.readString();
        type = in.readString();
    }

    @Override
    public boolean doAction(Alert alert, TriggerResult result) {
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(index);
        indexRequest.type(type);
        try {
            XContentBuilder resultBuilder = XContentFactory.jsonBuilder().prettyPrint();
            resultBuilder.startObject();
            resultBuilder = result.getResponse().toXContent(resultBuilder, ToXContent.EMPTY_PARAMS);
            //resultBuilder.field("timestamp", result.getFireTime()); ///@TODO FIXME
            resultBuilder.endObject();
            indexRequest.source(resultBuilder);
        } catch (IOException ie) {
            throw new ElasticsearchException("Unable to create XContentBuilder",ie);
        }
        return client.index(indexRequest).actionGet().isCreated();
    }
}
