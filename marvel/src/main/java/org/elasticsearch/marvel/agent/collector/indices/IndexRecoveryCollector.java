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

package org.elasticsearch.marvel.agent.collector.indices;

import org.elasticsearch.action.admin.indices.recovery.RecoveryResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.agent.collector.AbstractCollector;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.marvel.license.LicenseService;
import org.elasticsearch.marvel.shield.SecuredClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Collector for the Recovery API.
 * <p>
 * This collector runs on the master node only and collects a {@link IndexRecoveryMarvelDoc} document
 * for every index that has on-going shard recoveries.
 */
public class IndexRecoveryCollector extends AbstractCollector<IndexRecoveryCollector> {

    public static final String NAME = "index-recovery-collector";
    public static final String TYPE = "index_recovery";

    private final Client client;

    @Inject
    public IndexRecoveryCollector(Settings settings, ClusterService clusterService, MarvelSettings marvelSettings,  LicenseService licenseService,
                                  SecuredClient client) {
        super(settings, NAME, clusterService, marvelSettings, licenseService);
        this.client = client;
    }

    @Override
    protected boolean canCollect() {
        return super.canCollect() && isLocalNodeMaster();
    }

    @Override
    protected Collection<MarvelDoc> doCollect() throws Exception {
        List<MarvelDoc> results = new ArrayList<>(1);

        RecoveryResponse recoveryResponse = client.admin().indices().prepareRecoveries()
                .setIndices(marvelSettings.indices())
                .setIndicesOptions(IndicesOptions.lenientExpandOpen())
                .setActiveOnly(marvelSettings.recoveryActiveOnly())
                .get(marvelSettings.recoveryTimeout());

        if (recoveryResponse.hasRecoveries()) {
            results.add(new IndexRecoveryMarvelDoc(clusterUUID(), TYPE, System.currentTimeMillis(), recoveryResponse));
        }
        return Collections.unmodifiableCollection(results);
    }
}
