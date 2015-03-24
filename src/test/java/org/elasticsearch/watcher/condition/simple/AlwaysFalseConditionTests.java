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

package org.elasticsearch.watcher.condition.simple;

import org.elasticsearch.watcher.condition.Condition;
import org.elasticsearch.watcher.condition.ConditionException;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 */
public class AlwaysFalseConditionTests extends ElasticsearchTestCase {


    @Test
    public void testExecute() throws Exception {
        Condition alwaysTrue = new AlwaysTrueCondition(logger);
        assertTrue(alwaysTrue.execute(null).met());
    }

    @Test
    public void testParser_Valid() throws Exception {
        Condition.Parser p = new AlwaysTrueCondition.Parser(ImmutableSettings.settingsBuilder().build());
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.endObject();
        XContentParser xp = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        xp.nextToken();

        Condition alwaysTrue = p.parse(xp);
        assertTrue(alwaysTrue.execute(null).met());
    }

    @Test(expected = ConditionException.class)
    public void testParser_Invalid() throws Exception {
        Condition.Parser p = new AlwaysTrueCondition.Parser(ImmutableSettings.settingsBuilder().build());
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.field("foo", "bar");
        builder.endObject();
        XContentParser xp = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        xp.nextToken();

        p.parse(xp);
        fail("expected a condition exception trying to parse an invalid condition XContent, ["
                + AlwaysTrueCondition.TYPE + "] condition should not parse with a body");
    }


    @Test
    public void testResultParser_Valid() throws Exception {
        Condition.Parser p = new AlwaysTrueCondition.Parser(ImmutableSettings.settingsBuilder().build());
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.endObject();
        XContentParser xp = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        xp.nextToken();

        Condition.Result alwaysTrueResult = p.parseResult(xp);
        assertTrue(alwaysTrueResult.met());
    }

    @Test(expected = ConditionException.class)
    public void testResultParser_Invalid() throws Exception {
        Condition.Parser p = new AlwaysTrueCondition.Parser(ImmutableSettings.settingsBuilder().build());
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.field("met", false );
        builder.endObject();
        XContentParser xp = XContentFactory.xContent(builder.bytes()).createParser(builder.bytes());
        xp.nextToken();

        p.parseResult(xp);
        fail("expected a condition exception trying to parse an invalid condition result XContent, ["
                + AlwaysTrueCondition.TYPE + "] condition result should not parse with a [met] field");
    }
}
