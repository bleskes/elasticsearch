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

package org.elasticsearch.alerts.condition;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ConditionRegistry {

    private final ImmutableMap<String, Condition.Parser> parsers;

    @Inject
    public ConditionRegistry(Map<String, Condition.Parser> parsers) {
        this.parsers = ImmutableMap.copyOf(parsers);
    }

    public Set<String> types() {
        return parsers.keySet();
    }

    /**
     * Reads the contents of parser to create the correct Condition
     *
     * @param parser    The parser containing the condition definition
     * @return          A new condition instance from the parser
     * @throws IOException
     */
    public Condition parse(XContentParser parser) throws IOException {
        String type = null;
        XContentParser.Token token;
        Condition condition = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                type = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT && type != null) {
                Condition.Parser conditionParser = parsers.get(type);
                if (conditionParser == null) {
                    throw new ConditionException("unknown condition type [" + type + "]");
                }
                condition = conditionParser.parse(parser);
            }
        }
        return condition;
    }

    /**
     * Reads the contents of parser to create the correct Condition.Result
     *
     * @param parser    The parser containing the condition result definition
     * @return          A new condition result instance from the parser
     * @throws IOException
     */
    public Condition.Result parseResult(XContentParser parser) throws IOException {
        String type = null;
        XContentParser.Token token;
        Condition.Result conditionResult = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                type = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT && type != null) {
                Condition.Parser conditionParser = parsers.get(type);
                if (conditionParser == null) {
                    throw new ConditionException("unknown condition type [" + type + "]");
                }
                conditionResult = conditionParser.parseResult(parser);
            }
        }
        return conditionResult;
    }

}
