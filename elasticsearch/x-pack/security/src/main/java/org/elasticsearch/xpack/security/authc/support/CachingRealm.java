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

package org.elasticsearch.xpack.security.authc.support;

/**
 * This interface allows a {@link org.elasticsearch.xpack.security.authc.Realm} to indicate that it supports caching user credentials
 * and expose the ability to clear the cache for a given String identifier or all of the cache
 */
public interface CachingRealm {

    /**
     * Expires a single user from the cache identified by the String agument
     * @param username the identifier of the user to be cleared
     */
    void expire(String username);

    /**
     * Expires all of the data that has been cached in this realm
     */
    void expireAll();
}
