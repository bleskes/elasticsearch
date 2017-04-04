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

package org.elasticsearch.xpack.common.http;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.common.http.auth.HttpAuthRegistry;
import org.elasticsearch.xpack.common.http.auth.basic.BasicAuth;
import org.elasticsearch.xpack.common.http.auth.basic.BasicAuthFactory;

import static java.util.Collections.singletonMap;
import static org.elasticsearch.common.xcontent.XContentFactory.cborBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.smileBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.yamlBuilder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

public class HttpRequestTests extends ESTestCase {

    public void testParsingFromUrl() throws Exception {
        HttpRequest.Builder builder = HttpRequest.builder("www.example.org", 1234);
        builder.path("/foo/bar/org");
        builder.setParam("param", "test");
        builder.scheme(Scheme.HTTPS);
        assertThatManualBuilderEqualsParsingFromUrl("https://www.example.org:1234/foo/bar/org?param=test", builder);

        // test without specifying port
        builder = HttpRequest.builder("www.example.org", 80);
        assertThatManualBuilderEqualsParsingFromUrl("http://www.example.org", builder);

        // encoded values
        builder = HttpRequest.builder("www.example.org", 80).setParam("foo", " white space");
        assertThatManualBuilderEqualsParsingFromUrl("http://www.example.org?foo=%20white%20space", builder);
    }

    public void testParsingEmptyUrl() throws Exception {
        try {
            HttpRequest.builder().fromUrl("");
            fail("Expected exception due to empty URL");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), containsString("Configured URL is empty, please configure a valid URL"));
        }
    }

    public void testInvalidUrlsWithMissingScheme() throws Exception {
        try {
            HttpRequest.builder().fromUrl("www.test.de");
            fail("Expected exception due to missing scheme");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), containsString("URL [www.test.de] does not contain a scheme"));
        }
    }

    public void testInvalidUrlsWithHost() throws Exception {
        try {
            HttpRequest.builder().fromUrl("https://");
            fail("Expected exception due to missing host");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), containsString("Malformed URL [https://]"));
        }
    }

    public void testXContentSerialization() throws Exception {
        final HttpRequest.Builder builder;
        if (randomBoolean()) {
            builder = HttpRequest.builder();
            builder.fromUrl("http://localhost:9200/generic/createevent");
        } else {
            builder = HttpRequest.builder("localhost", 9200);
            if (randomBoolean()) {
                builder.scheme(randomFrom(Scheme.values()));
                if (usually()) {
                    builder.path(randomAlphaOfLength(50));
                }
            }
        }
        if (usually()) {
            builder.method(randomFrom(HttpMethod.values()));
        }
        if (randomBoolean()) {
            builder.setParam(randomAlphaOfLength(10), randomAlphaOfLength(10));
            if (randomBoolean()) {
                builder.setParam(randomAlphaOfLength(10), randomAlphaOfLength(10));
            }
        }
        if (randomBoolean()) {
            builder.setHeader(randomAlphaOfLength(10), randomAlphaOfLength(10));
            if (randomBoolean()) {
                builder.setHeader(randomAlphaOfLength(10), randomAlphaOfLength(10));
            }
        }
        if (randomBoolean()) {
            builder.auth(new BasicAuth(randomAlphaOfLength(10), randomAlphaOfLength(20).toCharArray()));
        }
        if (randomBoolean()) {
            builder.body(randomAlphaOfLength(200));
        }
        if (randomBoolean()) {
            builder.connectionTimeout(TimeValue.parseTimeValue(randomTimeValue(), "my.setting"));
        }
        if (randomBoolean()) {
            builder.readTimeout(TimeValue.parseTimeValue(randomTimeValue(), "my.setting"));
        }
        if (randomBoolean()) {
            builder.proxy(new HttpProxy(randomAlphaOfLength(10), randomIntBetween(1024, 65000)));
        }

        final HttpRequest httpRequest = builder.build();
        assertNotNull(httpRequest);

        try (XContentBuilder xContentBuilder = randomFrom(jsonBuilder(), smileBuilder(), yamlBuilder(), cborBuilder())) {
            httpRequest.toXContent(xContentBuilder, ToXContent.EMPTY_PARAMS);

            HttpAuthRegistry registry = new HttpAuthRegistry(singletonMap(BasicAuth.TYPE, new BasicAuthFactory(null)));
            HttpRequest.Parser httpRequestParser = new HttpRequest.Parser(registry);
    
            try (XContentParser parser = createParser(xContentBuilder)) {
                assertNull(parser.currentToken());
                parser.nextToken();
    
                HttpRequest parsedRequest = httpRequestParser.parse(parser);
                assertEquals(httpRequest, parsedRequest);
            }
        }
    }

    private void assertThatManualBuilderEqualsParsingFromUrl(String url, HttpRequest.Builder builder) throws Exception {
        XContentBuilder urlContentBuilder = jsonBuilder().startObject().field("url", url).endObject();
        XContentParser urlContentParser = createParser(urlContentBuilder);
        urlContentParser.nextToken();

        HttpRequest.Parser parser = new HttpRequest.Parser(mock(HttpAuthRegistry.class));
        HttpRequest urlParsedRequest = parser.parse(urlContentParser);

        XContentBuilder xContentBuilder = builder.build().toXContent(jsonBuilder(), ToXContent.EMPTY_PARAMS);
        XContentParser xContentParser = createParser(xContentBuilder);
        xContentParser.nextToken();
        HttpRequest parsedRequest = parser.parse(xContentParser);

        assertThat(parsedRequest, is(urlParsedRequest));
    }
}
