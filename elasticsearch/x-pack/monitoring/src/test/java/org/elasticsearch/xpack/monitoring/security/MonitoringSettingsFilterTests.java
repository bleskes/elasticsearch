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

package org.elasticsearch.xpack.monitoring.security;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.Response;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.xpack.MockNetty3Plugin;
import org.elasticsearch.xpack.monitoring.MonitoringSettings;
import org.elasticsearch.xpack.monitoring.test.MonitoringIntegTestCase;
import org.elasticsearch.xpack.security.authc.support.SecuredString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.elasticsearch.common.xcontent.support.XContentMapValues.extractValue;
import static org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken.BASIC_AUTH_HEADER;
import static org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken.basicAuthHeaderValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class MonitoringSettingsFilterTests extends MonitoringIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(NetworkModule.HTTP_ENABLED.getKey(), true)
                .put(MonitoringSettings.INTERVAL.getKey(), "-1")
                .put("xpack.monitoring.collection.exporters._http.type", "http")
                .put("xpack.monitoring.collection.exporters._http.enabled", false)
                .put("xpack.monitoring.collection.exporters._http.auth.username", "_user")
                .put("xpack.monitoring.collection.exporters._http.auth.password", "_passwd")
                .put("xpack.monitoring.collection.exporters._http.ssl.truststore.path", "/path/to/truststore")
                .put("xpack.monitoring.collection.exporters._http.ssl.truststore.password", "_passwd")
                .put("xpack.monitoring.collection.exporters._http.ssl.hostname_verification", true)
                .build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        ArrayList<Class<? extends Plugin>> plugins = new ArrayList<>(super.nodePlugins());
        plugins.add(MockNetty3Plugin.class); // for http
        return plugins;
    }

    public void testGetSettingsFiltered() throws Exception {
        Header[] headers;
        if (securityEnabled) {
            headers = new Header[] {
                    new BasicHeader(BASIC_AUTH_HEADER,
                            basicAuthHeaderValue(SecuritySettings.TEST_USERNAME,
                                    new SecuredString(SecuritySettings.TEST_PASSWORD.toCharArray())))};
        } else {
            headers = new Header[0];
        }
        Response response = getRestClient().performRequest("GET", "/_nodes/settings", headers);
        Map<String, Object> responseMap = JsonXContent.jsonXContent.createParser(response.getEntity().getContent()).map();
        @SuppressWarnings("unchecked")
        Map<String, Object> nodes = (Map<String, Object>) responseMap.get("nodes");
        for (Object node : nodes.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> settings = (Map<String, Object>) ((Map<String, Object>) node).get("settings");
            assertThat(extractValue("xpack.monitoring.collection.exporters._http.type", settings), equalTo("http"));
            assertThat(extractValue("xpack.monitoring.collection.exporters._http.enabled", settings), equalTo("false"));
            assertNullSetting(settings, "xpack.monitoring.collection.exporters._http.auth.username");
            assertNullSetting(settings, "xpack.monitoring.collection.exporters._http.auth.password");
            assertNullSetting(settings, "xpack.monitoring.collection.exporters._http.ssl.truststore.path");
            assertNullSetting(settings, "xpack.monitoring.collection.exporters._http.ssl.truststore.password");
            assertNullSetting(settings, "xpack.monitoring.collection.exporters._http.ssl.hostname_verification");
        }
    }

    private void assertNullSetting(Map<String, Object> settings, String setting) {
        assertThat(extractValue(setting, settings), nullValue());
    }
}
