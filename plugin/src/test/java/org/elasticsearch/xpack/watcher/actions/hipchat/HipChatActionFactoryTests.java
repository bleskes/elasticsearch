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

package org.elasticsearch.xpack.watcher.actions.hipchat;

import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.notification.hipchat.HipChatAccount;
import org.elasticsearch.xpack.notification.hipchat.HipChatService;
import org.junit.Before;

import java.util.Collections;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.xpack.watcher.actions.ActionBuilders.hipchatAction;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HipChatActionFactoryTests extends ESTestCase {
    private HipChatActionFactory factory;
    private HipChatService hipchatService;

    @Before
    public void init() throws Exception {
        hipchatService = mock(HipChatService.class);
        factory = new HipChatActionFactory(Settings.EMPTY, mock(TextTemplateEngine.class), hipchatService);
    }

    public void testParseAction() throws Exception {
        HipChatAccount account = mock(HipChatAccount.class);
        when(hipchatService.getAccount("_account1")).thenReturn(account);

        HipChatAction action = hipchatAction("_account1", "_body").build();
        XContentBuilder jsonBuilder = jsonBuilder().value(action);
        XContentParser parser = createParser(jsonBuilder);
        parser.nextToken();

        ExecutableHipChatAction parsedAction = factory.parseExecutable("_w1", "_a1", parser);
        assertThat(parsedAction.action(), is(action));

        verify(account, times(1)).validateParsedTemplate("_w1", "_a1", action.message);
    }

    public void testParseActionUnknownAccount() throws Exception {
        hipchatService = new HipChatService(Settings.EMPTY, null, new ClusterSettings(Settings.EMPTY,
                Collections.singleton(HipChatService.HIPCHAT_ACCOUNT_SETTING)));
        factory = new HipChatActionFactory(Settings.EMPTY, mock(TextTemplateEngine.class), hipchatService);
        HipChatAction action = hipchatAction("_unknown", "_body").build();
        XContentBuilder jsonBuilder = jsonBuilder().value(action);
        XContentParser parser = createParser(jsonBuilder);
        parser.nextToken();
        expectThrows(IllegalArgumentException.class, () -> factory.parseExecutable("_w1", "_a1", parser));
    }
}
