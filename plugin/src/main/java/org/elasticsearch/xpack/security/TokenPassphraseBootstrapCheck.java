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
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.XPackSettings;
import org.elasticsearch.xpack.security.authc.TokenService;

/**
 * Bootstrap check to ensure that the user has set the token passphrase setting and is not using
 * the default value in production
 */
final class TokenPassphraseBootstrapCheck implements BootstrapCheck {

    static final int MINIMUM_PASSPHRASE_LENGTH = 8;

    private final Settings settings;

    TokenPassphraseBootstrapCheck(Settings settings) {
        this.settings = settings;
    }

    @Override
    public boolean check() {
        if (XPackSettings.TOKEN_SERVICE_ENABLED_SETTING.get(settings)) {
            try (SecureString secureString = TokenService.TOKEN_PASSPHRASE.get(settings)) {
                return secureString.length() < MINIMUM_PASSPHRASE_LENGTH || secureString.equals(TokenService.DEFAULT_PASSPHRASE);
            }
        }
        // service is not enabled so no need to check
        return false;
    }

    @Override
    public String errorMessage() {
        return "Please set a passphrase using the elasticsearch-keystore tool for the setting [" + TokenService.TOKEN_PASSPHRASE.getKey() +
                "] that is at least " + MINIMUM_PASSPHRASE_LENGTH + " characters in length and does not match the default passphrase or " +
                "disable the token service using the [" + XPackSettings.TOKEN_SERVICE_ENABLED_SETTING.getKey() + "] setting";
    }
}
