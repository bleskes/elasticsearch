package org.elasticsearch.alerts.actions;

import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 * Created by brian on 8/17/14.
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
}
