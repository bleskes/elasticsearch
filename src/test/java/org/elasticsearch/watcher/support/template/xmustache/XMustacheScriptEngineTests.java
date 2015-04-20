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

package org.elasticsearch.watcher.support.template.xmustache;

import com.carrotsearch.randomizedtesting.generators.RandomPicks;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

/**
 *
 */
public class XMustacheScriptEngineTests extends ElasticsearchTestCase {

    private XMustacheScriptEngineService engine;

    @Before
    public void setup() {
        engine = new XMustacheScriptEngineService(ImmutableSettings.Builder.EMPTY_SETTINGS);
    }

    @Test
    public void testSimpleParameterReplace() {
        {
            String template = "GET _search {\"query\": " + "{\"boosting\": {" + "\"positive\": {\"match\": {\"body\": \"gift\"}},"
                    + "\"negative\": {\"term\": {\"body\": {\"value\": \"solr\"}" + "}}, \"negative_boost\": {{boost_val}} } }}";
            Map<String, Object> vars = new HashMap<>();
            vars.put("boost_val", "0.3");
            BytesReference o = (BytesReference) engine.execute(engine.compile(template), vars);
            assertEquals("GET _search {\"query\": {\"boosting\": {\"positive\": {\"match\": {\"body\": \"gift\"}},"
                            + "\"negative\": {\"term\": {\"body\": {\"value\": \"solr\"}}}, \"negative_boost\": 0.3 } }}",
                    new String(o.toBytes(), Charset.forName("UTF-8")));
        }
        {
            String template = "GET _search {\"query\": " + "{\"boosting\": {" + "\"positive\": {\"match\": {\"body\": \"gift\"}},"
                    + "\"negative\": {\"term\": {\"body\": {\"value\": \"{{body_val}}\"}" + "}}, \"negative_boost\": {{boost_val}} } }}";
            Map<String, Object> vars = new HashMap<>();
            vars.put("boost_val", "0.3");
            vars.put("body_val", "\"quick brown\"");
            BytesReference o = (BytesReference) engine.execute(engine.compile(template), vars);
            assertEquals("GET _search {\"query\": {\"boosting\": {\"positive\": {\"match\": {\"body\": \"gift\"}},"
                            + "\"negative\": {\"term\": {\"body\": {\"value\": \"\\\"quick brown\\\"\"}}}, \"negative_boost\": 0.3 } }}",
                    new String(o.toBytes(), Charset.forName("UTF-8")));
        }
    }

    @Test
    public void testEscapeJson() throws IOException {
        {
            StringWriter writer = new StringWriter();
            XMustacheFactory.escape("hello \n world", writer);
            assertThat(writer.toString(), equalTo("hello \\\n world"));
        }
        {
            StringWriter writer = new StringWriter();
            XMustacheFactory.escape("\n", writer);
            assertThat(writer.toString(), equalTo("\\\n"));
        }

        Character[] specialChars = new Character[]{'\f', '\n', '\r', '"', '\\', (char) 11, '\t', '\b' };
        int iters = scaledRandomIntBetween(100, 1000);
        for (int i = 0; i < iters; i++) {
            int rounds = scaledRandomIntBetween(1, 20);
            StringWriter escaped = new StringWriter();
            StringWriter writer = new StringWriter();
            for (int j = 0; j < rounds; j++) {
                String s = getChars();
                writer.write(s);
                escaped.write(s);
                char c = RandomPicks.randomFrom(getRandom(), specialChars);
                writer.append(c);
                escaped.append('\\');
                escaped.append(c);
            }
            StringWriter target = new StringWriter();
            assertThat(escaped.toString(), equalTo(XMustacheFactory.escape(writer.toString(), target).toString()));
        }
    }

    private String getChars() {
        String string = randomRealisticUnicodeOfCodepointLengthBetween(0, 10);
        for (int i = 0; i < string.length(); i++) {
            if (XMustacheFactory.isEscapeChar(string.charAt(i))) {
                return string.substring(0, i);
            }
        }
        return string;
    }

}