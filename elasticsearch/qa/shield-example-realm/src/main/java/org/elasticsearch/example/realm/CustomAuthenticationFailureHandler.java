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

package org.elasticsearch.example.realm;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.shield.authc.AuthenticationToken;
import org.elasticsearch.shield.authc.DefaultAuthenticationFailureHandler;
import org.elasticsearch.transport.TransportMessage;

public class CustomAuthenticationFailureHandler extends DefaultAuthenticationFailureHandler {

    @Override
    public ElasticsearchSecurityException unsuccessfulAuthentication(RestRequest request, AuthenticationToken token) {
        ElasticsearchSecurityException e = super.unsuccessfulAuthentication(request, token);
        // set a custom header
        e.addHeader("WWW-Authenticate", "custom-challenge");
        return e;
    }

    @Override
    public ElasticsearchSecurityException unsuccessfulAuthentication(TransportMessage message, AuthenticationToken token, String action) {
        ElasticsearchSecurityException e = super.unsuccessfulAuthentication(message, token, action);
        // set a custom header
        e.addHeader("WWW-Authenticate", "custom-challenge");
        return e;
    }

    @Override
    public ElasticsearchSecurityException missingToken(RestRequest request) {
        ElasticsearchSecurityException e = super.missingToken(request);
        // set a custom header
        e.addHeader("WWW-Authenticate", "custom-challenge");
        return e;
    }

    @Override
    public ElasticsearchSecurityException missingToken(TransportMessage message, String action) {
        ElasticsearchSecurityException e = super.missingToken(message, action);
        // set a custom header
        e.addHeader("WWW-Authenticate", "custom-challenge");
        return e;
    }
}
