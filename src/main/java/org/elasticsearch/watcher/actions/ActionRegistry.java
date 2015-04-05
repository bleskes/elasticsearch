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

package org.elasticsearch.watcher.actions;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.transform.TransformRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class ActionRegistry  {

    private final ImmutableMap<String, Action.Parser> parsers;
    private final TransformRegistry transformRegistry;

    @Inject
    public ActionRegistry(Map<String, Action.Parser> parsers, TransformRegistry transformRegistry) {
        this.parsers = ImmutableMap.copyOf(parsers);
        this.transformRegistry = transformRegistry;
    }

    Action.Parser parser(String type) {
        return parsers.get(type);
    }

    public Actions.Results parseResults(XContentParser parser) throws IOException {
        Map<String, ActionWrapper.Result> results = new HashMap<>();

        String id = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                id = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT && id != null) {
                ActionWrapper.Result result = ActionWrapper.Result.parse(parser, id, this, transformRegistry);
                results.put(id, result);
            }
        }
        return new Actions.Results(results);
    }

    public Actions parseActions(XContentParser parser) throws IOException {
        List<ActionWrapper> actions = new ArrayList<>();

        String id = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                id = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT && id != null) {
                ActionWrapper action = ActionWrapper.parse(parser, id, this, transformRegistry);
                actions.add(action);
            }
        }
        return new Actions(actions);
    }



}
