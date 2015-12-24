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

package org.elasticsearch.watcher.actions.email.service.attachment;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.watcher.support.http.HttpClient;
import org.elasticsearch.watcher.support.http.HttpRequest;
import org.elasticsearch.watcher.support.http.HttpRequestTemplate;
import org.elasticsearch.watcher.support.http.HttpResponse;
import org.elasticsearch.watcher.support.http.auth.HttpAuthRegistry;
import org.elasticsearch.watcher.support.http.auth.basic.BasicAuth;
import org.elasticsearch.watcher.support.http.auth.basic.BasicAuthFactory;
import org.elasticsearch.watcher.support.secret.SecretService;
import org.elasticsearch.watcher.test.MockTextTemplateEngine;
import org.junit.Before;

import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpEmailAttachementParserTests extends ESTestCase {

    private SecretService.PlainText secretService;
    private HttpAuthRegistry authRegistry;
    private HttpRequestTemplate.Parser httpRequestTemplateParser;
    private HttpClient httpClient;

    @Before
    public void init() throws Exception {
        secretService = new SecretService.PlainText();
        authRegistry = new HttpAuthRegistry(singletonMap(BasicAuth.TYPE, new BasicAuthFactory(secretService)));
        httpRequestTemplateParser = new HttpRequestTemplate.Parser(authRegistry);
        httpClient = mock(HttpClient.class);

        HttpResponse response = new HttpResponse(200, "This is my response".getBytes(UTF_8));
        when(httpClient.execute(any(HttpRequest.class))).thenReturn(response);
    }


    public void testSerializationWorks() throws Exception {
        Map<String, EmailAttachmentParser> attachmentParsers = new HashMap<>();
        attachmentParsers.put(HttpEmailAttachementParser.TYPE, new HttpEmailAttachementParser(httpClient, httpRequestTemplateParser, new MockTextTemplateEngine()));
        EmailAttachmentsParser emailAttachmentsParser = new EmailAttachmentsParser(attachmentParsers);

        String id = "some-id";
        XContentBuilder builder = jsonBuilder().startObject().startObject(id)
                .startObject(HttpEmailAttachementParser.TYPE)
                .startObject("request")
                .field("scheme", "http")
                .field("host", "test.de")
                .field("port", 80)
                .field("method", "get")
                .field("path", "/foo")
                .startObject("params").endObject()
                .startObject("headers").endObject()
                .endObject();

        boolean configureContentType = randomBoolean();
        if (configureContentType) {
            builder.field("content_type", "application/foo");
        }
        builder.endObject().endObject().endObject();
        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        logger.info("JSON: {}", builder.string());

        EmailAttachments emailAttachments = emailAttachmentsParser.parse(parser);
        assertThat(emailAttachments.getAttachments(), hasSize(1));

        XContentBuilder toXcontentBuilder = jsonBuilder().startObject();
        emailAttachments.getAttachments().get(0).toXContent(toXcontentBuilder, ToXContent.EMPTY_PARAMS);
        toXcontentBuilder.endObject();
        assertThat(toXcontentBuilder.string(), is(builder.string()));
    }

}
