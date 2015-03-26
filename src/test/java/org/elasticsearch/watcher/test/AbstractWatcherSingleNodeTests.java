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

package org.elasticsearch.watcher.test;

import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.test.ElasticsearchSingleNodeTest;
import org.elasticsearch.watcher.WatcherLifeCycleService;
import org.elasticsearch.watcher.support.init.proxy.ClientProxy;
import org.elasticsearch.watcher.support.init.proxy.ScriptServiceProxy;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.Collections;
import java.util.Map;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertNoFailures;

/**
 *
 */
public abstract class AbstractWatcherSingleNodeTests extends ElasticsearchSingleNodeTest {

    @BeforeClass
    public static void initSuite() throws Exception {
        getInstanceFromNode(WatcherLifeCycleService.class).start();
    }

    @AfterClass
    public static void cleanupSuite() throws Exception {
        getInstanceFromNode(WatcherLifeCycleService.class).stop();
    }

    @Override
    protected boolean resetNodeAfterTest() {
        return false;
    }

    protected IndexResponse index(String index, String type, String id) {
        return index(index, type, id, Collections.<String, Object>emptyMap());
    }

    protected IndexResponse index(String index, String type, String id, Map<String, Object> doc) {
        return client().prepareIndex(index, type, id).setSource(doc).get();
    }

    protected RefreshResponse refresh() {
        RefreshResponse actionGet = client().admin().indices().prepareRefresh().execute().actionGet();
        assertNoFailures(actionGet);
        return actionGet;
    }

    protected ClientProxy clientProxy() {
        return ClientProxy.of(client());
    }

    protected ScriptServiceProxy scriptService() {
        return getInstanceFromNode(ScriptServiceProxy.class);
    }
}
