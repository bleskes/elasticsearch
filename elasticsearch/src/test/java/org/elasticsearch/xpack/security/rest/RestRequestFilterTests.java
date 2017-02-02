/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.rest;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.rest.FakeRestRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class RestRequestFilterTests extends ESTestCase {

    public void testFilteringItemsInSubLevels() throws IOException {
        BytesReference content = new BytesArray("{\"root\": {\"second\": {\"third\": \"password\", \"foo\": \"bar\"}}}");
        RestRequestFilter filter = () -> Collections.singleton("root.second.third");
        FakeRestRequest restRequest =
                new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY).withContent(content, XContentType.JSON).build();
        RestRequest filtered = filter.getFilteredRequest(restRequest);
        assertNotEquals(content, filtered.content());

        Map<String, Object> map = XContentType.JSON.xContent().createParser(NamedXContentRegistry.EMPTY, filtered.content()).map();
        Map<String, Object> root = (Map<String, Object>) map.get("root");
        assertNotNull(root);
        Map<String, Object> second = (Map<String, Object>) root.get("second");
        assertNotNull(second);
        assertEquals("bar", second.get("foo"));
        assertNull(second.get("third"));
    }

    public void testFilteringItemsInSubLevelsWithWildCard() throws IOException {
        BytesReference content = new BytesArray("{\"root\": {\"second\": {\"third\": \"password\", \"foo\": \"bar\"}}}");
        RestRequestFilter filter = () -> Collections.singleton("root.*.third");
        FakeRestRequest restRequest =
                new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY).withContent(content, XContentType.JSON).build();
        RestRequest filtered = filter.getFilteredRequest(restRequest);
        assertNotEquals(content, filtered.content());

        Map<String, Object> map = XContentType.JSON.xContent().createParser(NamedXContentRegistry.EMPTY, filtered.content()).map();
        Map<String, Object> root = (Map<String, Object>) map.get("root");
        assertNotNull(root);
        Map<String, Object> second = (Map<String, Object>) root.get("second");
        assertNotNull(second);
        assertEquals("bar", second.get("foo"));
        assertNull(second.get("third"));
    }

    public void testFilteringItemsInSubLevelsWithLeadingWildCard() throws IOException {
        BytesReference content = new BytesArray("{\"root\": {\"second\": {\"third\": \"password\", \"foo\": \"bar\"}}}");
        RestRequestFilter filter = () -> Collections.singleton("*.third");
        FakeRestRequest restRequest =
                new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY).withContent(content, XContentType.JSON).build();
        RestRequest filtered = filter.getFilteredRequest(restRequest);
        assertNotEquals(content, filtered.content());

        Map<String, Object> map = XContentType.JSON.xContent().createParser(NamedXContentRegistry.EMPTY, filtered.content()).map();
        Map<String, Object> root = (Map<String, Object>) map.get("root");
        assertNotNull(root);
        Map<String, Object> second = (Map<String, Object>) root.get("second");
        assertNotNull(second);
        assertEquals("bar", second.get("foo"));
        assertNull(second.get("third"));
    }
}
