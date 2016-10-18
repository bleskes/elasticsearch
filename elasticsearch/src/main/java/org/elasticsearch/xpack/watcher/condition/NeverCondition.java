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

package org.elasticsearch.xpack.watcher.condition;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;

import java.io.IOException;

public final class NeverCondition extends Condition {

    public static final String TYPE = "never";
    public static final Result RESULT_INSTANCE = new Result(null, TYPE, false);
    public static final Condition INSTANCE = new NeverCondition();

    private NeverCondition() {
        super(TYPE);
    }

    public static Condition parse(String watchId, XContentParser parser) throws IOException {
        if (parser.currentToken() != XContentParser.Token.START_OBJECT) {
            throw new ElasticsearchParseException("could not parse [{}] condition for watch [{}]. expected an empty object but found [{}]",
                    TYPE, watchId, parser.currentName());
        }
        XContentParser.Token token = parser.nextToken();
        if (token != XContentParser.Token.END_OBJECT) {
            throw new ElasticsearchParseException("could not parse [{}] condition for watch [{}]. expected an empty object but found [{}]",
                    TYPE, watchId, parser.currentName());
        }
        return INSTANCE;
    }

    @Override
    public Result execute(WatchExecutionContext ctx) {
        return RESULT_INSTANCE;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NeverCondition;
    }

    @Override
    public int hashCode() {
        // All instances has to produce the same hashCode because they are all equal
        return 0;
    }
}
