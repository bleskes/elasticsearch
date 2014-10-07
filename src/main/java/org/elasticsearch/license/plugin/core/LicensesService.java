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

package org.elasticsearch.license.plugin.core;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.*;
import org.elasticsearch.cluster.ack.ClusterStateUpdateRequest;
import org.elasticsearch.cluster.ack.ClusterStateUpdateResponse;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.core.LicenseBuilders;
import org.elasticsearch.license.plugin.action.delete.DeleteLicenseRequest;
import org.elasticsearch.license.plugin.action.put.PutLicenseRequest;

/**
 * Service responsible for maintaining and providing access to licenses on nodes.
 *
 * TODO: Work in progress:
 *  - implement logic in clusterChanged
 *  - interface with LicenseManager
 */
public class LicensesService extends AbstractLifecycleComponent<LicensesService> implements ClusterStateListener {

   // private final Injector injector;

    private final ClusterService clusterService;

    //private volatile ESLicenses licenses = null;//ImmutableMap.of();

    @Inject
    public LicensesService(Settings settings, ClusterService clusterService/*, Injector injector*/) {
        super(settings);
        //this.injector = injector;
        this.clusterService = clusterService;
        // Doesn't make sense to maintain repositories on non-master and non-data nodes
        // Nothing happens there anyway
        if (DiscoveryNode.dataNode(settings) || DiscoveryNode.masterNode(settings)) {
            clusterService.add(this);
        }
    }

    /**
     * Registers new licenses in the cluster
     * <p/>
     * This method can be only called on the master node. It tries to create a new licenses on the master
     * and if it was successful it adds the license to cluster metadata.
     */
    public void registerLicenses(final String source, final PutLicenseRequest request, final ActionListener<ClusterStateUpdateResponse> listener) {
        final LicensesMetaData newLicenseMetaData = new LicensesMetaData(request.license());
        //TODO: add a source field to request
        clusterService.submitStateUpdateTask(source, new AckedClusterStateUpdateTask<ClusterStateUpdateResponse>(request, listener) {
            @Override
            protected ClusterStateUpdateResponse newResponse(boolean acknowledged) {
                return new ClusterStateUpdateResponse(acknowledged);
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                // TODO check if newLicenseMetaData actually needs a cluster update
                MetaData metaData = currentState.metaData();
                MetaData.Builder mdBuilder = MetaData.builder(currentState.metaData());
                LicensesMetaData currentLicenses = metaData.custom(LicensesMetaData.TYPE);

                if (currentLicenses == null) {
                    // no licenses were registered
                    currentLicenses = newLicenseMetaData;
                } else {
                    // merge previous license with new one
                    currentLicenses = new LicensesMetaData(LicenseBuilders.merge(currentLicenses, newLicenseMetaData));
                }
                mdBuilder.putCustom(LicensesMetaData.TYPE, currentLicenses);
                return ClusterState.builder(currentState).metaData(mdBuilder).build();
            }
        });

    }

    //TODO
    public void unregisteredLicenses(final String source, final DeleteLicenseRequest request, final ActionListener<ClusterStateUpdateResponse> listener) {
        clusterService.submitStateUpdateTask(source, new AckedClusterStateUpdateTask<ClusterStateUpdateResponse>(request, listener) {
            @Override
            protected ClusterStateUpdateResponse newResponse(boolean acknowledged) {
                return new ClusterStateUpdateResponse(acknowledged);
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                // TODO check if newLicenseMetaData actually needs a cluster update
                MetaData metaData = currentState.metaData();
                MetaData.Builder mdBuilder = MetaData.builder(currentState.metaData());
                LicensesMetaData currentLicenses = metaData.custom(LicensesMetaData.TYPE);

                //TODO: implement deletion
                if (currentLicenses == null) {
                    // no licenses were registered
                    //currentLicenses = newLicenseMetaData;
                } else {
                    // merge previous license with new one
                    //currentLicenses = new LicensesMetaData(LicenseBuilders.merge(currentLicenses, newLicenseMetaData));
                }
                mdBuilder.putCustom(LicensesMetaData.TYPE, currentLicenses);
                return ClusterState.builder(currentState).metaData(mdBuilder).build();
            }
        });
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        //TODO
    }

    @Override
    protected void doStop() throws ElasticsearchException {
        //TODO
    }

    @Override
    protected void doClose() throws ElasticsearchException {
        //TODO
    }

    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        //TODO
    }
}
