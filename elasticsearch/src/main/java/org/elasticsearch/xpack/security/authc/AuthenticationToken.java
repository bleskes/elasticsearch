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

package org.elasticsearch.xpack.security.authc;

/**
 * Interface for a token that is used for authentication. This token is the representation of the authentication
 * information that is presented with a request. The token will be extracted by a {@link Realm} and subsequently
 * used by a Realm to attempt authentication of a user.
 */
public interface AuthenticationToken {

    String principal();

    Object credentials();

    void clearCredentials();
}
