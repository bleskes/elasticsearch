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

import org.elasticsearch.common.Nullable;
import org.elasticsearch.env.Environment;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.nio.file.Path;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

abstract class KeyConfig extends TrustConfig {

    KeyConfig(boolean includeSystem) {
        super(includeSystem);
    }

    static final KeyConfig NONE = new KeyConfig(false) {
        @Override
        X509ExtendedKeyManager createKeyManager(@Nullable Environment environment) {
            return null;
        }

        @Override
        X509ExtendedTrustManager nonSystemTrustManager(@Nullable Environment environment) {
            return null;
        }

        @Override
        void validate() {
        }

        @Override
        List<Path> filesToMonitor(@Nullable Environment environment) {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return "NONE";
        }
    };

    abstract X509ExtendedKeyManager createKeyManager(@Nullable Environment environment);
}
