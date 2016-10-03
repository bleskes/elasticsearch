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

package org.elasticsearch.xpack.security.action.realm;

import org.elasticsearch.action.support.nodes.NodesOperationRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

/**
 *
 */
public class ClearRealmCacheRequestBuilder extends NodesOperationRequestBuilder<ClearRealmCacheRequest, ClearRealmCacheResponse,
        ClearRealmCacheRequestBuilder> {

    public ClearRealmCacheRequestBuilder(ElasticsearchClient client) {
        this(client, ClearRealmCacheAction.INSTANCE);
    }

    public ClearRealmCacheRequestBuilder(ElasticsearchClient client, ClearRealmCacheAction action) {
        super(client, action, new ClearRealmCacheRequest());
    }

    /**
     * Sets the realms for which caches will be evicted. When not set all the caches of all realms will be
     * evicted.
     *
     * @param realms The realm names
     */
    public ClearRealmCacheRequestBuilder realms(String... realms) {
        request.realms(realms);
        return this;
    }

    /**
     * Sets the usernames of the users that should be evicted from the caches. When not set, all users
     * will be evicted.
     *
     * @param usernames The usernames
     */
    public ClearRealmCacheRequestBuilder usernames(String... usernames) {
        request.usernames(usernames);
        return this;
    }
}
