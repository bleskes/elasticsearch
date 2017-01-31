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

package org.elasticsearch.xpack.common.text;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds a template to be used in many places in a watch as configuration.
 *
 * One liner templates are kept around as just strings and {@link Script} is used for
 * parsing/serialization logic for any non inlined templates and/or when templates
 * have custom params, lang or content type.
 */
public class TextTemplate implements ToXContent {

    private final Script script;
    private final String inlineTemplate;

    public TextTemplate(String template) {
        this.script = null;
        this.inlineTemplate = template;
    }

    public TextTemplate(String template, @Nullable XContentType contentType, ScriptType type,
                        @Nullable Map<String, Object> params) {
        Map<String, String> options = null;
        if (type == ScriptType.INLINE) {
            options = new HashMap<>();
            if (contentType != null) {
                options.put(Script.CONTENT_TYPE_OPTION, contentType.mediaType());
            }
        }
        if (params == null) {
            params = new HashMap<>();
        }
        this.script = new Script(type, Script.DEFAULT_TEMPLATE_LANG, template, options, params);
        this.inlineTemplate = null;
    }

    public TextTemplate(Script script) {
        this.script = script;
        this.inlineTemplate = null;
    }

    public Script getScript() {
        return script;
    }

    public String getTemplate() {
        return script != null ? script.getIdOrCode() : inlineTemplate;
    }

    public XContentType getContentType() {
        if (script == null || script.getOptions() == null) {
            return null;
        }

        String mediaType = script.getOptions().get(Script.CONTENT_TYPE_OPTION);

        if (mediaType == null) {
            return null;
        }

        return XContentType.fromMediaTypeOrFormat(mediaType);
    }

    public ScriptType getType() {
        return script != null ? script.getType(): ScriptType.INLINE;
    }

    public Map<String, Object> getParams() {
        return script != null ? script.getParams(): null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TextTemplate template1 = (TextTemplate) o;
        return Objects.equals(script, template1.script) &&
                Objects.equals(inlineTemplate, template1.inlineTemplate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(script, inlineTemplate);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        if (script != null) {
            script.toXContent(builder, params);
        } else {
            builder.value(inlineTemplate);
        }
        return builder;
    }

    public static TextTemplate parse(XContentParser parser) throws IOException {
        if (parser.currentToken() == XContentParser.Token.VALUE_STRING) {
            return new TextTemplate(parser.text());
        } else {
            Script template = Script.parse(parser, Script.DEFAULT_TEMPLATE_LANG);

            // for deprecation of stored script namespaces the default lang is ignored,
            // so the template lang must be set for a stored script
            if (template.getType() == ScriptType.STORED) {
                template = new Script(ScriptType.STORED, Script.DEFAULT_TEMPLATE_LANG, template.getIdOrCode(), template.getParams());
            }

            return new TextTemplate(template);
        }
    }
}

