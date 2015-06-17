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

package org.elasticsearch.watcher.input;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 *
 */
public class InputRegistryTests extends ElasticsearchTestCase {

    @Test(expected = InputException.class)
    public void testParse_EmptyInput() throws Exception {
        InputRegistry registry = new InputRegistry(ImmutableMap.<String, InputFactory>of());
        XContentParser parser = JsonXContent.jsonXContent.createParser(
                jsonBuilder().startObject().endObject().bytes());
        parser.nextToken();
        registry.parse("_id", parser);
        fail("expecting an exception when trying to parse an empty input");
    }

    @Test(expected = InputException.class)
    public void testParse_ArrayInput() throws Exception {
        InputRegistry registry = new InputRegistry(ImmutableMap.<String, InputFactory>of());
        XContentParser parser = JsonXContent.jsonXContent.createParser(
                jsonBuilder().startArray().endArray().bytes());
        parser.nextToken();
        registry.parse("_id", parser);
        fail("expecting an exception when trying to parse an input that is not an object");
    }
}
