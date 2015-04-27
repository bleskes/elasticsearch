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

package org.elasticsearch.watcher.condition.always;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.watcher.condition.Condition;
import org.elasticsearch.watcher.condition.ConditionFactory;
import org.elasticsearch.watcher.condition.ExecutableCondition;
import org.junit.Test;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 */
public class AlwaysConditionTests extends ElasticsearchTestCase {

    @Test
    public void testExecute() throws Exception {
        ExecutableCondition alwaysTrue = new ExecutableAlwaysCondition(logger);
        assertTrue(alwaysTrue.execute(null).met());
    }

    @Test
    public void testParser_Valid() throws Exception {
        AlwaysConditionFactory factory = new AlwaysConditionFactory(ImmutableSettings.settingsBuilder().build());
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.endObject();
        XContentParser parser = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        parser.nextToken();
        AlwaysCondition condition = factory.parseCondition("_id", parser);
        ExecutableAlwaysCondition executable = factory.createExecutable(condition);
        assertTrue(executable.execute(null).met());
    }

    @Test(expected = AlwaysConditionException.class)
    public void testParser_Invalid() throws Exception {
        ConditionFactory factor = new AlwaysConditionFactory(ImmutableSettings.settingsBuilder().build());
        XContentBuilder builder = jsonBuilder()
                .startObject()
                .field("foo", "bar")
                .endObject();
        XContentParser parser = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        parser.nextToken();
        factor.parseCondition("_id", parser);
        fail("expected a condition exception trying to parse an invalid condition XContent, ["
                + AlwaysCondition.TYPE + "] condition should not parse with a body");
    }


    @Test
    public void testResultParser_Valid() throws Exception {
        ConditionFactory factory = new AlwaysConditionFactory(ImmutableSettings.settingsBuilder().build());
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.endObject();
        XContentParser parser = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        parser.nextToken();

        Condition.Result alwaysTrueResult = factory.parseResult("_id", parser);
        assertTrue(alwaysTrueResult.met());
    }

    @Test(expected = AlwaysConditionException.class)
    public void testResultParser_Invalid() throws Exception {
        ConditionFactory factory = new AlwaysConditionFactory(ImmutableSettings.settingsBuilder().build());
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.field("met", false);
        builder.endObject();
        XContentParser parser = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        parser.nextToken();

        factory.parseResult("_id", parser);
        fail("expected a condition exception trying to parse an invalid condition result XContent, ["
                + AlwaysCondition.TYPE + "] condition result should not parse with a [met] field");
    }

}
