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

package org.elasticsearch.watcher.actions.slack;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.watcher.actions.slack.service.SlackAccount;
import org.elasticsearch.watcher.actions.slack.service.SlackService;
import org.elasticsearch.watcher.support.text.TextTemplateEngine;
import org.junit.Before;
import org.junit.Test;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.watcher.actions.ActionBuilders.slackAction;
import static org.elasticsearch.watcher.actions.slack.service.message.SlackMessageTests.createRandomTemplate;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class SlackActionFactoryTests extends ESTestCase {

    private SlackActionFactory factory;
    private SlackService service;

    @Before
    public void init() throws Exception {
        service = mock(SlackService.class);
        factory = new SlackActionFactory(Settings.EMPTY, mock(TextTemplateEngine.class), service);
    }

    @Test
    public void testParseAction() throws Exception {

        SlackAccount account = mock(SlackAccount.class);
        when(service.getAccount("_account1")).thenReturn(account);

        SlackAction action = slackAction("_account1", createRandomTemplate()).build();
        XContentBuilder jsonBuilder = jsonBuilder().value(action);
        XContentParser parser = JsonXContent.jsonXContent.createParser(jsonBuilder.bytes());
        parser.nextToken();

        SlackAction parsedAction = factory.parseAction("_w1", "_a1", parser);
        assertThat(parsedAction, is(action));
    }

    @Test(expected = ElasticsearchParseException.class)
    public void testParseAction_UnknownAccount() throws Exception {

        when(service.getAccount("_unknown")).thenReturn(null);

        SlackAction action = slackAction("_unknown", createRandomTemplate()).build();
        XContentBuilder jsonBuilder = jsonBuilder().value(action);
        XContentParser parser = JsonXContent.jsonXContent.createParser(jsonBuilder.bytes());
        parser.nextToken();
        factory.parseAction("_w1", "_a1", parser);
    }
}
