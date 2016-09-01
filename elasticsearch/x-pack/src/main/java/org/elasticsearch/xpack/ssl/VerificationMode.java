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

package org.elasticsearch.xpack.ssl;

import java.util.Locale;

/**
 * Represents the verification mode to be used for SSL connections.
 */
public enum VerificationMode {
    NONE {
        @Override
        public boolean isHostnameVerificationEnabled() {
            return false;
        }

        @Override
        public boolean isCertificateVerificationEnabled() {
            return false;
        }
    },
    CERTIFICATE {
        @Override
        public boolean isHostnameVerificationEnabled() {
            return false;
        }

        @Override
        public boolean isCertificateVerificationEnabled() {
            return true;
        }
    },
    FULL {
        @Override
        public boolean isHostnameVerificationEnabled() {
            return true;
        }

        @Override
        public boolean isCertificateVerificationEnabled() {
            return true;
        }
    };

    /**
     * @return true if hostname verification is enabled
     */
    public abstract boolean isHostnameVerificationEnabled();

    /**
     * @return true if certificate verification is enabled
     */
    public abstract boolean isCertificateVerificationEnabled();

    public static VerificationMode parse(String value) {
        assert value != null;
        switch (value.toLowerCase(Locale.ROOT)) {
            case "none":
                return NONE;
            case "certificate":
                return CERTIFICATE;
            case "full":
                return FULL;
            default:
                throw new IllegalArgumentException("could not resolve verification mode. unknown value [" + value + "]");
        }
    }
}