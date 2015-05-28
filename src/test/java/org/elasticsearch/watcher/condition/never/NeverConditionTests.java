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

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.watcher.condition.ConditionFactory;
import org.elasticsearch.watcher.condition.ExecutableCondition;
import org.elasticsearch.watcher.condition.always.AlwaysCondition;
import org.junit.Test;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 */
public class NeverConditionTests extends ElasticsearchTestCase {

    @Test
    public void testExecute() throws Exception {
        ExecutableCondition executable = new ExecutableNeverCondition(logger);
        assertFalse(executable.execute(null).met());
    }

    @Test
    public void testParser_Valid() throws Exception {
        NeverConditionFactory factory = new NeverConditionFactory(ImmutableSettings.settingsBuilder().build());
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.endObject();
        XContentParser parser = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        parser.nextToken();

        NeverCondition condition = factory.parseCondition("_id", parser);
        ExecutableNeverCondition executable = factory.createExecutable(condition);
        assertFalse(executable.execute(null).met());
    }

    @Test(expected = NeverConditionException.class)
    public void testParser_Invalid() throws Exception {
        ConditionFactory factory = new NeverConditionFactory(ImmutableSettings.settingsBuilder().build());
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.field("foo", "bar");
        builder.endObject();
        XContentParser parser = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        parser.nextToken();

        factory.parseCondition("_id", parser);
        fail("expected a condition exception trying to parse an invalid condition XContent, ["
                + AlwaysCondition.TYPE + "] condition should not parse with a body");
    }

}
