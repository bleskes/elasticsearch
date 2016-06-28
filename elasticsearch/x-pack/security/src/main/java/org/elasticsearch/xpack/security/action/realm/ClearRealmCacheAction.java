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

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 *
 */
public class ClearRealmCacheAction extends Action<ClearRealmCacheRequest, ClearRealmCacheResponse, ClearRealmCacheRequestBuilder> {

    public static final ClearRealmCacheAction INSTANCE = new ClearRealmCacheAction();
    public static final String NAME = "cluster:admin/xpack/security/realm/cache/clear";

    protected ClearRealmCacheAction() {
        super(NAME);
    }

    @Override
    public ClearRealmCacheRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new ClearRealmCacheRequestBuilder(client, this);
    }

    @Override
    public ClearRealmCacheResponse newResponse() {
        return new ClearRealmCacheResponse();
    }
}
