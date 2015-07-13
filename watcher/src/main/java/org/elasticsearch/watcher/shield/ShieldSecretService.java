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

package org.elasticsearch.watcher.shield;

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.crypto.CryptoService;
import org.elasticsearch.watcher.support.secret.SecretService;

/**
 *
 */
public class ShieldSecretService extends AbstractComponent implements SecretService {

    private final CryptoService cryptoService;
    private final boolean encryptSensitiveData;

    @Inject
    public ShieldSecretService(Settings settings, CryptoService cryptoService) {
        super(settings);
        this.encryptSensitiveData = settings.getAsBoolean("watcher.shield.encrypt_sensitive_data", false);
        this.cryptoService = cryptoService;
    }

    @Override
    public char[] encrypt(char[] text) {
        return encryptSensitiveData ? cryptoService.encrypt(text) : text;
    }

    @Override
    public char[] decrypt(char[] text) {
        return encryptSensitiveData ? cryptoService.decrypt(text) : text;
    }
}
