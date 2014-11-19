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
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.alerts.Alert;
import org.elasticsearch.alerts.triggers.TriggerResult;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 */
public class IndexAlertActionFactory implements AlertActionFactory {

    private final Client client;

    public IndexAlertActionFactory(Client client){
        this.client = client;
    }

    @Override
    public AlertAction createAction(XContentParser parser) throws IOException {
        String index = null;
        String type = null;

        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token.isValue()) {
                switch (currentFieldName) {
                    case "index":
                        index = parser.text();
                        break;
                    case "type":
                        type = parser.text();
                        break;
                    default:
                        throw new ElasticsearchIllegalArgumentException("Unexpected field [" + currentFieldName + "]");
                }
            } else {
                throw new ElasticsearchIllegalArgumentException("Unexpected token [" + token + "]");
            }
        }
        return new IndexAlertAction(index, type, client);
    }

    @Override
    public boolean doAction(AlertAction action, Alert alert, TriggerResult result) {
        if (!(action instanceof IndexAlertAction)) {
            throw new ElasticsearchIllegalStateException("Bad action [" + action.getClass() + "] passed to IndexAlertActionFactory expected [" + IndexAlertAction.class + "]");
        }

        IndexAlertAction indexAlertAction = (IndexAlertAction) action;

        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(indexAlertAction.getIndex());
        indexRequest.type(indexAlertAction.getType());
        try {
            XContentBuilder resultBuilder = XContentFactory.jsonBuilder().prettyPrint();
            resultBuilder.startObject();
            resultBuilder.field("response", result.getResponse());
            resultBuilder.field("timestamp", alert.lastActionFire()); ///@TODO FIXME the firetime should be in the result ?
            resultBuilder.endObject();
            indexRequest.source(resultBuilder);
        } catch (IOException ie) {
            throw new ElasticsearchException("Unable to create XContentBuilder",ie);
        }
        return client.index(indexRequest).actionGet().isCreated();
    }


}
