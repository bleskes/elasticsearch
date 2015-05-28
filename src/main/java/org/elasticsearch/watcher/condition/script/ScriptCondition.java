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

package org.elasticsearch.watcher.condition.script;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.condition.Condition;
import org.elasticsearch.watcher.support.Script;

import java.io.IOException;

/**
 *
 */
public class ScriptCondition implements Condition {

    public static final String TYPE = "script";

    final Script script;

    public ScriptCondition(Script script) {
        this.script = script;
    }

    @Override
    public final String type() {
        return TYPE;
    }

    public Script getScript() {
        return script;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return script.toXContent(builder, params);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScriptCondition condition = (ScriptCondition) o;

        return script.equals(condition.script);
    }

    @Override
    public int hashCode() {
        return script.hashCode();
    }

    public static ScriptCondition parse(String watchId, XContentParser parser) throws IOException {
        try {
            Script script = Script.parse(parser);
            return new ScriptCondition(script);
        } catch (Script.ParseException pe) {
            throw new ScriptConditionException("could not parse [{}] condition for watch [{}]. failed to parse script", pe, TYPE, watchId);
        }
    }

    public static Builder builder(Script script) {
        return new Builder(script);
    }

    public static class Result extends Condition.Result {

        static final Result MET = new Result(true);
        static final Result UNMET = new Result(false);

        private Result(boolean met) {
            super(TYPE, met);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject()
                    .field(Field.MET.getPreferredName(), met)
                    .endObject();
        }
    }

    public static class Builder implements Condition.Builder<ScriptCondition> {

        private final Script script;

        private Builder(Script script) {
            this.script = script;
        }

        @Override
        public ScriptCondition build() {
            return new ScriptCondition(script);
        }
    }
}
