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

package org.elasticsearch.alerts.actions.index;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.alerts.Alert;
import org.elasticsearch.alerts.actions.Action;
import org.elasticsearch.alerts.actions.ActionException;
import org.elasticsearch.alerts.support.init.proxy.ClientProxy;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.alerts.support.AlertUtils.responseToData;

/**
 */
public class IndexAction extends Action<IndexAction.Result> {

    public static final String TYPE = "index";

    private final ClientProxy client;

    private final String index;
    private final String type;

    protected IndexAction(ESLogger logger, ClientProxy client, String index, String type) {
        super(logger);
        this.client = client;
        this.index = index;
        this.type = type;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Result execute(Alert alert, Map<String, Object> data) throws IOException {
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(index);
        indexRequest.type(type);
        try {
            XContentBuilder resultBuilder = XContentFactory.jsonBuilder().prettyPrint();
            resultBuilder.startObject();
            resultBuilder.field("response", data);
            resultBuilder.field("timestamp", alert.status().lastExecuted());
            resultBuilder.endObject();
            indexRequest.source(resultBuilder);
        } catch (IOException ie) {
            throw new ActionException("failed to index result for alert [" + alert.name() + " ]", ie);
        }

        return new Result(client.index(indexRequest).actionGet());
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(Parser.INDEX_FIELD.getPreferredName(), index);
        builder.field(Parser.TYPE_FIELD.getPreferredName(), type);
        builder.endObject();
        return builder;
    }

    public static class Parser extends AbstractComponent implements Action.Parser<IndexAction> {

        public static final ParseField INDEX_FIELD = new ParseField("index");
        public static final ParseField TYPE_FIELD = new ParseField("type");

        private final ClientProxy client;

        @Inject
        public Parser(Settings settings, ClientProxy client) {
            super(settings);
            this.client = client;
        }

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public IndexAction parse(XContentParser parser) throws IOException {
            String index = null;
            String type = null;

            String currentFieldName = null;
            XContentParser.Token token;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else if (token.isValue()) {
                    if (INDEX_FIELD.match(currentFieldName)) {
                        index = parser.text();
                    } else if (TYPE_FIELD.match(currentFieldName)) {
                        type = parser.text();
                    } else {
                        throw new ActionException("could not parse index action. unexpected field [" + currentFieldName + "]");
                    }
                } else {
                    throw new ActionException("could not parse index action. unexpected token [" + token + "]");
                }
            }

            if (index == null) {
                throw new ActionException("could not parse index action [index] is required");
            }

            if (type == null) {
                throw new ActionException("could not parse index action [type] is required");
            }

            return new IndexAction(logger, client, index, type);
        }
    }

    public static class Result extends Action.Result {

        private final IndexResponse response;

        public Result(IndexResponse response) {
            super(TYPE, response.isCreated());
            this.response = response;
        }

        public IndexResponse response() {
            return response;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field("success", success());
            builder.field("index_response", responseToData(response()));
            builder.endObject();
            return builder;
        }
    }

}
