/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security;

import org.elasticsearch.bootstrap.BootstrapCheck;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.XPackSettings;

import static org.elasticsearch.xpack.XPackSettings.HTTP_SSL_ENABLED;

/**
 * Bootstrap check to ensure that the user has enabled HTTPS when using the token service
 */
final class TokenSSLBootstrapCheck implements BootstrapCheck {

    private final Settings settings;

    TokenSSLBootstrapCheck(Settings settings) {
        this.settings = settings;
    }

    @Override
    public boolean check() {
        return XPackSettings.TOKEN_SERVICE_ENABLED_SETTING.get(settings) && HTTP_SSL_ENABLED.get(settings) == false;
    }

    @Override
    public String errorMessage() {
        return "HTTPS is required in order to use the token service. Please enable HTTPS using the [" + HTTP_SSL_ENABLED.getKey() +
                "] setting or disable the token service using the [" + XPackSettings.TOKEN_SERVICE_ENABLED_SETTING.getKey() + "] setting.";
    }
}
