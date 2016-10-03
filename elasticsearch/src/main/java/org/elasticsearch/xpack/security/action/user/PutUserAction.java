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

package org.elasticsearch.xpack.security.action.user;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Action for putting (adding/updating) a native user.
 */
public class PutUserAction extends Action<PutUserRequest, PutUserResponse, PutUserRequestBuilder> {

    public static final PutUserAction INSTANCE = new PutUserAction();
    public static final String NAME = "cluster:admin/xpack/security/user/put";

    protected PutUserAction() {
        super(NAME);
    }

    @Override
    public PutUserRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new PutUserRequestBuilder(client, this);
    }

    @Override
    public PutUserResponse newResponse() {
        return new PutUserResponse();
    }
}
