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

package org.elasticsearch.xpack.security.action.role;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * The action for clearing the cache used by native roles that are stored in an index.
 */
public class ClearRolesCacheAction extends Action<ClearRolesCacheRequest, ClearRolesCacheResponse, ClearRolesCacheRequestBuilder> {

    public static final ClearRolesCacheAction INSTANCE = new ClearRolesCacheAction();
    public static final String NAME = "cluster:admin/xpack/security/roles/cache/clear";

    protected ClearRolesCacheAction() {
        super(NAME);
    }

    @Override
    public ClearRolesCacheRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new ClearRolesCacheRequestBuilder(client, this, new ClearRolesCacheRequest());
    }

    @Override
    public ClearRolesCacheResponse newResponse() {
        return new ClearRolesCacheResponse();
    }
}
