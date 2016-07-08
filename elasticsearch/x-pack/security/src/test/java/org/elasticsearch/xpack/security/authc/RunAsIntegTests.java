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

package org.elasticsearch.xpack.security.authc;

import org.apache.http.message.BasicHeader;
import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.xpack.security.Security;
import org.elasticsearch.xpack.security.authc.support.SecuredString;
import org.elasticsearch.xpack.security.authc.support.SecuredStringTests;
import org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken;
import org.elasticsearch.xpack.security.transport.netty.SecurityNettyTransport;
import org.elasticsearch.test.SecurityIntegTestCase;
import org.elasticsearch.test.SecuritySettingsSource;
import org.elasticsearch.xpack.XPackPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 *
 */
public class RunAsIntegTests extends SecurityIntegTestCase {
    static final String RUN_AS_USER = "run_as_user";
    static final String TRANSPORT_CLIENT_USER = "transport_user";
    static final String ROLES =
            "transport_client:\n" +
            "  cluster: [ 'cluster:monitor/nodes/liveness' ]\n" +
            "run_as_role:\n" +
            "  run_as: [ '" + SecuritySettingsSource.DEFAULT_USER_NAME + "', 'idontexist' ]\n";

    @Override
    public Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(NetworkModule.HTTP_ENABLED.getKey(), true)
                .build();
    }

    @Override
    public boolean sslTransportEnabled() {
        return false;
    }

    @Override
    public String configRoles() {
        return ROLES + super.configRoles();
    }

    @Override
    public String configUsers() {
        return super.configUsers()
                + RUN_AS_USER + ":" + SecuritySettingsSource.DEFAULT_PASSWORD_HASHED + "\n"
                + TRANSPORT_CLIENT_USER + ":" + SecuritySettingsSource.DEFAULT_PASSWORD_HASHED + "\n";
    }

    @Override
    public String configUsersRoles() {
        return super.configUsersRoles()
                + "run_as_role:" + RUN_AS_USER + "\n"
                + "transport_client:" + TRANSPORT_CLIENT_USER;
    }

    public void testUserImpersonation() throws Exception {
        try (TransportClient client = getTransportClient(Settings.builder()
                .put(Security.USER_SETTING.getKey(), TRANSPORT_CLIENT_USER + ":" + SecuritySettingsSource.DEFAULT_PASSWORD).build())) {
            //ensure the client can connect
            awaitBusy(() -> {
                return client.connectedNodes().size() > 0;
            });

            // make sure the client can't get health
            try {
                client.admin().cluster().prepareHealth().get();
                fail("the client user should not have privileges to get the health");
            } catch (ElasticsearchSecurityException e) {
                assertThat(e.getMessage(), containsString("unauthorized"));
            }

            // let's run as without authorization
            try {
                Map<String, String> headers = Collections.singletonMap(InternalAuthenticationService.RUN_AS_USER_HEADER,
                        SecuritySettingsSource.DEFAULT_USER_NAME);
                client.filterWithHeader(headers)
                        .admin().cluster().prepareHealth().get();
                fail("run as should be unauthorized for the transport client user");
            } catch (ElasticsearchSecurityException e) {
                assertThat(e.getMessage(), containsString("unauthorized"));
                assertThat(e.getMessage(), containsString("run as"));
            }

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", UsernamePasswordToken.basicAuthHeaderValue(RUN_AS_USER,
                    new SecuredString(SecuritySettingsSource.DEFAULT_PASSWORD.toCharArray())));
            headers.put(InternalAuthenticationService.RUN_AS_USER_HEADER, SecuritySettingsSource.DEFAULT_USER_NAME);
            // lets set the user
            ClusterHealthResponse response = client.filterWithHeader(headers).admin().cluster().prepareHealth().get();
            assertThat(response.isTimedOut(), is(false));
        }
    }

    public void testUserImpersonationUsingHttp() throws Exception {
        // use the transport client user and try to run as
        try {
            getRestClient().performRequest("GET", "/_nodes",
                    new BasicHeader(UsernamePasswordToken.BASIC_AUTH_HEADER,
                            UsernamePasswordToken.basicAuthHeaderValue(TRANSPORT_CLIENT_USER,
                                    SecuredStringTests.build(SecuritySettingsSource.DEFAULT_PASSWORD))),
                    new BasicHeader(InternalAuthenticationService.RUN_AS_USER_HEADER, SecuritySettingsSource.DEFAULT_USER_NAME));
            fail("request should have failed");
        } catch(ResponseException e) {
            assertThat(e.getResponse().getStatusLine().getStatusCode(), is(403));
        }

        try {
            //the run as user shouldn't have access to the nodes api
            getRestClient().performRequest("GET", "/_nodes",
                    new BasicHeader(UsernamePasswordToken.BASIC_AUTH_HEADER,
                            UsernamePasswordToken.basicAuthHeaderValue(RUN_AS_USER,
                                    SecuredStringTests.build(SecuritySettingsSource.DEFAULT_PASSWORD))));
            fail("request should have failed");
        } catch(ResponseException e) {
            assertThat(e.getResponse().getStatusLine().getStatusCode(), is(403));
        }

        // but when running as a different user it should work
        try (Response response = getRestClient().performRequest("GET", "/_nodes",
                new BasicHeader(UsernamePasswordToken.BASIC_AUTH_HEADER,
                        UsernamePasswordToken.basicAuthHeaderValue(RUN_AS_USER,
                                SecuredStringTests.build(SecuritySettingsSource.DEFAULT_PASSWORD))),
                new BasicHeader(InternalAuthenticationService.RUN_AS_USER_HEADER, SecuritySettingsSource.DEFAULT_USER_NAME))) {
            assertThat(response.getStatusLine().getStatusCode(), is(200));
        }
    }

    public void testEmptyUserImpersonationHeader() throws Exception {
        try (TransportClient client = getTransportClient(Settings.builder()
                .put(Security.USER_SETTING.getKey(), TRANSPORT_CLIENT_USER + ":" + SecuritySettingsSource.DEFAULT_PASSWORD).build())) {
            //ensure the client can connect
            awaitBusy(() -> {
                return client.connectedNodes().size() > 0;
            });

            try {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", UsernamePasswordToken.basicAuthHeaderValue(RUN_AS_USER,
                        new SecuredString(SecuritySettingsSource.DEFAULT_PASSWORD.toCharArray())));
                headers.put(InternalAuthenticationService.RUN_AS_USER_HEADER, "");

                client.filterWithHeader(headers).admin().cluster().prepareHealth().get();
                fail("run as header should not be allowed to be empty");
            } catch (ElasticsearchSecurityException e) {
                assertThat(e.getMessage(), containsString("unable to authenticate"));
            }
        }
    }

    public void testEmptyHeaderUsingHttp() throws Exception {
        try {
            getRestClient().performRequest("GET", "/_nodes",
                    new BasicHeader(UsernamePasswordToken.BASIC_AUTH_HEADER,
                            UsernamePasswordToken.basicAuthHeaderValue(RUN_AS_USER,
                            SecuredStringTests.build(SecuritySettingsSource.DEFAULT_PASSWORD))),
                    new BasicHeader(InternalAuthenticationService.RUN_AS_USER_HEADER, ""));
            fail("request should have failed");
        } catch(ResponseException e) {
            assertThat(e.getResponse().getStatusLine().getStatusCode(), is(401));
        }
    }

    public void testNonExistentRunAsUser() throws Exception {
        try (TransportClient client = getTransportClient(Settings.builder()
                .put(Security.USER_SETTING.getKey(), TRANSPORT_CLIENT_USER + ":" + SecuritySettingsSource.DEFAULT_PASSWORD).build())) {
            //ensure the client can connect
            awaitBusy(() -> {
                return client.connectedNodes().size() > 0;
            });

            try {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", UsernamePasswordToken.basicAuthHeaderValue(RUN_AS_USER,
                        new SecuredString(SecuritySettingsSource.DEFAULT_PASSWORD.toCharArray())));
                headers.put(InternalAuthenticationService.RUN_AS_USER_HEADER, "idontexist");

                client.filterWithHeader(headers).admin().cluster().prepareHealth().get();
                fail("run as header should not accept non-existent users");
            } catch (ElasticsearchSecurityException e) {
                assertThat(e.getMessage(), containsString("unauthorized"));
            }
        }
    }

    public void testNonExistentRunAsUserUsingHttp() throws Exception {
        try {
            getRestClient().performRequest("GET", "/_nodes",
                    new BasicHeader(UsernamePasswordToken.BASIC_AUTH_HEADER,
                            UsernamePasswordToken.basicAuthHeaderValue(RUN_AS_USER,
                            SecuredStringTests.build(SecuritySettingsSource.DEFAULT_PASSWORD))),
                    new BasicHeader(InternalAuthenticationService.RUN_AS_USER_HEADER, "idontexist"));
            fail("request should have failed");
        } catch (ResponseException e) {
            assertThat(e.getResponse().getStatusLine().getStatusCode(), is(403));
        }
    }

    // build our own here to better mimic an actual client...
    TransportClient getTransportClient(Settings extraSettings) {
        NodesInfoResponse nodeInfos = client().admin().cluster().prepareNodesInfo().get();
        List<NodeInfo> nodes = nodeInfos.getNodes();
        assertTrue(nodes.isEmpty() == false);
        TransportAddress publishAddress = randomFrom(nodes).getTransport().address().publishAddress();
        String clusterName = nodeInfos.getClusterName().value();

        Settings settings = Settings.builder()
                .put(extraSettings)
                .put("cluster.name", clusterName)
                .put(SecurityNettyTransport.SSL_SETTING.getKey(), false)
                .build();

        return TransportClient.builder()
                .settings(settings)
                .addPlugin(XPackPlugin.class)
                .build()
                .addTransportAddress(publishAddress);
    }
}
