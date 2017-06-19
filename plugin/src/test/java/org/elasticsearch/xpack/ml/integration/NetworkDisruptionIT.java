/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.integration;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.discovery.DiscoverySettings;
import org.elasticsearch.discovery.zen.FaultDetection;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.discovery.TestZenDiscovery;
import org.elasticsearch.test.disruption.NetworkDisruption;
import org.elasticsearch.test.transport.MockTransportService;
import org.elasticsearch.xpack.ml.action.CloseJobAction;
import org.elasticsearch.xpack.ml.action.OpenJobAction;
import org.elasticsearch.xpack.ml.action.PutJobAction;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.persistence.AnomalyDetectorsIndex;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.Quantiles;
import org.elasticsearch.xpack.ml.support.BaseMlIntegTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ESIntegTestCase.SuppressLocalMode
public class NetworkDisruptionIT extends BaseMlIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder().put(super.nodeSettings(nodeOrdinal))
                .put(TestZenDiscovery.USE_MOCK_PINGS.getKey(), false)
                .put(FaultDetection.PING_TIMEOUT_SETTING.getKey(), "1s") // for hitting simulated network failures quickly
                .put(FaultDetection.PING_RETRIES_SETTING.getKey(), "1") // for hitting simulated network failures quickly
                .put(DiscoverySettings.PUBLISH_TIMEOUT_SETTING.getKey(), "1s") // for hitting simulated network failures quickly
                .put("discovery.zen.join_timeout", "10s")  // still long to induce failures but not too long so test won't time out
                .build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        Collection<Class<? extends Plugin>> plugins = new ArrayList<>(super.nodePlugins());
        plugins.add(MockTransportService.TestPlugin.class);
        return plugins;
    }

    public void testJobRelocation() throws Exception {
        internalCluster().ensureAtLeastNumDataNodes(5);
        ensureStableCluster(5);

        Job.Builder job = createJob("relocation-job");
        PutJobAction.Request putJobRequest = new PutJobAction.Request(job);
        PutJobAction.Response putJobResponse = client().execute(PutJobAction.INSTANCE, putJobRequest).actionGet();
        assertTrue(putJobResponse.isAcknowledged());
        ensureGreen();

        OpenJobAction.Request openJobRequest = new OpenJobAction.Request(job.getId());
        OpenJobAction.Response openJobResponse = client().execute(OpenJobAction.INSTANCE, openJobRequest).actionGet();
        assertTrue(openJobResponse.isAcknowledged());

        // Record which node the job starts off on
        String origJobNode = awaitJobOpenedAndAssigned(job.getId(), null);

        // Isolate the node the job is running on from the cluster
        Set<String> isolatedSide = Collections.singleton(origJobNode);
        Set<String> restOfClusterSide = new HashSet<>(Arrays.asList(internalCluster().getNodeNames()));
        restOfClusterSide.remove(origJobNode);
        String notIsolatedNode = restOfClusterSide.iterator().next();

        NetworkDisruption networkDisruption = new NetworkDisruption(new NetworkDisruption.TwoPartitions(isolatedSide, restOfClusterSide),
                new NetworkDisruption.NetworkDisconnect());
        internalCluster().setDisruptionScheme(networkDisruption);
        networkDisruption.startDisrupting();
        ensureStableCluster(4, notIsolatedNode);

        // Job should move to a new node in the bigger portion of the cluster
        String newJobNode = awaitJobOpenedAndAssigned(job.getId(), notIsolatedNode);
        assertNotEquals(origJobNode, newJobNode);

        networkDisruption.removeAndEnsureHealthy(internalCluster());
        ensureGreen();

        // Job should remain running on the new node, not the one that temporarily detached from the cluster
        String finalJobNode = awaitJobOpenedAndAssigned(job.getId(), null);
        assertEquals(newJobNode, finalJobNode);

        // The job running on the original node should have been killed, and hence should not have persisted quantiles
        SearchResponse searchResponse = client().prepareSearch(AnomalyDetectorsIndex.jobStateIndexName())
                .setQuery(QueryBuilders.idsQuery().addIds(Quantiles.documentId(job.getId())))
                .setIndicesOptions(IndicesOptions.lenientExpandOpen()).execute().actionGet();
        assertEquals(0L, searchResponse.getHits().getTotalHits());

        CloseJobAction.Request closeJobRequest = new CloseJobAction.Request(job.getId());
        CloseJobAction.Response closeJobResponse = client().execute(CloseJobAction.INSTANCE, closeJobRequest).actionGet();
        assertTrue(closeJobResponse.isClosed());

        // The relocated job was closed rather than killed, and hence should have persisted quantiles
        searchResponse = client().prepareSearch(AnomalyDetectorsIndex.jobStateIndexName())
                .setQuery(QueryBuilders.idsQuery().addIds(Quantiles.documentId(job.getId())))
                .setIndicesOptions(IndicesOptions.lenientExpandOpen()).execute().actionGet();
        assertEquals(1L, searchResponse.getHits().getTotalHits());
    }
}
