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

package org.elasticsearch.xpack.common.http.auth.basic;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.elasticsearch.xpack.common.http.auth.ApplicableHttpAuth;
import org.elasticsearch.xpack.security.crypto.CryptoService;

/**
 */
public class ApplicableBasicAuth extends ApplicableHttpAuth<BasicAuth> {

    private final String basicAuth;

    public ApplicableBasicAuth(BasicAuth auth, CryptoService service) {
        super(auth);
        basicAuth = headerValue(auth.username, auth.password.text(service));
    }

    public static String headerValue(String username, char[] password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + new String(password)).getBytes(StandardCharsets.UTF_8));
    }

    public void apply(HttpURLConnection connection) {
        connection.setRequestProperty("Authorization", basicAuth);
    }

}
