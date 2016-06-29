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

package org.elasticsearch.xpack.watcher.rest.action;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.rest.FakeRestRequest;
import org.elasticsearch.xpack.watcher.client.WatcherClient;
import org.elasticsearch.xpack.watcher.transport.actions.execute.ExecuteWatchRequestBuilder;
import org.elasticsearch.xpack.watcher.trigger.TriggerService;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestExecuteWatchActionTests extends ESTestCase {

    private RestController restController = mock(RestController.class);
    private Client client = mock(Client.class);
    private TriggerService triggerService = mock(TriggerService.class);
    private RestChannel restChannel = mock(RestChannel.class);
    private WatcherClient watcherClient = mock(WatcherClient.class);

    public void testThatFlagsCanBeSpecifiedViaParameters() throws Exception {
        String randomId = randomAsciiOfLength(10);
        for (String recordExecution : Arrays.asList("true", "false", null)) {
            for (String ignoreCondition : Arrays.asList("true", "false", null)) {
                for (String debugCondition : Arrays.asList("true", "false", null)) {
                    ExecuteWatchRequestBuilder builder = new ExecuteWatchRequestBuilder(client);
                    when(watcherClient.prepareExecuteWatch()).thenReturn(builder);

                    RestExecuteWatchAction restExecuteWatchAction = new RestExecuteWatchAction(Settings.EMPTY, restController,
                            triggerService);
                    restExecuteWatchAction.handleRequest(createFakeRestRequest(randomId, recordExecution, ignoreCondition,
                            debugCondition), restChannel, watcherClient);

                    assertThat(builder.request().getId(), is(randomId));
                    assertThat(builder.request().isRecordExecution(), is(Boolean.parseBoolean(recordExecution)));
                    assertThat(builder.request().isIgnoreCondition(), is(Boolean.parseBoolean(ignoreCondition)));
                    assertThat(builder.request().isDebug(), is(Boolean.parseBoolean(debugCondition)));
                }
            }
        }
    }

    private FakeRestRequest createFakeRestRequest(String randomId, String recordExecution, String ignoreCondition, String debugCondition) {
        FakeRestRequest restRequest = new FakeRestRequest() {
            @Override
            public boolean hasContent() {
                return true;
            }

            @Override
            public BytesReference content() {
                return new BytesArray("{}");
            }
        };

        restRequest.params().put("id", randomId);
        // make sure we test true/false/no params
        if (recordExecution != null) restRequest.params().put("record_execution", recordExecution);
        if (ignoreCondition != null) restRequest.params().put("ignore_condition", ignoreCondition);
        if (debugCondition != null) restRequest.params().put("debug", debugCondition);

        return restRequest;
    }
}
