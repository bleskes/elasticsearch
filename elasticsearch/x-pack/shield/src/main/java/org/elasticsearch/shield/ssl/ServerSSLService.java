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

public class ServerSSLService extends AbstractSSLService {

    @Inject
    public ServerSSLService(Settings settings, Environment environment) {
        super(settings, environment);
    }

    @Override
    protected SSLSettings sslSettings(Settings customSettings) {
        SSLSettings sslSettings = new SSLSettings(customSettings, settings);

        if (sslSettings.keyStorePath == null) {
            throw new IllegalArgumentException("no keystore configured");
        }
        if (sslSettings.keyStorePassword == null) {
            throw new IllegalArgumentException("no keystore password configured");
        }
        assert sslSettings.keyPassword != null;

        assert sslSettings.trustStorePath != null;
        if (sslSettings.trustStorePassword == null) {
            throw new IllegalArgumentException("no truststore password configured");
        }
        return sslSettings;
    }
}
