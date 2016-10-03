/*
 * ELASTICSEARCH CONFIDENTIAL
 *  __________________
 *
 * [2014] Elasticsearch Incorporated. All Rights Reserved.
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

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.support.WriteRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Request builder for setting a user as enabled or disabled
 */
public class SetEnabledRequestBuilder extends ActionRequestBuilder<SetEnabledRequest, SetEnabledResponse, SetEnabledRequestBuilder>
        implements WriteRequestBuilder<SetEnabledRequestBuilder> {

    public SetEnabledRequestBuilder(ElasticsearchClient client) {
        super(client, SetEnabledAction.INSTANCE, new SetEnabledRequest());
    }

    /**
     * Set the username of the user that should enabled or disabled. Must not be {@code null}
     */
    public SetEnabledRequestBuilder username(String username) {
        request.username(username);
        return this;
    }

    /**
     * Set whether the user should be enabled or not
     */
    public SetEnabledRequestBuilder enabled(boolean enabled) {
        request.enabled(enabled);
        return this;
    }
}
