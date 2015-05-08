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

package org.elasticsearch.watcher.actions.email.service;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.elasticsearch.watcher.actions.email.service.EmailTemplate.sanitizeHtml;
import static org.hamcrest.Matchers.equalTo;


/**
 */
public class HtmlSanitizeTests extends ElasticsearchTestCase {

    @Test
    public void test_HtmlSanitizer_onclick() {
        String badHtml = "<button type=\"button\"" +
                "onclick=\"document.getElementById('demo').innerHTML = Date()\">" +
                "Click me to display Date and Time.</button>";
        byte[] bytes = new byte[0];
        String sanitizedHtml = sanitizeHtml(badHtml, ImmutableMap.of("foo", (Attachment) new Attachment.Bytes("foo", bytes, "")));
        assertThat(sanitizedHtml, equalTo("Click me to display Date and Time."));
    }

    @Test
    public void test_HtmlSanitizer_Nonattachment_img() {
        String badHtml = "<img src=\"http://test.com/nastyimage.jpg\"/>This is a bad image";
        byte[] bytes = new byte[0];
        String sanitizedHtml = sanitizeHtml(badHtml, ImmutableMap.of("foo", (Attachment) new Attachment.Bytes("foo", bytes, "")));
        assertThat(sanitizedHtml, equalTo("This is a bad image"));
    }

    @Test
    public void test_HtmlSanitizer_Goodattachment_img() {
        String goodHtml = "<img src=\"cid:foo\" />This is a good image";
        byte[] bytes = new byte[0];
        String sanitizedHtml = sanitizeHtml(goodHtml, ImmutableMap.of("foo", (Attachment) new Attachment.Bytes("foo", bytes, "")));
        assertThat(sanitizedHtml, equalTo(goodHtml));
    }

    @Test
    public void test_HtmlSanitizer_table() {
        String goodHtml = "<table><tr><td>cell1</td><td>cell2</td></tr></table>";
        byte[] bytes = new byte[0];
        String sanitizedHtml = sanitizeHtml(goodHtml, ImmutableMap.of("foo", (Attachment) new Attachment.Bytes("foo", bytes, "")));
        assertThat(sanitizedHtml, equalTo(goodHtml));

    }

    @Test
    public void test_HtmlSanitizer_Badattachment_img() {
        String goodHtml = "<img src=\"cid:bad\" />This is a bad image";
        byte[] bytes = new byte[0];
        String sanitizedHtml = sanitizeHtml(goodHtml, ImmutableMap.of("foo", (Attachment) new Attachment.Bytes("foo", bytes, "")));
        assertThat(sanitizedHtml, equalTo("This is a bad image"));
    }

    @Test
    public void test_HtmlSanitizer_Script() {
        String badHtml = "<script>doSomethingNefarious()</script>This was a dangerous script";
        byte[] bytes = new byte[0];
        String sanitizedHtml = sanitizeHtml(badHtml, ImmutableMap.of("foo", (Attachment) new Attachment.Bytes("foo", bytes, "")));
        assertThat(sanitizedHtml, equalTo("This was a dangerous script"));
    }

    @Test
    public void test_HtmlSanitizer_FullHtmlWithMetaString() {
        String needsSanitation = "<html><head></head><body><h1>Hello {{ctx.metadata.name}}</h1> meta <a href='https://www.google.com/search?q={{ctx.metadata.name}}'>Testlink</a>meta</body></html>";
        byte[] bytes = new byte[0];
        String sanitizedHtml = sanitizeHtml(needsSanitation, ImmutableMap.of("foo", (Attachment) new Attachment.Bytes("foo", bytes, "")));
        assertThat(sanitizedHtml, equalTo("<head></head><body><h1>Hello {{ctx.metadata.name}}</h1> meta <a href=\"https://www.google.com/search?q&#61;{{ctx.metadata.name}}\" rel=\"nofollow\">Testlink</a>meta</body>"));
    }

}
