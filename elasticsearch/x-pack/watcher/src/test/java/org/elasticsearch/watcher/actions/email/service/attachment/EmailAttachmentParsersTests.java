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

import com.google.common.base.Charsets;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.watcher.actions.email.service.Attachment;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.support.http.HttpRequestTemplate;
import org.elasticsearch.watcher.support.http.Scheme;
import org.elasticsearch.watcher.watch.Payload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

public class EmailAttachmentParsersTests extends ESTestCase {

    private WatchExecutionContext ctx = mock(WatchExecutionContext.class);

    public void testThatCustomParsersCanBeRegistered() throws Exception {
        Map<String, EmailAttachmentParser> parsers = new HashMap<>();
        parsers.put("test", new TestEmailAttachmentParser());
        EmailAttachmentsParser parser = new EmailAttachmentsParser(parsers);

        XContentBuilder builder = jsonBuilder();
        builder.startObject()
                .startObject("my-id")
                .startObject("test")
                .field("foo", "bar")
                .endObject()
                .endObject()
                .startObject("my-other-id")
                .startObject("test")
                .field("foo", "baz")
                .endObject()
                .endObject()
                .endObject();

        logger.info("JSON: {}", builder.string());
        XContentParser xContentParser = JsonXContent.jsonXContent.createParser(builder.bytes());
        EmailAttachments attachments = parser.parse(xContentParser);
        assertThat(attachments.getAttachments(), hasSize(2));

        EmailAttachmentParser.EmailAttachment emailAttachment = attachments.getAttachments().get(0);
        assertThat(emailAttachment, instanceOf(TestEmailAttachment.class));

        Attachment attachment = parsers.get("test").toAttachment(ctx, new Payload.Simple(), emailAttachment);
        assertThat(attachment.name(), is("my-id"));
        assertThat(attachment.contentType(), is("personalContentType"));

        assertThat(parsers.get("test").toAttachment(ctx, new Payload.Simple(),
                attachments.getAttachments().get(1)).id(), is("my-other-id"));
    }

    public void testThatUnknownParserThrowsException() throws IOException {
        EmailAttachmentsParser parser = new EmailAttachmentsParser(Collections.emptyMap());

        XContentBuilder builder = jsonBuilder();
        String type = randomAsciiOfLength(8);
        builder.startObject().startObject("some-id").startObject(type);

        XContentParser xContentParser = JsonXContent.jsonXContent.createParser(builder.bytes());
        try {
            parser.parse(xContentParser);
            fail("Expected random parser of type [" + type + "] to throw an exception");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), containsString("Cannot parse attachment of type " + type));
        }
    }

    public void testThatToXContentSerializationWorks() throws Exception {
        List<EmailAttachmentParser.EmailAttachment> attachments = new ArrayList<>();
        attachments.add(new DataAttachment("my-id", org.elasticsearch.watcher.actions.email.DataAttachment.JSON));

        HttpRequestTemplate requestTemplate = HttpRequestTemplate.builder("localhost", 80).scheme(Scheme.HTTP).path("/").build();
        HttpRequestAttachment httpRequestAttachment = new HttpRequestAttachment("other-id", requestTemplate, null);

        attachments.add(httpRequestAttachment);
        EmailAttachments emailAttachments = new EmailAttachments(attachments);
        XContentBuilder builder = jsonBuilder();
        emailAttachments.toXContent(builder, ToXContent.EMPTY_PARAMS);
        logger.info("JSON is: " + builder.string());
        assertThat(builder.string(), containsString("my-id"));
        assertThat(builder.string(), containsString("json"));
        assertThat(builder.string(), containsString("other-id"));
        assertThat(builder.string(), containsString("localhost"));
        assertThat(builder.string(), containsString("/"));
    }

    public class TestEmailAttachmentParser implements EmailAttachmentParser<TestEmailAttachment> {

        @Override
        public String type() {
            return "test";
        }

        @Override
        public TestEmailAttachment parse(String id, XContentParser parser) throws IOException {
            TestEmailAttachment attachment = null;
            String currentFieldName = null;
            XContentParser.Token token;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else {
                    if ("foo".equals(currentFieldName)) {
                        attachment = new TestEmailAttachment(id, parser.text());
                    }
                }
            }

            if (attachment == null) {
                throw new ElasticsearchParseException("Expected test parser to have field [foo]");
            }

            return attachment;
        }

        @Override
        public Attachment toAttachment(WatchExecutionContext ctx, Payload payload, TestEmailAttachment attachment) {
            return new Attachment.Bytes(attachment.getId(), attachment.getValue().getBytes(Charsets.UTF_8), "personalContentType");
        }
    }

    public static class TestEmailAttachment implements EmailAttachmentParser.EmailAttachment {

        private final String value;
        private final String id;

        interface Fields {
            ParseField FOO = new ParseField("foo");
        }

        public TestEmailAttachment(String id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public String type() {
            return "test";
        }

        public String getValue() {
            return value;
        }

        public String getId() {
            return id;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject(id)
                    .startObject(type())
                    .field(Fields.FOO.getPreferredName(), value)
                    .endObject()
                    .endObject();
        }
    }
}
