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

package org.elasticsearch.xpack.security.support;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.security.Security;

/**
 *
 */
public abstract class AbstractSecurityModule extends AbstractModule {

    protected final Settings settings;
    protected final boolean clientMode;
    protected final boolean securityEnabled;

    public AbstractSecurityModule(Settings settings) {
        this.settings = settings;
        this.clientMode = TransportClient.CLIENT_TYPE.equals(settings.get(Client.CLIENT_TYPE_SETTING_S.getKey()));
        this.securityEnabled = Security.enabled(settings);
    }

    @Override
    protected final void configure() {
        configure(clientMode);
    }

    protected abstract void configure(boolean clientMode);

    public static abstract class Node extends AbstractSecurityModule {

        protected Node(Settings settings) {
            super(settings);
        }

        @Override
        protected final void configure(boolean clientMode) {
            assert !clientMode : "[" + getClass().getSimpleName() + "] is a node only module";
            configureNode();
        }

        protected abstract void configureNode();
    }
}
