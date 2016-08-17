/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
            assertThat(result.getTemplate().script(), equalTo(expectedScript));
            assertThat(result.getTemplate().lang(), equalTo(expectedLang));
            assertThat(result.getTemplate().params(), equalTo(expectedParams));
        } catch (IOException e) {
            fail("Failed to parse watch search request: " + e.getMessage());
        }
    }
}
