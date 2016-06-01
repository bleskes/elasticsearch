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

package org.elasticsearch.xpack.security.transport;

import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.security.support.AbstractSecurityModule;
import org.elasticsearch.xpack.security.transport.filter.IPFilter;

/**
 *
 */
public class SecurityTransportModule extends AbstractSecurityModule {

    public SecurityTransportModule(Settings settings) {
        super(settings);
    }

    @Override
    protected void configure(boolean clientMode) {
        if (securityEnabled == false) {
            bind(IPFilter.class).toProvider(Providers.<IPFilter>of(null));
            return;
        }

        if (clientMode) {
            // no ip filtering on the client
            bind(IPFilter.class).toProvider(Providers.<IPFilter>of(null));
            bind(ClientTransportFilter.class).to(ClientTransportFilter.TransportClient.class).asEagerSingleton();
        } else {
            bind(ClientTransportFilter.class).to(ClientTransportFilter.Node.class).asEagerSingleton();
            if (IPFilter.IP_FILTER_ENABLED_SETTING.get(settings)) {
                bind(IPFilter.class).asEagerSingleton();
            }
        }
    }
}
