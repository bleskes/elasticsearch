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

package org.elasticsearch.xpack.watcher.input;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ESTestCase;

import static java.util.Collections.emptyMap;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.containsString;

/**
 *
 */
public class InputRegistryTests extends ESTestCase {

    public void testParseEmptyInput() throws Exception {
        InputRegistry registry = new InputRegistry(emptyMap());
        XContentParser parser = JsonXContent.jsonXContent.createParser(
                jsonBuilder().startObject().endObject().bytes());
        parser.nextToken();
        try {
            registry.parse("_id", parser);
            fail("expecting an exception when trying to parse an empty input");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), containsString("expected field indicating the input type, but found an empty object instead"));
        }
    }

    public void testParseArrayInput() throws Exception {
        InputRegistry registry = new InputRegistry(emptyMap());
        XContentParser parser = JsonXContent.jsonXContent.createParser(
                jsonBuilder().startArray().endArray().bytes());
        parser.nextToken();
        try {
            registry.parse("_id", parser);
            fail("expecting an exception when trying to parse an input that is not an object");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), containsString("expected an object representing the input, but found [START_ARRAY] instead"));
        }
    }
}
