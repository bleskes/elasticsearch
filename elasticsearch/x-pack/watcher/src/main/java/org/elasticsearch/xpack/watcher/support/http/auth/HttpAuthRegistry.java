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

package org.elasticsearch.xpack.watcher.support.http.auth;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.xpack.watcher.support.Exceptions.illegalArgument;

/**
 *
 */
public class HttpAuthRegistry {

    private final Map<String, HttpAuthFactory> factories;

    @Inject
    public HttpAuthRegistry(Map<String, HttpAuthFactory> factories) {
        this.factories = factories;
    }

    public HttpAuth parse(XContentParser parser) throws IOException {
        String type = null;
        XContentParser.Token token;
        HttpAuth auth = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                type = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT && type != null) {
                HttpAuthFactory factory = factories.get(type);
                if (factory == null) {
                    throw new ElasticsearchParseException("unknown http auth type [{}]", type);
                }
                auth = factory.parse(parser);
            }
        }
        return auth;
    }

    public <A extends HttpAuth, AA extends ApplicableHttpAuth<A>> AA createApplicable(A auth) {
        HttpAuthFactory factory = factories.get(auth.type());
        if (factory == null) {
            throw illegalArgument("unknown http auth type [{}]", auth.type());
        }
        return (AA) factory.createApplicable(auth);
    }

}
