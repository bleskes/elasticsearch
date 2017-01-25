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

import org.elasticsearch.action.bulk.byscroll.BulkByScrollResponse;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.ReindexAction;
import org.elasticsearch.index.reindex.ReindexPlugin;
import org.elasticsearch.index.reindex.UpdateByQueryAction;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.SecurityIntegTestCase;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ReindexWithSecurityIT extends SecurityIntegTestCase {

    private boolean useSecurity3;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        useSecurity3 = randomBoolean();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        Collection<Class<? extends Plugin>> plugins = new ArrayList<>(super.nodePlugins());
        plugins.add(ReindexPlugin.class);
        return Collections.unmodifiableCollection(plugins);
    }

    @Override
    protected Collection<Class<? extends Plugin>> transportClientPlugins() {
        Collection<Class<? extends Plugin>> plugins = new ArrayList<>(super.nodePlugins());
        plugins.add(ReindexPlugin.class);
        return Collections.unmodifiableCollection(plugins);
    }

    @Override
    protected Settings externalClusterClientSettings() {
        Settings.Builder builder = Settings.builder().put(super.externalClusterClientSettings());
        if (useSecurity3) {
            builder.put(NetworkModule.TRANSPORT_TYPE_KEY, Security.NAME3);
        } else {
            builder.put(NetworkModule.TRANSPORT_TYPE_KEY, Security.NAME4);
        }
        builder.put(Security.USER_SETTING.getKey(), "test_admin:changeme");
        return builder.build();
    }

    public void testDeleteByQuery() {
        createIndicesWithRandomAliases("test1", "test2", "test3");

        BulkByScrollResponse response = DeleteByQueryAction.INSTANCE.newRequestBuilder(client()).source("test1", "test2").get();
        assertNotNull(response);

        response = DeleteByQueryAction.INSTANCE.newRequestBuilder(client()).source("test*").get();
        assertNotNull(response);

        IndexNotFoundException e = expectThrows(IndexNotFoundException.class,
                () -> DeleteByQueryAction.INSTANCE.newRequestBuilder(client()).source("test1", "index1").get());
        assertEquals("no such index", e.getMessage());
    }

    public void testUpdateByQuery() {
        createIndicesWithRandomAliases("test1", "test2", "test3");

        BulkByScrollResponse response = UpdateByQueryAction.INSTANCE.newRequestBuilder(client()).source("test1", "test2").get();
        assertNotNull(response);

        response = UpdateByQueryAction.INSTANCE.newRequestBuilder(client()).source("test*").get();
        assertNotNull(response);

        IndexNotFoundException e = expectThrows(IndexNotFoundException.class,
                () -> UpdateByQueryAction.INSTANCE.newRequestBuilder(client()).source("test1", "index1").get());
        assertEquals("no such index", e.getMessage());
    }

    public void testReindex() {
        createIndicesWithRandomAliases("test1", "test2", "test3", "dest");

        BulkByScrollResponse response = ReindexAction.INSTANCE.newRequestBuilder(client()).source("test1", "test2")
                .destination("dest").get();
        assertNotNull(response);

        response = ReindexAction.INSTANCE.newRequestBuilder(client()).source("test*").destination("dest").get();
        assertNotNull(response);

        IndexNotFoundException e = expectThrows(IndexNotFoundException.class,
                () -> ReindexAction.INSTANCE.newRequestBuilder(client()).source("test1", "index1").destination("dest").get());
        assertEquals("no such index", e.getMessage());
    }
}
