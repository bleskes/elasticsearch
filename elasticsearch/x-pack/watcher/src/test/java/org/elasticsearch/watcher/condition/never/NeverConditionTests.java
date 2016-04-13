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

package org.elasticsearch.watcher.condition.never;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.watcher.condition.ConditionFactory;
import org.elasticsearch.watcher.condition.ExecutableCondition;
import org.elasticsearch.watcher.condition.always.AlwaysCondition;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.containsString;

/**
 */
public class NeverConditionTests extends ESTestCase {
    public void testExecute() throws Exception {
        ExecutableCondition executable = new ExecutableNeverCondition(logger);
        assertFalse(executable.execute(null).met());
    }

    public void testParserValid() throws Exception {
        NeverConditionFactory factory = new NeverConditionFactory(Settings.builder().build());
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.endObject();
        XContentParser parser = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        parser.nextToken();

        NeverCondition condition = factory.parseCondition("_id", parser);
        ExecutableNeverCondition executable = factory.createExecutable(condition);
        assertFalse(executable.execute(null).met());
    }

    public void testParserInvalid() throws Exception {
        ConditionFactory factory = new NeverConditionFactory(Settings.builder().build());
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.field("foo", "bar");
        builder.endObject();
        XContentParser parser = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        parser.nextToken();
        try {
            factory.parseCondition("_id", parser);
            fail("expected a condition exception trying to parse an invalid condition XContent, ["
                    + AlwaysCondition.TYPE + "] condition should not parse with a body");
        } catch (ElasticsearchParseException e) {
            assertThat(e.getMessage(), containsString("expected an empty object but found [foo]"));
        }
    }
}
