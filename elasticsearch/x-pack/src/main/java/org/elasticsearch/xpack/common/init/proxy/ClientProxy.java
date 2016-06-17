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

package org.elasticsearch.xpack.common.init.proxy;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.xpack.security.InternalClient;
import org.elasticsearch.transport.TransportMessage;
import org.elasticsearch.xpack.common.init.LazyInitializable;

/**
 * A lazily initialized proxy to an elasticsearch {@link Client}. Inject this proxy whenever a client
 * needs to injected to be avoid circular dependencies issues.
 */
public class ClientProxy implements LazyInitializable {

    protected InternalClient client;

    @Override
    public void init(Injector injector) {
        this.client = injector.getInstance(InternalClient.class);
    }

    public AdminClient admin() {
        return client.admin();
    }

    public void bulk(BulkRequest request, ActionListener<BulkResponse> listener) {
        client.bulk(preProcess(request), listener);
    }

    public BulkRequestBuilder prepareBulk() {
        return client.prepareBulk();
    }

    protected <M extends TransportMessage> M preProcess(M message) {
        return message;
    }
}
