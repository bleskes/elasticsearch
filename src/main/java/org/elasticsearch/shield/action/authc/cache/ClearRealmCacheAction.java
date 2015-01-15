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

package org.elasticsearch.shield.action.authc.cache;

import org.elasticsearch.action.admin.cluster.ClusterAction;
import org.elasticsearch.client.ClusterAdminClient;

/**
 *
 */
public class ClearRealmCacheAction extends ClusterAction<ClearRealmCacheRequest, ClearRealmCacheResponse, ClearRealmCacheRequestBuilder> {

    public static final ClearRealmCacheAction INSTANCE = new ClearRealmCacheAction();
    public static final String NAME = "cluster:admin/shield/realm/cache/clear";

    protected ClearRealmCacheAction() {
        super(NAME);
    }

    @Override
    public ClearRealmCacheRequestBuilder newRequestBuilder(ClusterAdminClient client) {
        return new ClearRealmCacheRequestBuilder(client);
    }

    @Override
    public ClearRealmCacheResponse newResponse() {
        return new ClearRealmCacheResponse();
    }
}
