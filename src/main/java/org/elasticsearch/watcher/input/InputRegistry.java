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

package org.elasticsearch.watcher.input;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class InputRegistry {

    private final ImmutableMap<String, Input.Parser> parsers;

    @Inject
    public InputRegistry(Map<String, Input.Parser> parsers) {
        this.parsers = ImmutableMap.copyOf(parsers);
    }

    /**
     * Reads the contents of parser to create the correct Input
     *
     * @param parser    The parser containing the input definition
     * @return          A new input instance from the parser
     * @throws java.io.IOException
     */
    public Input parse(XContentParser parser) throws IOException {
        String type = null;
        XContentParser.Token token;
        Input input = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                type = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT && type != null) {
                Input.Parser inputParser = parsers.get(type);
                if (inputParser == null) {
                    throw new InputException("unknown input type [" + type + "]");
                }
                input = inputParser.parse(parser);
            }
        }
        return input;
    }

    public Input.Result parseResult(XContentParser parser) throws IOException {
        String type = null;
        XContentParser.Token token;
        Input.Result inputResult = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                type = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT && type != null) {
                Input.Parser inputParser = parsers.get(type);
                if (inputParser == null) {
                    throw new InputException("unknown input type [" + type + "]");
                }
                inputResult = inputParser.parseResult(parser);
            }
        }
        return inputResult;
    }

}
