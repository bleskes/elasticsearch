/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.rest.schedulers;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.rest.FakeRestRequest;
import org.elasticsearch.xpack.prelert.action.StartJobSchedulerAction;

import java.util.Collections;

import static org.mockito.Mockito.mock;

public class RestStartJobSchedulerActionTests extends ESTestCase {

    public void testPrepareRequest() throws Exception {
        RestStartJobSchedulerAction action = new RestStartJobSchedulerAction(Settings.EMPTY, mock(RestController.class),
                mock(StartJobSchedulerAction.TransportAction.class));

        RestRequest restRequest1 = new FakeRestRequest.Builder().withParams(Collections.singletonMap("start", "not-a-date")).build();
        ElasticsearchParseException e =  expectThrows(ElasticsearchParseException.class,
                () -> action.prepareRequest(restRequest1, mock(NodeClient.class)));
        assertEquals("Query param 'start' with value 'not-a-date' cannot be parsed as a date or converted to a number (epoch).",
                e.getMessage());

        RestRequest restRequest2 = new FakeRestRequest.Builder().withParams(Collections.singletonMap("end", "not-a-date")).build();
        e =  expectThrows(ElasticsearchParseException.class, () -> action.prepareRequest(restRequest2, mock(NodeClient.class)));
        assertEquals("Query param 'end' with value 'not-a-date' cannot be parsed as a date or converted to a number (epoch).",
                e.getMessage());
    }

    public void testParseDateOrThrow() {
        assertEquals(0L, RestStartJobSchedulerAction.parseDateOrThrow("0", "start"));
        assertEquals(0L, RestStartJobSchedulerAction.parseDateOrThrow("1970-01-01T00:00:00Z", "start"));

        Exception e = expectThrows(ElasticsearchParseException.class,
                () -> RestStartJobSchedulerAction.parseDateOrThrow("not-a-date", "start"));
        assertEquals("Query param 'start' with value 'not-a-date' cannot be parsed as a date or converted to a number (epoch).",
                e.getMessage());
    }

}
