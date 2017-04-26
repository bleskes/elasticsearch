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

import org.elasticsearch.common.settings.MockSecureSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.XPackSettings;
import org.elasticsearch.xpack.security.TokenPassphraseBootstrapCheck;
import org.elasticsearch.xpack.security.authc.TokenService;

import static org.elasticsearch.xpack.security.TokenPassphraseBootstrapCheck.MINIMUM_PASSPHRASE_LENGTH;

public class TokenPassphraseBootstrapCheckTests extends ESTestCase {

    public void testTokenPassphraseCheck() throws Exception {
        assertTrue(new TokenPassphraseBootstrapCheck(Settings.EMPTY).check());
        MockSecureSettings secureSettings = new MockSecureSettings();
        Settings settings = Settings.builder().setSecureSettings(secureSettings).build();
        assertTrue(new TokenPassphraseBootstrapCheck(settings).check());

        secureSettings.setString(TokenService.TOKEN_PASSPHRASE.getKey(), randomAlphaOfLengthBetween(MINIMUM_PASSPHRASE_LENGTH, 30));
        assertFalse(new TokenPassphraseBootstrapCheck(settings).check());

        secureSettings.setString(TokenService.TOKEN_PASSPHRASE.getKey(), TokenService.DEFAULT_PASSPHRASE);
        assertTrue(new TokenPassphraseBootstrapCheck(settings).check());

        secureSettings.setString(TokenService.TOKEN_PASSPHRASE.getKey(), randomAlphaOfLengthBetween(1, MINIMUM_PASSPHRASE_LENGTH - 1));
        assertTrue(new TokenPassphraseBootstrapCheck(settings).check());
    }

    public void testTokenPassphraseCheckServiceDisabled() throws Exception {
        Settings settings = Settings.builder().put(XPackSettings.TOKEN_SERVICE_ENABLED_SETTING.getKey(), false).build();
        assertFalse(new TokenPassphraseBootstrapCheck(settings).check());
        MockSecureSettings secureSettings = new MockSecureSettings();
        settings = Settings.builder().put(settings).setSecureSettings(secureSettings).build();
        assertFalse(new TokenPassphraseBootstrapCheck(settings).check());

        secureSettings.setString(TokenService.TOKEN_PASSPHRASE.getKey(), randomAlphaOfLengthBetween(1, 30));
        assertFalse(new TokenPassphraseBootstrapCheck(settings).check());

        secureSettings.setString(TokenService.TOKEN_PASSPHRASE.getKey(), TokenService.DEFAULT_PASSPHRASE);
        assertFalse(new TokenPassphraseBootstrapCheck(settings).check());
    }
}
