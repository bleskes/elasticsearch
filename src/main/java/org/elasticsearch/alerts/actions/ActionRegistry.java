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

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
public class ActionRegistry  {

    private final ImmutableMap<String, Action.Parser> parsers;

    @Inject
    public ActionRegistry(Map<String, Action.Parser> parsers) {
        this.parsers = ImmutableMap.copyOf(parsers);
    }

    /**
     * Reads the contents of parser to create the correct Action
     * @param parser The parser containing the trigger definition
     * @return a new Action instance from the parser
     * @throws IOException
     */
    public Action parse(XContentParser parser) throws IOException {
        String type = null;
        XContentParser.Token token;
        Action action = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                type = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT && type != null) {
                Action.Parser triggerParser = parsers.get(type);
                if (triggerParser == null) {
                    throw new ActionException("unknown action type [" + type + "]");
                }
                action = triggerParser.parse(parser);
            }
        }
        return action;
    }

    public AlertActions parseActions(XContentParser parser) throws IOException {
        List<Action> actions = new ArrayList<>();

        while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
            actions.add(parse(parser));
        }

        return new AlertActions(actions);
    }


}
