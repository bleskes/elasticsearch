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

package org.elasticsearch.watcher.support;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.WatcherException;

import java.io.IOException;
import java.util.*;

import static org.elasticsearch.common.xcontent.XContentParser.Token.*;

public final class XContentFilterKeysUtils {

    private XContentFilterKeysUtils() {
    }

    public static Map<String, Object> filterMapOrdered(Set<String> keys, XContentParser parser) {
        try {
            if (parser.currentToken() != null) {
                throw new IllegalArgumentException("Parser already started");
            }
            if (parser.nextToken() != START_OBJECT) {
                throw new IllegalArgumentException("Content should start with START_OBJECT");
            }
            State state = new State(new ArrayList<>(keys));
            return parse(parser, state);
        } catch (IOException e) {
            throw new WatcherException("could not build a filtered payload out of xcontent", e);
        }
    }

    private static Map<String, Object> parse(XContentParser parser, State state) throws IOException {
        if (state.includeLeaf) {
            return parser.map();
        }

        Map<String, Object> data = new HashMap<>();
        for (XContentParser.Token token = parser.nextToken(); token != END_OBJECT; token = parser.nextToken()) {
            switch (token) {
                case FIELD_NAME:
                    state.nextField(parser.currentName());
                    break;
                case START_OBJECT:
                    if (state.includeKey) {
                        String fieldName = state.currentFieldName();
                        Map<String, Object> nestedData = parse(parser, state);
                        data.put(fieldName, nestedData);
                    } else {
                        parser.skipChildren();
                    }
                    state.previousField();
                    break;
                case START_ARRAY:
                    if (state.includeKey) {
                        String fieldName = state.currentFieldName();
                        List<Object> arrayData = arrayParsing(parser, state);
                        data.put(fieldName, arrayData);
                    } else {
                        parser.skipChildren();
                    }
                    state.previousField();
                    break;
                case VALUE_STRING:
                    if (state.includeKey) {
                        data.put(state.currentFieldName(), parser.text());
                    }
                    state.previousField();
                    break;
                case VALUE_NUMBER:
                    if (state.includeKey) {
                        data.put(state.currentFieldName(), parser.numberValue());
                    }
                    state.previousField();
                    break;
                case VALUE_BOOLEAN:
                    if (state.includeKey) {
                        data.put(state.currentFieldName(), parser.booleanValue());
                    }
                    state.previousField();
                    break;
            }
        }
        return data;
    }

    private static List<Object> arrayParsing(XContentParser parser, State state) throws IOException {
        List<Object> values = new ArrayList<>();
        for (XContentParser.Token token = parser.nextToken(); token != END_ARRAY; token = parser.nextToken()) {
            switch (token) {
                case START_OBJECT:
                    values.add(parse(parser, state));
                    break;
                case VALUE_STRING:
                    values.add(parser.text());
                    break;
                case VALUE_NUMBER:
                    values.add(parser.numberValue());
                    break;
                case VALUE_BOOLEAN:
                    values.add(parser.booleanValue());
                    break;
            }
        }
        return values;
    }

    private static final class State {

        final List<String> extractPaths;
        StringBuilder currentPath = new StringBuilder();

        boolean includeLeaf;
        boolean includeKey;
        String currentFieldName;

        private State(List<String> extractPaths) {
            this.extractPaths = extractPaths;
        }

        void nextField(String fieldName) {
            currentFieldName = fieldName;
            if (currentPath.length() != 0) {
                currentPath.append('.');
            }
            currentPath = currentPath.append(fieldName);
            final String path = currentPath.toString();
            for (String extractPath : extractPaths) {
                if (path.equals(extractPath)) {
                    includeKey = true;
                    includeLeaf = true;
                    return;
                } else if (extractPath.startsWith(path)) {
                    includeKey = true;
                    return;
                }
            }
            includeKey = false;
            includeLeaf = false;
        }

        String currentFieldName() {
            return currentFieldName;
        }

        void previousField() {
            int start = currentPath.lastIndexOf(currentFieldName);
            currentPath = currentPath.delete(start, currentPath.length());
            if (currentPath.length() > 0 && currentPath.charAt(currentPath.length() - 1) == '.') {
                currentPath = currentPath.deleteCharAt(currentPath.length() - 1);
            }
            currentFieldName = currentPath.toString();
            includeKey = false;
            includeLeaf = false;
        }

    }

}
