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

import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentParser;
import java.io.IOException;
import org.elasticsearch.common.io.stream.StreamInput;

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
    public AlertAction readFrom(StreamInput in) throws IOException {
        return new IndexAlertAction(in.readString(), in.readString(), client);
    }
}
