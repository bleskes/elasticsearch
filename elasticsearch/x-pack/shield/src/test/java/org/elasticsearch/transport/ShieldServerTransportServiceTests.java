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

package org.elasticsearch.transport;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.Shield;
import org.elasticsearch.shield.transport.ShieldServerTransportService;
import org.elasticsearch.test.ShieldIntegTestCase;
import org.elasticsearch.xpack.XPackPlugin;

import java.util.Map;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.startsWith;

// this class sits in org.elasticsearch.transport so that TransportService.requestHandlers is visible
public class ShieldServerTransportServiceTests extends ShieldIntegTestCase {
    @Override
    protected Settings transportClientSettings() {
        return Settings.settingsBuilder()
                .put(super.transportClientSettings())
                .put(XPackPlugin.featureEnabledSetting(Shield.NAME), true)
                .build();
    }

    public void testShieldServerTransportServiceWrapsAllHandlers() {
        for (TransportService transportService : internalCluster().getInstances(TransportService.class)) {
            assertThat(transportService, instanceOf(ShieldServerTransportService.class));
            for (Map.Entry<String, RequestHandlerRegistry> entry : transportService.requestHandlers.entrySet()) {
                assertThat(
                        "handler not wrapped by " + ShieldServerTransportService.ProfileSecuredRequestHandler.class +
                                "; do all the handler registration methods have overrides?",
                        entry.getValue().toString(),
                        startsWith(ShieldServerTransportService.ProfileSecuredRequestHandler.class.getName() + "@")
                );
            }
        }
    }
}
