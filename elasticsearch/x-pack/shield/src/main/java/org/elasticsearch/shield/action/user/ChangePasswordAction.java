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

package org.elasticsearch.shield.action.user;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 */
public class ChangePasswordAction extends Action<ChangePasswordRequest, ChangePasswordResponse, ChangePasswordRequestBuilder> {

    public static final ChangePasswordAction INSTANCE = new ChangePasswordAction();
    public static final String NAME = "cluster:admin/xpack/security/user/change_password";

    protected ChangePasswordAction() {
        super(NAME);
    }

    @Override
    public ChangePasswordRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new ChangePasswordRequestBuilder(client, this);
    }

    @Override
    public ChangePasswordResponse newResponse() {
        return new ChangePasswordResponse();
    }
}
