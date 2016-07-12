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

package org.elasticsearch.xpack.watcher.test.integration;

import org.apache.http.Header;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.Response;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.NettyPlugin;
import org.elasticsearch.xpack.monitoring.test.MonitoringIntegTestCase;
import org.elasticsearch.xpack.security.authc.support.SecuredString;
import org.elasticsearch.xpack.watcher.test.AbstractWatcherIntegrationTestCase;
import org.junit.After;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken.BASIC_AUTH_HEADER;
import static org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken.basicAuthHeaderValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;

public class WatcherSettingsFilterTests extends AbstractWatcherIntegrationTestCase {
    private CloseableHttpClient httpClient = HttpClients.createDefault();

    @After
    public void cleanup() throws IOException {
        httpClient.close();
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(NetworkModule.HTTP_ENABLED.getKey(), true)
                .put("xpack.notification.email.account._email.smtp.host", "host.domain")
                .put("xpack.notification.email.account._email.smtp.port", 587)
                .put("xpack.notification.email.account._email.smtp.user", "_user")
                .put("xpack.notification.email.account._email.smtp.password", "_passwd")
                .build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        ArrayList<Class<? extends Plugin>> plugins = new ArrayList<>(super.nodePlugins());
        plugins.add(NettyPlugin.class); // for http
        return plugins;
    }

    public void testGetSettingsSmtpPassword() throws Exception {
        Header[] headers;
        if (securityEnabled()) {
            headers = new Header[] {
                    new BasicHeader(BASIC_AUTH_HEADER,
                            basicAuthHeaderValue(MonitoringIntegTestCase.SecuritySettings.TEST_USERNAME,
                                    new SecuredString(MonitoringIntegTestCase.SecuritySettings.TEST_PASSWORD.toCharArray())))};
        } else {
            headers = new Header[0];
        }
        try (Response response = getRestClient().performRequest("GET", "/_nodes/settings",
                Collections.emptyMap(), null, headers)) {
            Map<String, Object> responseMap = JsonXContent.jsonXContent.createParser(response.getEntity().getContent()).map();
            Map<String, Object> nodes = (Map<String, Object>) responseMap.get("nodes");
            for (Object node : nodes.values()) {
                Map<String, Object> settings = (Map<String, Object>) ((Map<String, Object>) node).get("settings");
                assertThat(XContentMapValues.extractValue("xpack.notification.email.account._email.smtp.user", settings),
                        is((Object) "_user"));
                assertThat(XContentMapValues.extractValue("xpack.notification.email.account._email.smtp.password", settings),
                        nullValue());
            }
        }
    }
}
