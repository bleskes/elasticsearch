/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.action.user;

import java.nio.charset.StandardCharsets;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthAction;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.security.authz.RoleDescriptor;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

public class HasPrivilegesRequestBuilderTests extends ESTestCase {

    public void testParseValidJsonWithClusterAndIndexPrivileges() throws Exception {
        String json = "{ "
                + " \"cluster\":[ \"all\"],"
                + " \"index\":[ "
                + " { \"names\": [ \".kibana\", \".reporting\" ], "
                + "   \"privileges\" : [ \"read\", \"write\" ] }, "
                + " { \"names\": [ \".security\" ], "
                + "   \"privileges\" : [ \"manage\" ] } "
                + " ]"
                + "}";

        final HasPrivilegesRequestBuilder builder = new HasPrivilegesRequestBuilder(mock(Client.class));
        builder.source("elastic", new BytesArray(json.getBytes(StandardCharsets.UTF_8)), XContentType.JSON);

        final HasPrivilegesRequest request = builder.request();
        assertThat(request.clusterPrivileges().length, equalTo(1));
        assertThat(request.clusterPrivileges()[0], equalTo("all"));

        assertThat(request.indexPrivileges().length, equalTo(2));

        final RoleDescriptor.IndicesPrivileges privileges0 = request.indexPrivileges()[0];
        assertThat(privileges0.getIndices(), arrayContaining(".kibana", ".reporting"));
        assertThat(privileges0.getPrivileges(), arrayContaining("read", "write"));

        final RoleDescriptor.IndicesPrivileges privileges1 = request.indexPrivileges()[1];
        assertThat(privileges1.getIndices(), arrayContaining(".security"));
        assertThat(privileges1.getPrivileges(), arrayContaining("manage"));
    }

    public void testParseValidJsonWithJustIndexPrivileges() throws Exception {
        String json = "{ \"index\":[ "
                + "{ \"names\": [ \".kibana\", \".reporting\" ], "
                + " \"privileges\" : [ \"read\", \"write\" ] }, "
                + "{ \"names\": [ \".security\" ], "
                + " \"privileges\" : [ \"manage\" ] } "
                + "] }";

        final HasPrivilegesRequestBuilder builder = new HasPrivilegesRequestBuilder(mock(Client.class));
        builder.source("elastic", new BytesArray(json.getBytes(StandardCharsets.UTF_8)), XContentType.JSON);

        final HasPrivilegesRequest request = builder.request();
        assertThat(request.clusterPrivileges().length, equalTo(0));
        assertThat(request.indexPrivileges().length, equalTo(2));

        final RoleDescriptor.IndicesPrivileges privileges0 = request.indexPrivileges()[0];
        assertThat(privileges0.getIndices(), arrayContaining(".kibana", ".reporting"));
        assertThat(privileges0.getPrivileges(), arrayContaining("read", "write"));

        final RoleDescriptor.IndicesPrivileges privileges1 = request.indexPrivileges()[1];
        assertThat(privileges1.getIndices(), arrayContaining(".security"));
        assertThat(privileges1.getPrivileges(), arrayContaining("manage"));
    }

    public void testParseValidJsonWithJustClusterPrivileges() throws Exception {
        String json = "{ \"cluster\":[ "
                + "\"manage\","
                + "\"" + ClusterHealthAction.NAME + "\","
                + "\"" + ClusterStatsAction.NAME + "\""
                + "] }";

        final HasPrivilegesRequestBuilder builder = new HasPrivilegesRequestBuilder(mock(Client.class));
        builder.source("elastic", new BytesArray(json.getBytes(StandardCharsets.UTF_8)), XContentType.JSON);

        final HasPrivilegesRequest request = builder.request();
        assertThat(request.indexPrivileges().length, equalTo(0));
        assertThat(request.clusterPrivileges(), arrayContaining("manage", ClusterHealthAction.NAME, ClusterStatsAction.NAME));
    }

    public void testUseOfFieldLevelSecurityThrowsException() throws Exception {
        String json = "{ \"index\":[ "
                + "{"
                + " \"names\": [ \"employees\" ], "
                + " \"privileges\" : [ \"read\", \"write\" ] ,"
                + " \"field_security\": { \"grant\": [ \"name\", \"department\", \"title\" ] }"
                + "} ] }";

        final HasPrivilegesRequestBuilder builder = new HasPrivilegesRequestBuilder(mock(Client.class));
        final ElasticsearchParseException parseException = expectThrows(ElasticsearchParseException.class,
                () -> builder.source("elastic", new BytesArray(json.getBytes(StandardCharsets.UTF_8)), XContentType.JSON)
        );
        assertThat(parseException.getMessage(), containsString("[field_security]"));
    }

    public void testMissingPrivilegesThrowsException() throws Exception {
        String json = "{ }";
        final HasPrivilegesRequestBuilder builder = new HasPrivilegesRequestBuilder(mock(Client.class));
        final ElasticsearchParseException parseException = expectThrows(ElasticsearchParseException.class,
                () -> builder.source("elastic", new BytesArray(json.getBytes(StandardCharsets.UTF_8)), XContentType.JSON)
        );
        assertThat(parseException.getMessage(), containsString("[index] and [cluster] are both missing"));
    }
}