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

package org.elasticsearch.alerts.transform;

import org.elasticsearch.alerts.AlertsSettingsException;
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
            } else if (token == XContentParser.Token.START_OBJECT && type != null) {
                Transform.Parser transformParser = parsers.get(type);
                if (transformParser == null) {
                    throw new AlertsSettingsException("unknown transform type [" + type + "]");
                }
                transform = transformParser.parse(parser);
            }
        }
        return transform;
    }

    public Transform parse(String type, XContentParser parser) throws IOException {
        Transform.Parser transformParser = parsers.get(type);
        if (transformParser == null) {
            throw new AlertsSettingsException("unknown transform type [" + type + "]");
        }
        return transformParser.parse(parser);
    }
}
