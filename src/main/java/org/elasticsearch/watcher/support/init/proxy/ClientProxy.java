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

package org.elasticsearch.watcher.support.init.proxy;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.transport.TransportMessage;
import org.elasticsearch.watcher.shield.ShieldIntegration;
import org.elasticsearch.watcher.support.init.InitializingService;

import java.util.concurrent.TimeUnit;

/**
 * A lazily initialized proxy to an elasticsearch {@link Client}. Inject this proxy whenever a client
 * needs to injected to be avoid circular dependencies issues.
 */
public class ClientProxy implements InitializingService.Initializable {

    private final ShieldIntegration shieldIntegration;
    private Client client;

    @Inject
    public ClientProxy(ShieldIntegration shieldIntegration) {
        this.shieldIntegration = shieldIntegration;
    }

    /**
     * Creates a proxy to the given client (can be used for testing)
     */
    public static ClientProxy of(Client client) {
        ClientProxy proxy = new ClientProxy(null);
        proxy.client = client;
        return proxy;
    }

    @Override
    public void init(Injector injector) {
        client = injector.getInstance(Client.class);
    }

    public AdminClient admin() {
        return client.admin();
    }

    public IndexResponse index(IndexRequest request) {
        return client.index(preProcess(request)).actionGet();
    }

    public UpdateResponse update(UpdateRequest request) {
        return client.update(preProcess(request)).actionGet();
    }

    public BulkResponse bulk(BulkRequest request) {
        request.listenerThreaded(true);
        return client.bulk(preProcess(request)).actionGet();
    }

    public void index(IndexRequest request, ActionListener<IndexResponse> listener) {
        request.listenerThreaded(true);
        client.index(preProcess(request), listener);
    }

    public void bulk(BulkRequest request, ActionListener<BulkResponse> listener) {
        request.listenerThreaded(true);
        client.bulk(preProcess(request), listener);
    }

    public DeleteResponse delete(DeleteRequest request) {
        return client.delete(preProcess(request)).actionGet();
    }

    public SearchResponse search(SearchRequest request) {
        return client.search(preProcess(request)).actionGet(5, TimeUnit.SECONDS);
    }

    public SearchResponse searchScroll(String scrollId, TimeValue timeout) {
        SearchScrollRequest request = new SearchScrollRequest(scrollId).scroll(timeout);
        return client.searchScroll(preProcess(request)).actionGet();
    }

    public ClearScrollResponse clearScroll(String scrollId) {
        ClearScrollRequest request = new ClearScrollRequest();
        request.addScrollId(scrollId);
        return client.clearScroll(preProcess(request)).actionGet();
    }

    public RefreshResponse refresh(RefreshRequest request) {
        return client.admin().indices().refresh(preProcess(request)).actionGet();
    }

    public PutIndexTemplateResponse putTemplate(PutIndexTemplateRequest request) {
        preProcess(request);
        return client.admin().indices().putTemplate(request).actionGet();
    }

    <M extends TransportMessage> M preProcess(M message) {
        if (shieldIntegration != null) {
            shieldIntegration.bindWatcherUser(message);
        }
        return message;
    }

}
