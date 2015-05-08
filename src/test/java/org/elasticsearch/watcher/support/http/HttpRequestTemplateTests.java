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

package org.elasticsearch.watcher.support.http;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.netty.handler.codec.http.HttpHeaders;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.watcher.support.http.auth.HttpAuthFactory;
import org.elasticsearch.watcher.support.http.auth.HttpAuthRegistry;
import org.elasticsearch.watcher.support.http.auth.basic.BasicAuth;
import org.elasticsearch.watcher.support.http.auth.basic.BasicAuthFactory;
import org.elasticsearch.watcher.support.secret.SecretService;
import org.elasticsearch.watcher.support.template.Template;
import org.elasticsearch.watcher.support.template.TemplateEngine;
import org.junit.Test;

import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.*;

/**
 *
 */
public class HttpRequestTemplateTests extends ElasticsearchTestCase {

    @Test @Repeat(iterations = 5)
    public void testBody_WithXContent() throws Exception {
        XContentType type = randomFrom(XContentType.JSON, XContentType.YAML);
        HttpRequestTemplate template = HttpRequestTemplate.builder("_host", 1234)
                .body(XContentBuilder.builder(type.xContent()).startObject().endObject())
                .build();
        HttpRequest request = template.render(new MockTemplateEngine(), ImmutableMap.<String, Object>of());
        assertThat(request.headers, hasEntry(HttpHeaders.Names.CONTENT_TYPE, type.restContentType()));
    }

    @Test
    public void testBody() throws Exception {
        HttpRequestTemplate template = HttpRequestTemplate.builder("_host", 1234)
                .body("_body")
                .build();
        HttpRequest request = template.render(new MockTemplateEngine(), ImmutableMap.<String, Object>of());
        assertThat(request.headers.size(), is(0));
    }

    @Test @Repeat(iterations = 20)
    public void testParse_SelfGenerated() throws Exception {
        HttpRequestTemplate.Builder builder = HttpRequestTemplate.builder("_host", 1234);

        if (randomBoolean()) {
            builder.method(randomFrom(HttpMethod.values()));
        }
        if (randomBoolean()) {
            builder.path("/path");
        }
        boolean xbody = randomBoolean();
        if (randomBoolean()) {
            if (xbody) {
                builder.body(jsonBuilder().startObject().endObject());
            } else {
                builder.body("_body");
            }
        }
        if (randomBoolean()) {
            builder.auth(new BasicAuth("_username", "_password".toCharArray()));
        }
        if (randomBoolean()) {
            builder.putParam("_key", Template.inline("_value"));
        }
        if (randomBoolean()) {
            builder.putHeader("_key", Template.inline("_value"));
        }

        HttpRequestTemplate template = builder.build();

        HttpAuthRegistry registry = new HttpAuthRegistry(ImmutableMap.<String, HttpAuthFactory>of(BasicAuth.TYPE, new BasicAuthFactory(new SecretService.PlainText())));
        HttpRequestTemplate.Parser parser = new HttpRequestTemplate.Parser(registry);

        XContentBuilder xContentBuilder = template.toXContent(jsonBuilder(), ToXContent.EMPTY_PARAMS);
        XContentParser xContentParser = JsonXContent.jsonXContent.createParser(xContentBuilder.bytes());
        xContentParser.nextToken();
        HttpRequestTemplate parsed = parser.parse(xContentParser);

        assertThat(parsed, equalTo(template));
    }

    static class MockTemplateEngine implements TemplateEngine {
        @Override
        public String render(Template template, Map<String, Object> model) {
            return template.getTemplate();
        }
    }
}
