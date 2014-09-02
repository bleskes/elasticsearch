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

package org.elasticsearch.shield.n2n;

import org.elasticsearch.common.Nullable;

import java.net.InetAddress;
import java.security.Principal;

/**
 *
 */
public interface N2NAuthenticator {

    N2NAuthenticator NO_AUTH = new N2NAuthenticator() {
        @Override
        public boolean authenticate(@Nullable Principal peerPrincipal, InetAddress peerAddress, int peerPort) {
            return true;
        }
    };

    boolean authenticate(@Nullable Principal peerPrincipal, InetAddress peerAddress, int peerPort);


    class Compound implements N2NAuthenticator {

        private N2NAuthenticator[] authenticators;

        public Compound(N2NAuthenticator... authenticators) {
            this.authenticators = authenticators;
        }

        @Override
        public boolean authenticate(@Nullable Principal peerPrincipal, InetAddress peerAddress, int peerPort) {
            for (int i = 0; i < authenticators.length; i++) {
                if (!authenticators[i].authenticate(peerPrincipal, peerAddress, peerPort)) {
                    return false;
                }
            }
            return true;
        }
    }
}
