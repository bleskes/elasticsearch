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

package org.elasticsearch.shield;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.integration.LicensingTests;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;
import org.elasticsearch.shield.transport.ShieldServerTransportService;
import org.elasticsearch.shield.transport.netty.ShieldNettyTransport;
import org.elasticsearch.test.ShieldIntegTestCase;
import org.elasticsearch.test.ShieldSettingsSource;
import org.elasticsearch.test.rest.client.http.HttpRequestBuilder;
import org.elasticsearch.test.rest.client.http.HttpResponse;
import org.elasticsearch.test.rest.json.JsonPath;
import org.elasticsearch.transport.Transport;
import org.elasticsearch.transport.TransportService;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.basicAuthHeaderValue;
import static org.hamcrest.Matchers.*;

/**
 *
 */
public class ShieldPluginEnabledDisabledTests extends ShieldIntegTestCase {

    private static boolean enabled;

    @BeforeClass
    public static void init() {
        enabled = randomBoolean();
    }

    @After
    public void cleanup() throws Exception {
        // now that on a disabled license we block cluster health/stats and indices stats, we need
        // to make sure that after the tests (which disable the license for testing purposes) we
        // reenabled the license, so the internal cluster will be cleaned appropriately.
        logger.info("cleanup: enabling licensing...");
        LicensingTests.enableLicensing();
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        logger.info("******* shield is " + (enabled ? "enabled" : "disabled"));
        return Settings.settingsBuilder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(ShieldPlugin.ENABLED_SETTING_NAME, enabled)
                .put(Node.HTTP_ENABLED, true)
                .build();
    }

    @Override
    protected Settings transportClientSettings() {
        return Settings.settingsBuilder()
                .put(super.transportClientSettings())
                .put(ShieldPlugin.ENABLED_SETTING_NAME, enabled)
                .build();
    }

    @Override
    protected Class<? extends Plugin> licensePluginClass() {
        return LicensingTests.InternalLicensePlugin.class;
    }

    @Override
    protected String licensePluginName() {
        return LicensingTests.InternalLicensePlugin.NAME;
    }

    @Test
    public void testTransportEnabledDisabled() throws Exception {
        for (TransportService service : internalCluster().getInstances(TransportService.class)) {
            Matcher<TransportService> matcher = instanceOf(ShieldServerTransportService.class);
            if (!enabled) {
                matcher = not(matcher);
            }
            assertThat(service, matcher);
        }
        for (Transport transport : internalCluster().getInstances(Transport.class)) {
            Matcher<Transport> matcher = instanceOf(ShieldNettyTransport.class);
            if (!enabled) {
                matcher = not(matcher);
            }
            assertThat(transport, matcher);
        }
    }

    @Test
    public void testShieldInfoStatus() throws IOException {
        HttpServerTransport httpServerTransport = internalCluster().getDataNodeInstance(HttpServerTransport.class);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpResponse response = new HttpRequestBuilder(httpClient).httpTransport(httpServerTransport).method("GET").path("/_shield").addHeader(UsernamePasswordToken.BASIC_AUTH_HEADER,
                    basicAuthHeaderValue(ShieldSettingsSource.DEFAULT_USER_NAME, new SecuredString(ShieldSettingsSource.DEFAULT_PASSWORD.toCharArray()))).execute();
            assertThat(response.getStatusCode(), is(OK.getStatus()));
            assertThat(new JsonPath(response.getBody()).evaluate("status").toString(), equalTo(enabled ? "enabled" : "disabled"));

            if (enabled) {
                LicensingTests.disableLicensing();
                response = new HttpRequestBuilder(httpClient).httpTransport(httpServerTransport).method("GET").path("/_shield").addHeader(UsernamePasswordToken.BASIC_AUTH_HEADER,
                        basicAuthHeaderValue(ShieldSettingsSource.DEFAULT_USER_NAME, new SecuredString(ShieldSettingsSource.DEFAULT_PASSWORD.toCharArray()))).execute();
                assertThat(response.getStatusCode(), is(OK.getStatus()));
                assertThat(new JsonPath(response.getBody()).evaluate("status").toString(), equalTo("unlicensed"));
            }
        }
    }
}
