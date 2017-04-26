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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.XPackSettings;
import org.elasticsearch.xpack.security.TokenSSLBootstrapCheck;

public class TokenSSLBootsrapCheckTests extends ESTestCase {

    public void testTokenSSLBootstrapCheck() {
        Settings settings = Settings.EMPTY;
        assertTrue(new TokenSSLBootstrapCheck(settings).check());

        settings = Settings.builder().put(XPackSettings.TOKEN_SERVICE_ENABLED_SETTING.getKey(), false).build();
        assertFalse(new TokenSSLBootstrapCheck(settings).check());

        settings = Settings.builder().put(XPackSettings.HTTP_SSL_ENABLED.getKey(), true).build();
        assertFalse(new TokenSSLBootstrapCheck(settings).check());

        settings = Settings.builder().put(XPackSettings.HTTP_SSL_ENABLED.getKey(), false).build();
        assertTrue(new TokenSSLBootstrapCheck(settings).check());
    }
}
