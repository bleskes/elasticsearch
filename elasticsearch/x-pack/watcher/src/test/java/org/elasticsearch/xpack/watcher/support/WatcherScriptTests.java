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

package org.elasticsearch.xpack.watcher.support;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.ScriptService.ScriptType;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.equalTo;

public class WatcherScriptTests extends ESTestCase {

    public void testParseScript() throws IOException {
        WatcherScript script = new WatcherScript(randomAsciiOfLengthBetween(1, 5),
                                                randomFrom(ScriptType.values()),
                                                randomBoolean() ? null : randomFrom("custom", "mustache"),
                                                randomBoolean() ? null : randomFrom(emptyMap(), singletonMap("foo", "bar")));

        try (XContentParser parser = createParser(script)) {
            assertThat(WatcherScript.parse(parser), equalTo(script));
        }
    }

    public void testParseScriptWithCustomLang() throws IOException {
        final String lang = randomFrom("custom", "painful");
        final WatcherScript script = new WatcherScript("my-script", randomFrom(ScriptType.values()), lang, null);

        try (XContentParser parser = createParser(script)) {
            WatcherScript result = WatcherScript.parse(parser, WatcherScript.DEFAULT_LANG);
            assertThat(result.script(), equalTo(script.script()));
            assertThat(result.type(), equalTo(script.type()));
            assertThat(result.lang(), equalTo(lang));
            assertThat(result.params(), equalTo(script.params()));
        }
    }

    public void testParseScriptWithDefaultLang() throws IOException {
        final WatcherScript script = new WatcherScript("my-script", randomFrom(ScriptType.values()), null, null);

        try (XContentParser parser = createParser(script)) {
            WatcherScript result = WatcherScript.parse(parser, WatcherScript.DEFAULT_LANG);
            assertThat(result.script(), equalTo(script.script()));
            assertThat(result.type(), equalTo(script.type()));
            assertThat(result.lang(), equalTo(WatcherScript.DEFAULT_LANG));
            assertThat(result.params(), equalTo(script.params()));
        }
    }

    private static XContentParser createParser(WatcherScript watcherScript) throws IOException {
        final XContent xContent = randomFrom(XContentType.values()).xContent();

        XContentBuilder builder = XContentBuilder.builder(xContent);
        watcherScript.toXContent(builder, ToXContent.EMPTY_PARAMS);

        XContentParser parser = XContentHelper.createParser(builder.bytes());
        assertNull(parser.currentToken());
        parser.nextToken();
        return parser;
    }
}
