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

package org.elasticsearch.xpack.watcher.actions;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.WatcherLicensee;
import org.elasticsearch.xpack.support.clock.Clock;
import org.elasticsearch.xpack.watcher.support.validation.Validation;
import org.elasticsearch.xpack.watcher.transform.TransformRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
public class ActionRegistry {

    private final Map<String, ActionFactory> parsers;
    private final TransformRegistry transformRegistry;
    private final Clock clock;
    private final WatcherLicensee watcherLicensee;

    @Inject
    public ActionRegistry(Map<String, ActionFactory> parsers, TransformRegistry transformRegistry, Clock clock,
                          WatcherLicensee watcherLicensee) {
        this.parsers = parsers;
        this.transformRegistry = transformRegistry;
        this.clock = clock;
        this.watcherLicensee = watcherLicensee;
    }

    ActionFactory factory(String type) {
        return parsers.get(type);
    }

    public ExecutableActions parseActions(String watchId, XContentParser parser) throws IOException {
        if (parser.currentToken() != XContentParser.Token.START_OBJECT) {
            throw new ElasticsearchParseException("could not parse actions for watch [{}]. expected an object but found [{}] instead",
                    watchId, parser.currentToken());
        }
        List<ActionWrapper> actions = new ArrayList<>();

        String id = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                id = parser.currentName();
                Validation.Error error = Validation.actionId(id);
                if (error != null) {
                    throw new ElasticsearchParseException("could not parse action [{}] for watch [{}]. {}", id, watchId, error);
                }
            } else if (token == XContentParser.Token.START_OBJECT && id != null) {
                ActionWrapper action = ActionWrapper.parse(watchId, id, parser, this, transformRegistry, clock, watcherLicensee);
                actions.add(action);
            }
        }
        return new ExecutableActions(actions);
    }

}
