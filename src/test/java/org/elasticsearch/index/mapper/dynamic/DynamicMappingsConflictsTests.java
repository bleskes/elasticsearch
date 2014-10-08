package org.elasticsearch.index.mapper.dynamic;/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.routing.operation.hash.HashFunction;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.service.IndexService;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@ClusterScope(minNumDataNodes = 2)
public class DynamicMappingsConflictsTests extends ElasticsearchIntegrationTest {

    @Test
    public void testConflictingConcurrentUpdates() throws Exception {
        int numOfShards = cluster().numDataNodes();
        prepareCreate("test").setSettings(IndexMetaData.SETTING_NUMBER_OF_SHARDS, numOfShards, IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0).get();
        ensureGreen();

        HashFunction hashFunction = internalCluster().getInstance(HashFunction.class);

        String[] idPerShard = new String[numOfShards];
        for (int i = 0; i < idPerShard.length; i++) {
            for (int id = 0; idPerShard[i] == null; id++) {
                String s = Integer.toString(id);
                if (hashFunction.hash(s) % numOfShards == i) {
                    idPerShard[i] = s;
                }
            }
        }

        Thread[] indexThreads = new Thread[numOfShards];
        final CyclicBarrier barrier = new CyclicBarrier(numOfShards + 1);
        for (int t = 0; t < indexThreads.length; t++) {
            final int finalT = t;
            indexThreads[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    IndexRequestBuilder request = client().prepareIndex("test", "type").setSource("f", finalT % 2 == 0 ? "s" : 1);
                    try {
                        barrier.await();
                        request.get();
                        logger.info("succeeded indexing {}", finalT % 2 == 0 ? "s" : 1);
                    } catch (Exception e) {
                        logger.info("exception in indexing thread", e);
                    }
                }
            });
            indexThreads[t].start();
        }
        logger.info("waiting on indexing shards to start");
        barrier.await();
        logger.info("waiting on indexing shards to finish");
        for (Thread t : indexThreads) {
            t.join();
        }
        assertBusy(new Runnable() {
            @Override
            public void run() {
                Set<String> nodes = internalCluster().nodesInclude("test");
                Set<String> fieldTypes = new HashSet<>();
                for (String node : nodes) {
                    IndicesService indicesService = internalCluster().getInstance(IndicesService.class, node);
                    IndexService indexService = indicesService.indexService("test");
                    assertThat("index service doesn't exists on " + node, indexService, notNullValue());
                    DocumentMapper documentMapper = indexService.mapperService().documentMapper("type");
                    assertThat("document mapper doesn't exists on " + node, documentMapper, notNullValue());
                    fieldTypes.add(documentMapper.mappers().fullName("f").mapper().getClass().getSimpleName());
                }
                assertThat("found types: " + fieldTypes.toString(), fieldTypes.size(), equalTo(1));
            }
        });
    }
}
