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
package org.elasticsearch.xpack;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * TransportClient.Builder that installs the XPackPlugin by default.
 */
@SuppressWarnings({"unchecked","varargs"})
public class XPackTransportClient extends TransportClient {

    @SafeVarargs
    public XPackTransportClient(Settings settings, Class<? extends Plugin>... plugins) {
        this(settings, Arrays.asList(plugins));
    }

    public XPackTransportClient(Settings settings, Collection<Class<? extends Plugin>> plugins) {
        super(settings, Settings.EMPTY, addPlugins(plugins, XPackPlugin.class));
    }
}
