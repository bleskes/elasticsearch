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

package org.elasticsearch.xpack.watcher.actions.pagerduty;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.notification.pagerduty.PagerDutyAccount;
import org.elasticsearch.xpack.notification.pagerduty.PagerDutyService;
import org.elasticsearch.xpack.watcher.support.text.TextTemplateEngine;
import org.junit.Before;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.xpack.watcher.actions.ActionBuilders.triggerPagerDutyAction;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class PagerDutyActionFactoryTests extends ESTestCase {

    private PagerDutyActionFactory factory;
    private PagerDutyService service;

    @Before
    public void init() throws Exception {
        service = mock(PagerDutyService.class);
        factory = new PagerDutyActionFactory(Settings.EMPTY, mock(TextTemplateEngine.class), service);
    }

    public void testParseAction() throws Exception {

        PagerDutyAccount account = mock(PagerDutyAccount.class);
        when(service.getAccount("_account1")).thenReturn(account);

        PagerDutyAction action = triggerPagerDutyAction("_account1", "_description").build();
        XContentBuilder jsonBuilder = jsonBuilder().value(action);
        XContentParser parser = JsonXContent.jsonXContent.createParser(jsonBuilder.bytes());
        parser.nextToken();

        PagerDutyAction parsedAction = factory.parseAction("_w1", "_a1", parser);
        assertThat(parsedAction, is(action));
    }

    public void testParseActionUnknownAccount() throws Exception {
        try {
            when(service.getAccount("_unknown")).thenReturn(null);

            PagerDutyAction action = triggerPagerDutyAction("_unknown", "_body").build();
            XContentBuilder jsonBuilder = jsonBuilder().value(action);
            XContentParser parser = JsonXContent.jsonXContent.createParser(jsonBuilder.bytes());
            parser.nextToken();
            factory.parseAction("_w1", "_a1", parser);
            fail("Expected ElasticsearchParseException due to unknown account");
        } catch (ElasticsearchParseException e) {}
    }
}
