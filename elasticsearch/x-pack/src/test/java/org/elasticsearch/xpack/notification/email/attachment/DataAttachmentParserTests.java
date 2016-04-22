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

package org.elasticsearch.xpack.notification.email.attachment;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ESTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class DataAttachmentParserTests extends ESTestCase {

    public void testSerializationWorks() throws Exception {
        Map<String, EmailAttachmentParser> attachmentParsers = new HashMap<>();
        attachmentParsers.put(DataAttachmentParser.TYPE, new DataAttachmentParser());
        EmailAttachmentsParser emailAttachmentsParser = new EmailAttachmentsParser(attachmentParsers);

        String id = "some-id";
        XContentBuilder builder = jsonBuilder().startObject().startObject(id)
                .startObject(DataAttachmentParser.TYPE).field("format", randomFrom("yaml", "json")).endObject()
                .endObject().endObject();
        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        logger.info("JSON: {}", builder.string());

        EmailAttachments emailAttachments = emailAttachmentsParser.parse(parser);
        assertThat(emailAttachments.getAttachments(), hasSize(1));

        XContentBuilder toXcontentBuilder = jsonBuilder().startObject();
        List<EmailAttachmentParser.EmailAttachment> attachments = new ArrayList<>(emailAttachments.getAttachments());
        attachments.get(0).toXContent(toXcontentBuilder, ToXContent.EMPTY_PARAMS);
        toXcontentBuilder.endObject();
        assertThat(toXcontentBuilder.string(), is(builder.string()));
    }

}
