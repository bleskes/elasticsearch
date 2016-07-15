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

package org.elasticsearch.xpack.security.ssl;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.security.ssl.SSLConfiguration.Global;

public class ClientSSLService extends AbstractSSLService {

    public ClientSSLService(Settings settings, Global globalSSLConfiguration) {
        super(settings, null, globalSSLConfiguration, null);
    }

    public void setEnvAndResourceWatcher(Environment environment, ResourceWatcherService resourceWatcherService) {
        this.env = environment;
        this.resourceWatcherService = resourceWatcherService;
    }

    @Override
    protected void validateSSLConfiguration(SSLConfiguration sslConfiguration) {
        sslConfiguration.keyConfig().validate();
        sslConfiguration.trustConfig().validate();
    }
}
