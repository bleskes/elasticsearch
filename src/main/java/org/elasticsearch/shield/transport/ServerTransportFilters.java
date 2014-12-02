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
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

import java.util.Map;

public class ServerTransportFilters extends AbstractComponent {

    public static final String SETTING_NAME = "shield.type";

    public static final String SERVER_TRANSPORT_FILTER_TRANSPORT_CLIENT = "transportclient";
    public static final String SERVER_TRANSPORT_FILTER_AUTHENTICATE_REJECT_INTERNAL_ACTIONS = "client";
    public static final String SERVER_TRANSPORT_FILTER_AUTHENTICATE_ONLY = "server";


    private final Map<String, ServerTransportFilter> transportFilters;
    private final boolean isTransportClient;
    private final ServerTransportFilter clientServerTransportFilter;

    @Inject
    public ServerTransportFilters(Settings settings, Map<String, ServerTransportFilter> configuredTransportFilter) {
        super(settings);
        this.isTransportClient = "transport".equals(settings.get("client.type"));
        this.clientServerTransportFilter = configuredTransportFilter.get(SERVER_TRANSPORT_FILTER_TRANSPORT_CLIENT);

        Map<String, Settings> profileSettings = settings.getGroups("transport.profiles.", true);
        this.transportFilters = Maps.newHashMapWithExpectedSize(profileSettings.size());

        for (Map.Entry<String, Settings> entry : profileSettings.entrySet()) {
            String type = entry.getValue().get(SETTING_NAME, SERVER_TRANSPORT_FILTER_AUTHENTICATE_ONLY);
            transportFilters.put(entry.getKey(), configuredTransportFilter.get(type));
        }

        if (!transportFilters.containsKey("default")) {
            transportFilters.put("default", configuredTransportFilter.get(SERVER_TRANSPORT_FILTER_AUTHENTICATE_ONLY));
        }

        logger.trace("Added shield transport filters: {}", transportFilters.keySet());
    }

    public ServerTransportFilter getTransportFilterForProfile(String profile) {
        if (isTransportClient) {
            return clientServerTransportFilter;
        }

        if (!transportFilters.containsKey(profile)) {
            return transportFilters.get("default");
        }

        return transportFilters.get(profile);
    }
}
