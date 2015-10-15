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

package org.elasticsearch.marvel.agent.collector.cluster;

import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.core.LicensesManagerService;
import org.elasticsearch.marvel.agent.collector.AbstractCollectorTestCase;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.marvel.license.MarvelLicensee;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.Matchers.*;

public class ClusterInfoCollectorTests extends AbstractCollectorTestCase {

    @Test
    public void testClusterInfoCollector() throws Exception {
        Collection<MarvelDoc> results = newClusterInfoCollector().doCollect();
        assertThat(results, hasSize(1));

        MarvelDoc marvelDoc = results.iterator().next();
        assertNotNull(marvelDoc);
        assertThat(marvelDoc, instanceOf(ClusterInfoMarvelDoc.class));

        ClusterInfoMarvelDoc clusterInfoMarvelDoc = (ClusterInfoMarvelDoc) marvelDoc;
        assertThat(clusterInfoMarvelDoc.clusterUUID(), equalTo(client().admin().cluster().prepareState().setMetaData(true).get().getState().metaData().clusterUUID()));
        assertThat(clusterInfoMarvelDoc.timestamp(), greaterThan(0L));
        assertThat(clusterInfoMarvelDoc.type(), equalTo(ClusterInfoCollector.TYPE));

        assertThat(clusterInfoMarvelDoc.getClusterName(), equalTo(client().admin().cluster().prepareState().setMetaData(true).get().getClusterName().value()));
        assertThat(clusterInfoMarvelDoc.getVersion(), equalTo(client().admin().cluster().prepareNodesInfo().get().getNodes()[0].getVersion().toString()));

        assertThat(clusterInfoMarvelDoc.getLicense(), notNullValue());

        assertNotNull(clusterInfoMarvelDoc.getClusterStats());
        assertThat(clusterInfoMarvelDoc.getClusterStats().getNodesStats().getCounts().getTotal(), equalTo(internalCluster().getNodeNames().length));
    }

    @Test
    public void testClusterInfoCollectorWithLicensing() {
        try {
            String[] nodes = internalCluster().getNodeNames();
            for (String node : nodes) {
                logger.debug("--> creating a new instance of the collector");
                ClusterInfoCollector collector = newClusterInfoCollector(node);
                assertNotNull(collector);

                logger.debug("--> enabling license and checks that the collector can collect data (if node is master)");
                enableLicense();
                if (node.equals(internalCluster().getMasterName())) {
                    assertCanCollect(collector);
                } else {
                    assertCannotCollect(collector);
                }

                logger.debug("--> starting graceful period and checks that the collector can still collect data (if node is master)");
                beginGracefulPeriod();
                if (node.equals(internalCluster().getMasterName())) {
                    assertCanCollect(collector);
                } else {
                    assertCannotCollect(collector);
                }

                logger.debug("--> ending graceful period and checks that the collector can still collect data (if node is master)");
                endGracefulPeriod();
                if (node.equals(internalCluster().getMasterName())) {
                    assertCanCollect(collector);
                } else {
                    assertCannotCollect(collector);
                }

                logger.debug("--> disabling license and checks that the collector can still collect data (if node is master)");
                disableLicense();
                if (node.equals(internalCluster().getMasterName())) {
                    assertCanCollect(collector);
                } else {
                    assertCannotCollect(collector);
                }
            }
        } finally {
            // Ensure license is enabled before finishing the test
            enableLicense();
        }
    }

    private ClusterInfoCollector newClusterInfoCollector() {
        // This collector runs on master node only
        return newClusterInfoCollector(internalCluster().getMasterName());
    }

    private ClusterInfoCollector newClusterInfoCollector(String nodeId) {
        assertNotNull(nodeId);
        return new ClusterInfoCollector(internalCluster().getInstance(Settings.class, nodeId),
                internalCluster().getInstance(ClusterService.class, nodeId),
                internalCluster().getInstance(MarvelSettings.class, nodeId),
                internalCluster().getInstance(MarvelLicensee.class, nodeId),
                internalCluster().getInstance(LicensesManagerService.class, nodeId),
                internalCluster().getInstance(ClusterName.class, nodeId),
                securedClient(nodeId));
    }
}
