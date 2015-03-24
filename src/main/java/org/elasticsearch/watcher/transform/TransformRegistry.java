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

package org.elasticsearch.watcher.transform;

import org.elasticsearch.watcher.WatcherSettingsException;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class TransformRegistry {

    private final ImmutableMap<String, Transform.Parser> parsers;

    @Inject
    public TransformRegistry(Map<String, Transform.Parser> parsers) {
        this.parsers = ImmutableMap.copyOf(parsers);
    }

    public Transform parse(XContentParser parser) throws IOException {
        String type = null;
        XContentParser.Token token;
        Transform transform = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                type = parser.currentName();
            } else if (type != null) {
                transform = parse(type, parser);
            }
        }
        return transform;
    }

    public Transform parse(String type, XContentParser parser) throws IOException {
        Transform.Parser transformParser = parsers.get(type);
        if (transformParser == null) {
            throw new WatcherSettingsException("unknown transform type [" + type + "]");
        }
        return transformParser.parse(parser);
    }

    public Transform.Result parseResult(XContentParser parser) throws IOException {
        String type = null;
        XContentParser.Token token;
        Transform.Result result = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                type = parser.currentName();
            } else if (type != null) {
                result = parseResult(type, parser);
            }
        }
        return result;
    }

    public Transform.Result parseResult(String type, XContentParser parser) throws IOException {
        Transform.Parser transformParser = parsers.get(type);
        if (transformParser == null) {
            throw new TransformException("unknown transform type [" + type + "]");
        }
        return transformParser.parseResult(parser);
    }
}
