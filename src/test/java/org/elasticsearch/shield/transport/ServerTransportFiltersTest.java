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

package org.elasticsearch.shield.transport;

import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.hamcrest.Matchers.instanceOf;

public class ServerTransportFiltersTest extends ElasticsearchTestCase {

    private Map<String, ServerTransportFilter> filters = Maps.newHashMap();

    public class TestAuthenticateTransportFilter extends ServerTransportFilter.TransportClient {}
    public class TestRejectInternalActionsTransportFilter extends ServerTransportFilter.TransportClient {}

    @Before
    public void setup() {
        filters.put(ServerTransportFilters.SERVER_TRANSPORT_FILTER_TRANSPORT_CLIENT, new ServerTransportFilter.TransportClient());
        filters.put(ServerTransportFilters.SERVER_TRANSPORT_FILTER_AUTHENTICATE_REJECT_INTERNAL_ACTIONS, new TestRejectInternalActionsTransportFilter());
        filters.put(ServerTransportFilters.SERVER_TRANSPORT_FILTER_AUTHENTICATE_ONLY, new TestAuthenticateTransportFilter());
    }

    @Test
    public void test() {
        Settings settings = settingsBuilder()
                .put("transport.profiles.default.shield.type", "client")
                .put("transport.profiles.alternative.shield.type", "server")
                .build();

        ServerTransportFilters serverTransportFilters = new ServerTransportFilters(settings, filters);

        // default filter is returned by default
        ServerTransportFilter expectedClientFilter = serverTransportFilters.getTransportFilterForProfile("default");
        assertThat(expectedClientFilter, instanceOf(TestRejectInternalActionsTransportFilter.class));

        ServerTransportFilter expectedDummyFilter = serverTransportFilters.getTransportFilterForProfile("alternative");
        assertThat(expectedDummyFilter, instanceOf(TestAuthenticateTransportFilter.class));
    }

    @Test
    public void testThatExceptionIsThrownForUnknownProfile() {
        ServerTransportFilters serverTransportFilters = new ServerTransportFilters(settingsBuilder().build(), filters);
        assertThat(serverTransportFilters.getTransportFilterForProfile("unknown"), instanceOf(TestAuthenticateTransportFilter.class));
    }

    @Test
    public void testThatClientFilterIsReturnedOnClientNodes() {
        Settings settings = settingsBuilder()
                .put("node.client", true)
                .put("client.type", "transport")
                .build();

        ServerTransportFilters serverTransportFilters = new ServerTransportFilters(settings, filters);

        // no matter the profile, client node means client filter
        ServerTransportFilter expectedDummyFilter = serverTransportFilters.getTransportFilterForProfile("a");
        assertThat(expectedDummyFilter, instanceOf(ServerTransportFilter.TransportClient.class));
        expectedDummyFilter = serverTransportFilters.getTransportFilterForProfile("b");
        assertThat(expectedDummyFilter, instanceOf(ServerTransportFilter.TransportClient.class));
        expectedDummyFilter = serverTransportFilters.getTransportFilterForProfile("c");
        assertThat(expectedDummyFilter, instanceOf(ServerTransportFilter.TransportClient.class));
    }

}
