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

package org.elasticsearch.watcher.support.http.auth;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class HttpAuthRegistry {

    private final ImmutableMap<String, HttpAuth.Parser> parsers;

    @Inject
    public HttpAuthRegistry(Map<String, HttpAuth.Parser> parsers) {
        this.parsers = ImmutableMap.copyOf(parsers);
    }

    public HttpAuth parse(XContentParser parser) throws IOException {
        String type = null;
        XContentParser.Token token;
        HttpAuth auth = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                type = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT && type != null) {
                HttpAuth.Parser inputParser = parsers.get(type);
                if (inputParser == null) {
                    throw new HttpAuthException("unknown http auth type [" + type + "]");
                }
                auth = inputParser.parse(parser);
            }
        }
        return auth;
    }

}
