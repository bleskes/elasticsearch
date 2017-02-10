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
package org.elasticsearch.xpack.security;

import org.apache.lucene.util.CollectionUtil;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESSingleNodeTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class InternalClientIntegTests extends ESSingleNodeTestCase {

    public void testFetchAllEntities() throws ExecutionException, InterruptedException {
        Client client = client();
        int numDocs = randomIntBetween(5, 30);
        for (int i = 0; i < numDocs; i++) {
            client.prepareIndex("foo", "bar").setSource(Collections.singletonMap("number", i)).get();
        }
        client.admin().indices().prepareRefresh("foo").get();
        SearchRequest request = client.prepareSearch()
                .setScroll(TimeValue.timeValueHours(10L))
                .setQuery(QueryBuilders.matchAllQuery())
                .setSize(randomIntBetween(1, 10))
                .setFetchSource(true)
                .request();
        request.indicesOptions().ignoreUnavailable();
        PlainActionFuture<Collection<Integer>> future = new PlainActionFuture<>();
        InternalClient.fetchAllByEntity(client(), request, future, (hit) -> Integer.parseInt(hit.sourceAsMap().get("number").toString()));
        Collection<Integer> integers = future.actionGet();
        ArrayList<Integer> list = new ArrayList<>(integers);
        CollectionUtil.timSort(list);
        assertEquals(numDocs, list.size());
        for (int i = 0; i < numDocs; i++) {
            assertEquals(list.get(i).intValue(), i);
        }
    }
}
