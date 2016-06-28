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

package org.elasticsearch.xpack.notification.email;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ESTestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

/**
 */
public class EmailTests extends ESTestCase {
    public void testEmailParserSelfGenerated() throws Exception {
        String id = "test-id";
        Email.Address from = randomFrom(new Email.Address("from@from.com"), null);
        List<Email.Address> addresses = new ArrayList<>();
        for( int i = 0; i < randomIntBetween(1, 5); ++i){
            addresses.add(new Email.Address("address" + i + "@test.com"));
        }
        Email.AddressList possibleList = new Email.AddressList(addresses);
        Email.AddressList replyTo = randomFrom(possibleList, null);
        Email.Priority priority = randomFrom(Email.Priority.values());
        DateTime sentDate = new DateTime(randomInt(), DateTimeZone.UTC);
        Email.AddressList to = randomFrom(possibleList, null);
        Email.AddressList cc = randomFrom(possibleList, null);
        Email.AddressList bcc = randomFrom(possibleList, null);
        String subject = randomFrom("Random Subject", "", null);
        String textBody = randomFrom("Random Body", "", null);
        String htmlBody = randomFrom("<hr /><b>BODY</b><hr />", "", null);
        Map<String, Attachment> attachments = null;

        Email email = new Email(id, from, replyTo, priority, sentDate, to, cc, bcc, subject, textBody, htmlBody, attachments);

        XContentBuilder builder = XContentFactory.jsonBuilder();
        email.toXContent(builder, ToXContent.EMPTY_PARAMS);

        XContentParser parser = JsonXContent.jsonXContent.createParser(builder.bytes());
        parser.nextToken();

        Email parsedEmail = Email.parse(parser);

        assertThat(email.id, equalTo(parsedEmail.id));
        assertThat(email.from, equalTo(parsedEmail.from));
        assertThat(email.replyTo, equalTo(parsedEmail.replyTo));
        assertThat(email.priority, equalTo(parsedEmail.priority));
        assertThat(email.sentDate, equalTo(parsedEmail.sentDate));
        assertThat(email.to, equalTo(parsedEmail.to));
        assertThat(email.cc, equalTo(parsedEmail.cc));
        assertThat(email.bcc, equalTo(parsedEmail.bcc));
        assertThat(email.subject, equalTo(parsedEmail.subject));
        assertThat(email.textBody, equalTo(parsedEmail.textBody));
        assertThat(email.htmlBody, equalTo(parsedEmail.htmlBody));
    }

}
