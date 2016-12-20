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
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.SearchRequestParsers;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.equalTo;

public class WatcherSearchTemplateRequestTests extends ESTestCase {

    public void testFromXContentWithTemplateDefaultLang() throws IOException {
        String source = "{\"template\":{\"stored\":\"default-script\", \"params\":{\"foo\":\"bar\"}}}";
        assertTemplate(source, "default-script", "mustache", singletonMap("foo", "bar"));
    }

    public void testFromXContentWithTemplateCustomLang() throws IOException {
        String source = "{\"template\":{\"file\":\"custom-script\", \"lang\":\"painful\",\"params\":{\"bar\":\"baz\"}}}";
        assertTemplate(source, "custom-script", "painful", singletonMap("bar", "baz"));
    }

    private void assertTemplate(String source, String expectedScript, String expectedLang, Map<String, Object> expectedParams) {
        try (XContentParser parser = createParser(JsonXContent.jsonXContent, source)) {
            parser.nextToken();

            WatcherSearchTemplateRequest result = WatcherSearchTemplateRequest.fromXContent(
                    logger, parser, randomFrom(SearchType.values()), false, null, null, null);
            assertNotNull(result.getTemplate());
            assertThat(result.getTemplate().getIdOrCode(), equalTo(expectedScript));
            assertThat(result.getTemplate().getLang(), equalTo(expectedLang));
            assertThat(result.getTemplate().getParams(), equalTo(expectedParams));
        } catch (IOException e) {
            fail("Failed to parse watch search request: " + e.getMessage());
        }
    }

    public void testUpgradeSearchSource() throws IOException {
        XContentBuilder contentBuilder = JsonXContent.contentBuilder();
        contentBuilder.startObject();
        contentBuilder.startObject("body");

        contentBuilder.startObject("query");
        contentBuilder.startObject("script");
        contentBuilder.startObject("script");
        contentBuilder.field("inline", "return true");
        contentBuilder.endObject();
        contentBuilder.endObject();
        contentBuilder.endObject();

        contentBuilder.startObject("aggregations");
        contentBuilder.startObject("avg_grade");
        contentBuilder.startObject("avg");
        contentBuilder.startObject("script");
        contentBuilder.field("inline", "1 + 1");
        contentBuilder.endObject();
        contentBuilder.endObject();
        contentBuilder.endObject();
        contentBuilder.startObject("another_avg");
        contentBuilder.startObject("avg");
        contentBuilder.startObject("script");
        contentBuilder.field("inline", "1 + 2");
        contentBuilder.field("lang", "javascript");
        contentBuilder.endObject();
        contentBuilder.endObject();
        contentBuilder.endObject();
        contentBuilder.endObject();

        contentBuilder.endObject();
        contentBuilder.endObject();
        XContentParser parser = createParser(contentBuilder);
        parser.nextToken();

        SearchRequestParsers searchRequestParsers = new SearchModule(Settings.EMPTY, false, Collections.emptyList())
                .getSearchRequestParsers();
        WatcherSearchTemplateRequest result = WatcherSearchTemplateRequest.fromXContent(
                logger, parser, SearchType.DEFAULT, true, "your_legacy_lang", ParseFieldMatcher.STRICT, searchRequestParsers);
        Map<String, Object> parsedResult = XContentHelper.convertToMap(result.getSearchSource(), true).v2();
        // after upgrading the language must be equal to legacy language, because no language was defined explicitly in these scripts:
        assertThat(XContentMapValues.extractValue("query.script.script.lang", parsedResult), equalTo("your_legacy_lang"));
        assertThat(XContentMapValues.extractValue("aggregations.avg_grade.avg.script.lang", parsedResult), equalTo("your_legacy_lang"));
        // after upgrading the language must remain javascript here, because that has been explicitly defined in the script:
        assertThat(XContentMapValues.extractValue("aggregations.another_avg.avg.script.lang", parsedResult), equalTo("javascript"));
    }

    @Override
    protected NamedXContentRegistry xContentRegistry() {
        return new NamedXContentRegistry(new SearchModule(Settings.EMPTY, false, Collections.emptyList()).getNamedXContents());
    }
}
