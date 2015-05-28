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

package org.elasticsearch.watcher.actions.index;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.actions.Action;
import org.elasticsearch.watcher.watch.Payload;

import java.io.IOException;

/**
 *
 */
public class IndexAction implements Action {

    public static final String TYPE = "index";

    final String index;
    final String docType;

    public IndexAction(String index, String docType) {
        this.index = index;
        this.docType = docType;
    }

    @Override
    public String type() {
        return TYPE;
    }

    public String getIndex() {
        return index;
    }

    public String getDocType() {
        return docType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexAction action = (IndexAction) o;

        if (!index.equals(action.index)) return false;
        return docType.equals(action.docType);
    }

    @Override
    public int hashCode() {
        int result = index.hashCode();
        result = 31 * result + docType.hashCode();
        return result;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject()
                .field(Field.INDEX.getPreferredName(), index)
                .field(Field.DOC_TYPE.getPreferredName(), docType)
                .endObject();
    }

    public static IndexAction parse(String watchId, String actionId, XContentParser parser) throws IOException {
        String index = null;
        String docType = null;

        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.VALUE_STRING) {
                if (Field.INDEX.match(currentFieldName)) {
                    index = parser.text();
                } else if (Field.DOC_TYPE.match(currentFieldName)) {
                    docType = parser.text();
                } else {
                    throw new IndexActionException("could not parse [{}] action [{}/{}]. unexpected string field [{}]", TYPE, watchId, actionId, currentFieldName);
                }
            } else {
                throw new IndexActionException("could not parse [{}] action [{}/{}]. unexpected token [{}]", TYPE, watchId, actionId, token);
            }
        }

        if (index == null) {
            throw new IndexActionException("could not parse [{}] action [{}/{}]. missing required [{}] field", TYPE, watchId, actionId, Field.INDEX.getPreferredName());
        }

        if (docType == null) {
            throw new IndexActionException("could not parse [{}] action [{}/{}]. missing required [{}] field", TYPE, watchId, actionId, Field.DOC_TYPE.getPreferredName());
        }

        return new IndexAction(index, docType);
    }

    private static void assertNotNull(Object value, String message, Object... args) {
        if (value == null) {
            throw new IndexActionException(message, args);
        }
    }

    public static Builder builder(String index, String docType) {
        return new Builder(index, docType);
    }

    public interface Result {

        class Success extends Action.Result implements Result {

            private final Payload response;

            public Success(Payload response) {
                super(TYPE, Status.SUCCESS);
                this.response = response;
            }

            public Payload response() {
                return response;
            }

            @Override
            protected XContentBuilder xContentBody(XContentBuilder builder, Params params) throws IOException {
                if (response != null) {
                    builder.field(Field.RESPONSE.getPreferredName(), response, params);
                }
                return builder;
            }
        }

        class Simulated extends Action.Result implements Result {

            private final String index;
            private final String docType;
            private final Payload source;

            protected Simulated(String index, String docType, Payload source) {
                super(TYPE, Status.SIMULATED);
                this.index = index;
                this.docType = docType;
                this.source = source;
            }

            public String index() {
                return index;
            }

            public String docType() {
                return docType;
            }

            public Payload source() {
                return source;
            }

            @Override
            protected XContentBuilder xContentBody(XContentBuilder builder, Params params) throws IOException {
                return builder.startObject(Field.REQUEST.getPreferredName())
                        .field(Field.INDEX.getPreferredName(), index)
                        .field(Field.DOC_TYPE.getPreferredName(), docType)
                        .field(Field.SOURCE.getPreferredName(), source, params)
                        .endObject();
            }
        }
    }

    public static class Builder implements Action.Builder<IndexAction> {

        final String index;
        final String docType;

        private Builder(String index, String docType) {
            this.index = index;
            this.docType = docType;
        }

        @Override
        public IndexAction build() {
            return new IndexAction(index, docType);
        }
    }

    interface Field extends Action.Field {
        ParseField INDEX = new ParseField("index");
        ParseField DOC_TYPE = new ParseField("doc_type");
        ParseField SOURCE = new ParseField("source");
        ParseField RESPONSE = new ParseField("response");
        ParseField REQUEST = new ParseField("request");
    }
}
