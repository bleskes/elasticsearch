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

package org.elasticsearch.xpack.common.secret;

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.crypto.CryptoService;

/**
 *
 */
public interface SecretService {

    char[] encrypt(char[] text);

    char[] decrypt(char[] text);

    class Insecure implements SecretService {

        public static final Insecure INSTANCE = new Insecure();

        Insecure() {
        }

        @Override
        public char[] encrypt(char[] text) {
            return text;
        }

        @Override
        public char[] decrypt(char[] text) {
            return text;
        }
    }

    /**
     *
     */
    class Secure extends AbstractComponent implements SecretService {

        private final CryptoService cryptoService;

        @Inject
        public Secure(Settings settings, CryptoService cryptoService) {
            super(settings);
            this.cryptoService = cryptoService;
        }

        @Override
        public char[] encrypt(char[] text) {
            return cryptoService.encrypt(text);
        }

        @Override
        public char[] decrypt(char[] text) {
            return cryptoService.decrypt(text);
        }
    }
}
