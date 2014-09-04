/*
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

package org.elasticsearch.discovery.raft;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.discovery.ClusterDiscoveryConfiguration;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

@ElasticsearchIntegrationTest.ClusterScope(scope = ElasticsearchIntegrationTest.Scope.TEST, numDataNodes = 0)
@TestLogging("cluster.service:TRACE,discovery:TRACE,indices.cluster:TRACE")
public class RaftDiscoveryTests extends ElasticsearchIntegrationTest {

    private ClusterDiscoveryConfiguration discoveryConfig;

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return discoveryConfig.node(nodeOrdinal);
    }

    @Before
    public void clearConfig() {
        discoveryConfig = null;
    }

    @Test
    public void testNormalClusterForming() throws ExecutionException, InterruptedException {
        int currentNumNodes = randomIntBetween(3, 5);
        discoveryConfig = new ClusterDiscoveryConfiguration.Raft(currentNumNodes);

        internalCluster().startNodesAsync(currentNumNodes).get();

        if (client().admin().cluster().prepareHealth().setWaitForNodes("" + currentNumNodes).get().isTimedOut()) {
            logger.info("cluster forming timed out, cluster state:\n{}", client().admin().cluster().prepareState().get().getState().prettyPrint());
            fail("timed out waiting for cluster to form with [" + currentNumNodes + "] nodes");
        }
    }
}

