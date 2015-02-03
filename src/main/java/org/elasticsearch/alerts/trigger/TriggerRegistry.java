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

package org.elasticsearch.alerts.trigger;

import org.elasticsearch.alerts.trigger.search.SearchTrigger;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class TriggerRegistry {

    private final ImmutableMap<String, SearchTrigger.Parser> parsers;

    @Inject
    public TriggerRegistry(Map<String, SearchTrigger.Parser> parsers) {
        this.parsers = ImmutableMap.copyOf(parsers);
    }

    /**
     * Reads the contents of parser to create the correct Trigger
     * @param parser The parser containing the trigger definition
     * @return a new AlertTrigger instance from the parser
     * @throws IOException
     */
    public Trigger parse(XContentParser parser) throws IOException {
        String type = null;
        XContentParser.Token token;
        Trigger trigger = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                type = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT && type != null) {
                SearchTrigger.Parser triggerParser = parsers.get(type);
                if (triggerParser == null) {
                    throw new TriggerException("unknown trigger type [" + type + "]");
                }
                trigger = triggerParser.parse(parser);
            }
        }
        return trigger;
    }
}
