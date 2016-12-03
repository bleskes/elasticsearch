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

package org.elasticsearch.xpack.watcher.transform.script;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.script.Script;
import org.elasticsearch.xpack.watcher.transform.Transform;
import org.elasticsearch.xpack.watcher.watch.Payload;

import java.io.IOException;

public class ScriptTransform implements Transform {

    public static final String TYPE = "script";

    private final Script script;

    public ScriptTransform(Script script) {
        this.script = script;
    }

    @Override
    public String type() {
        return TYPE;
    }

    public Script getScript() {
        return script;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScriptTransform that = (ScriptTransform) o;

        return script.equals(that.script);
    }

    @Override
    public int hashCode() {
        return script.hashCode();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return script.toXContent(builder, params);
    }

    public static ScriptTransform parse(String watchId, XContentParser parser, boolean upgradeSource,
                                        String defaultLegacyScriptLanguage) throws IOException {
        try {
            Script script;
            if (upgradeSource) {
                script = Script.parse(parser, ParseFieldMatcher.EMPTY, defaultLegacyScriptLanguage);
            } else {
                script = Script.parse(parser, ParseFieldMatcher.EMPTY);
            }
            return new ScriptTransform(script);
        } catch (ElasticsearchParseException pe) {
            throw new ElasticsearchParseException("could not parse [{}] transform for watch [{}]. failed to parse script", pe, TYPE,
                    watchId);
        }
    }

    public static Builder builder(Script script) {
        return new Builder(script);
    }

    public static class Result extends Transform.Result {

        public Result(Payload payload) {
            super(TYPE, payload);
        }

        public Result(Exception e) {
            super(TYPE, e);
        }

        @Override
        protected XContentBuilder typeXContent(XContentBuilder builder, Params params) throws IOException {
            return builder;
        }
    }

    public static class Builder implements Transform.Builder<ScriptTransform> {

        private final Script script;

        public Builder(Script script) {
            this.script = script;
        }

        @Override
        public ScriptTransform build() {
            return new ScriptTransform(script);
        }
    }
}
