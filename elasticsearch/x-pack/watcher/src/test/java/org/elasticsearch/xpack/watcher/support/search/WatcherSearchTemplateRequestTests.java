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

package org.elasticsearch.xpack.watcher.support.search;

import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.equalTo;

public class WatcherSearchTemplateRequestTests extends ESTestCase {

    public void testFromXContentWithTemplateDefaultLang() throws IOException {
        String source = "{\"template\":{\"id\":\"default-script\", \"params\":{\"foo\":\"bar\"}}}";
        assertTemplate(source, "default-script", "mustache", singletonMap("foo", "bar"));
    }

    public void testFromXContentWithTemplateCustomLang() throws IOException {
        String source = "{\"template\":{\"file\":\"custom-script\", \"lang\":\"painful\",\"params\":{\"bar\":\"baz\"}}}";
        assertTemplate(source, "custom-script", "painful", singletonMap("bar", "baz"));
    }

    private void assertTemplate(String source, String expectedScript, String expectedLang, Map<String, Object> expectedParams) {
        try (XContentParser parser = XContentHelper.createParser(new BytesArray(source))) {
            parser.nextToken();

            WatcherSearchTemplateRequest result = WatcherSearchTemplateRequest.fromXContent(parser, randomFrom(SearchType.values()));
            assertNotNull(result.getTemplate());
            assertThat(result.getTemplate().getScript(), equalTo(expectedScript));
            assertThat(result.getTemplate().getLang(), equalTo(expectedLang));
            assertThat(result.getTemplate().getParams(), equalTo(expectedParams));
        } catch (IOException e) {
            fail("Failed to parse watch search request: " + e.getMessage());
        }
    }
}
