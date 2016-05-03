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

package org.elasticsearch.shield.ssl;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.shield.ssl.SSLConfiguration.Global;
import org.elasticsearch.watcher.ResourceWatcherService;

public class ClientSSLService extends AbstractSSLService {

    @Inject
    public ClientSSLService(Settings settings, Global globalSSLConfiguration) {
        super(settings, null, globalSSLConfiguration, null);
    }

    @Inject(optional = true)
    public void setEnvironment(Environment environment) {
        this.env = environment;
    }

    @Inject(optional = true)
    public void setResourceWatcherService(ResourceWatcherService resourceWatcherService) {
        this.resourceWatcherService = resourceWatcherService;
    }

    @Override
    protected void validateSSLConfiguration(SSLConfiguration sslConfiguration) {
        sslConfiguration.keyConfig().validate();
        sslConfiguration.trustConfig().validate();
    }
}
