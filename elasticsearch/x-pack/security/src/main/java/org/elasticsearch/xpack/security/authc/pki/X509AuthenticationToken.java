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

package org.elasticsearch.xpack.security.authc.pki;

import org.elasticsearch.xpack.security.authc.AuthenticationToken;

import java.security.cert.X509Certificate;

public class X509AuthenticationToken implements AuthenticationToken {

    private final String principal;
    private final String dn;
    private X509Certificate[] credentials;

    public X509AuthenticationToken(X509Certificate[] certificates, String principal, String dn) {
        this.principal = principal;
        this.credentials = certificates;
        this.dn = dn;
    }

    @Override
    public String principal() {
        return principal;
    }

    @Override
    public X509Certificate[] credentials() {
        return credentials;
    }

    public String dn() {
        return dn;
    }

    @Override
    public void clearCredentials() {
        credentials = null;
    }
}
