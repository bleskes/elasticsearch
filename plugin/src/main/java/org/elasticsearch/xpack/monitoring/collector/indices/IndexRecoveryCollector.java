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

package org.elasticsearch.xpack.monitoring.collector.indices;

import org.elasticsearch.action.admin.indices.recovery.RecoveryResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.xpack.monitoring.MonitoringSettings;
import org.elasticsearch.xpack.monitoring.collector.Collector;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;
import org.elasticsearch.xpack.security.InternalClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Collector for the Recovery API.
 * <p>
 * This collector runs on the master node only and collects a {@link IndexRecoveryMonitoringDoc}
 * document for every index that has on-going shard recoveries.
 */
public class IndexRecoveryCollector extends Collector {

    private final Client client;

    public IndexRecoveryCollector(Settings settings, ClusterService clusterService,
                                  MonitoringSettings monitoringSettings,
                                  XPackLicenseState licenseState, InternalClient client) {
        super(settings, "index-recovery", clusterService, monitoringSettings, licenseState);
        this.client = client;
    }

    @Override
    protected boolean shouldCollect() {
        return super.shouldCollect() && isLocalNodeMaster();
    }

    @Override
    protected Collection<MonitoringDoc> doCollect() throws Exception {
        List<MonitoringDoc> results = new ArrayList<>(1);
        RecoveryResponse recoveryResponse = client.admin().indices().prepareRecoveries()
                .setIndices(monitoringSettings.indices())
                .setIndicesOptions(IndicesOptions.lenientExpandOpen())
                .setActiveOnly(monitoringSettings.recoveryActiveOnly())
                .get(monitoringSettings.recoveryTimeout());

        if (recoveryResponse.hasRecoveries()) {
            results.add(new IndexRecoveryMonitoringDoc(monitoringId(), monitoringVersion(),
                    clusterUUID(), System.currentTimeMillis(), localNode(), recoveryResponse));
        }
        return Collections.unmodifiableCollection(results);
    }
}
