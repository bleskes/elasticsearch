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

import javax.net.ssl.SSLEngine;
import java.util.Locale;

/**
 * The client authentication mode that is used for SSL servers
 */
public enum SSLClientAuth {

    NONE() {
        public boolean enabled() {
            return false;
        }

        public void configure(SSLEngine engine) {
            // nothing to do here
            assert !engine.getWantClientAuth();
            assert !engine.getNeedClientAuth();
        }
    },
    OPTIONAL() {
        public boolean enabled() {
            return true;
        }

        public void configure(SSLEngine engine) {
            engine.setWantClientAuth(true);
        }
    },
    REQUIRED() {
        public boolean enabled() {
            return true;
        }

        public void configure(SSLEngine engine) {
            engine.setNeedClientAuth(true);
        }
    };

    /**
     * @return true if client authentication is enabled
     */
    public abstract boolean enabled();

    /**
     * Configure client authentication of the provided {@link SSLEngine}
     */
    public abstract void configure(SSLEngine engine);

    public static SSLClientAuth parse(String value) {
        assert value != null;
        switch (value.toLowerCase(Locale.ROOT)) {
            case "none":
                return NONE;
            case "optional":
                return OPTIONAL;
            case "required":
                return REQUIRED;
            default:
                throw new IllegalArgumentException("could not resolve ssl client auth. unknown value [" + value + "]");
        }
    }
}
