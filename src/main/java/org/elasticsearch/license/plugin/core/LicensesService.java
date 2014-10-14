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
import org.elasticsearch.cluster.ack.ClusterStateUpdateResponse;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Singleton;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.core.ESLicenses;
import org.elasticsearch.license.core.LicenseBuilders;
import org.elasticsearch.license.manager.ESLicenseManager;
import org.elasticsearch.license.plugin.action.delete.DeleteLicenseRequest;
import org.elasticsearch.license.plugin.action.put.PutLicenseRequest;
import org.elasticsearch.license.plugin.core.trial.TrialLicenses;
import org.elasticsearch.license.plugin.core.trial.TrialLicensesBuilder;

import java.util.HashSet;
import java.util.Set;

import static org.elasticsearch.license.core.ESLicenses.FeatureType;
import static org.elasticsearch.license.plugin.core.trial.TrialLicenses.TrialLicense;
import static org.elasticsearch.license.plugin.core.trial.TrialLicensesBuilder.EMPTY;

/**
 * Service responsible for maintaining and providing access to licenses on nodes.
 *
 * TODO: Work in progress:
 *  - implement logic in clusterChanged
 *  - interface with LicenseManager
 */
@Singleton
public class LicensesService extends AbstractLifecycleComponent<LicensesService> implements ClusterStateListener, LicensesManagerService, LicensesClientService {

    private ESLicenseManager esLicenseManager;

    private ClusterService clusterService;

    private volatile TrialLicenses trialLicenses = EMPTY;

    @Inject
    public LicensesService(Settings settings, ClusterService clusterService) {
        super(settings);
        this.clusterService = clusterService;
        this.esLicenseManager = ESLicenseManager.createClusterStateBasedInstance(clusterService);
    }

    /**
     * Registers new licenses in the cluster
     * <p/>
     * This method can be only called on the master node. It tries to create a new licenses on the master
     * and if it was successful it adds the license to cluster metadata.
     */
    @Override
    public void registerLicenses(final PutLicenseRequestHolder requestHolder, final ActionListener<ClusterStateUpdateResponse> listener) {
        final PutLicenseRequest request = requestHolder.request;
        final ESLicenses newLicenses = request.license();
        clusterService.submitStateUpdateTask(requestHolder.source, new AckedClusterStateUpdateTask<ClusterStateUpdateResponse>(request, listener) {
            @Override
            protected ClusterStateUpdateResponse newResponse(boolean acknowledged) {
                return new ClusterStateUpdateResponse(acknowledged);
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                MetaData metaData = currentState.metaData();
                MetaData.Builder mdBuilder = MetaData.builder(currentState.metaData());
                LicensesMetaData currentLicenses = metaData.custom(LicensesMetaData.TYPE);

                esLicenseManager.verifyLicenses(newLicenses);

                if (currentLicenses == null) {
                    // no licenses were registered
                    currentLicenses = new LicensesMetaData(newLicenses, null);
                } else {
                    // merge previous license with new one
                    ESLicenses mergedLicenses = LicenseBuilders.merge(currentLicenses.getLicenses(), newLicenses);
                    currentLicenses = new LicensesMetaData(mergedLicenses, currentLicenses.getTrialLicenses());
                }

                mdBuilder.putCustom(LicensesMetaData.TYPE, currentLicenses);
                return ClusterState.builder(currentState).metaData(mdBuilder).build();
            }
        });

    }

    @Override
    public void unregisterLicenses(final DeleteLicenseRequestHolder requestHolder, final ActionListener<ClusterStateUpdateResponse> listener) {
        final DeleteLicenseRequest request = requestHolder.request;
        final Set<FeatureType> featuresToDelete = asFeatureTypes(request.features());
        clusterService.submitStateUpdateTask(requestHolder.source, new AckedClusterStateUpdateTask<ClusterStateUpdateResponse>(request, listener) {
            @Override
            protected ClusterStateUpdateResponse newResponse(boolean acknowledged) {
                return new ClusterStateUpdateResponse(acknowledged);
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                MetaData metaData = currentState.metaData();
                MetaData.Builder mdBuilder = MetaData.builder(currentState.metaData());
                LicensesMetaData currentLicenses = metaData.custom(LicensesMetaData.TYPE);

                if (currentLicenses != null) {
                    final ESLicenses newLicenses = LicenseBuilders.removeFeatures(currentLicenses.getLicenses(), featuresToDelete);
                    currentLicenses = new LicensesMetaData(newLicenses, currentLicenses.getTrialLicenses());
                }
                mdBuilder.putCustom(LicensesMetaData.TYPE, currentLicenses);
                return ClusterState.builder(currentState).metaData(mdBuilder).build();
            }
        });
    }

    //TODO: hook this up
    private void registerTrialLicense(final TrialLicense trialLicense) {
        clusterService.submitStateUpdateTask("register trial license []", new ProcessedClusterStateUpdateTask() {
            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {

            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                MetaData metaData = currentState.metaData();
                MetaData.Builder mdBuilder = MetaData.builder(currentState.metaData());
                LicensesMetaData currentLicenses = metaData.custom(LicensesMetaData.TYPE);
                TrialLicensesBuilder trialLicensesBuilder = TrialLicensesBuilder.trialLicensesBuilder().license(trialLicense);
                if (currentLicenses != null) {
                    if (currentLicenses.getTrialLicenses() != null) {
                        // had previous trial licenses
                        if (trialLicenseCheck(trialLicense.feature().string())) {
                            trialLicensesBuilder = trialLicensesBuilder.licenses(currentLicenses.getTrialLicenses());
                            currentLicenses = new LicensesMetaData(currentLicenses.getLicenses(), trialLicensesBuilder.build());
                        } else {
                            // had previously generated trial license for the feature
                            currentLicenses = new LicensesMetaData(currentLicenses.getLicenses(), currentLicenses.getTrialLicenses());
                        }
                    } else {
                        // had no previous trial license
                        currentLicenses = new LicensesMetaData(currentLicenses.getLicenses(), trialLicensesBuilder.build());
                    }
                } else {
                    // had no license meta data
                    currentLicenses = new LicensesMetaData(null, trialLicensesBuilder.build());
                }
                trialLicenses = currentLicenses.getTrialLicenses();
                mdBuilder.putCustom(LicensesMetaData.TYPE, currentLicenses);
                return ClusterState.builder(currentState).metaData(mdBuilder).build();
            }

            @Override
            public void onFailure(String source, @Nullable Throwable t) {

            }

            private boolean trialLicenseCheck(String feature) {
                final TrialLicense license = trialLicenses.getTrialLicense(FeatureType.fromString(feature));
                return license == null;
            }
        });
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        if ( DiscoveryNode.dataNode(settings) || DiscoveryNode.masterNode(settings)) {
            clusterService.add(this);
        }
    }

    @Override
    protected void doStop() throws ElasticsearchException {
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }

    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        //TODO

        // check for registered plugin
        // if appropriate registered plugin is found; push one-time trial license

        // generate one-time trial license
        // registerTrialLicense(generateTrialLicense(feature, 30, 1000));

        // check for cluster status (recovery)
        // switch validation enforcement
    }

    @Override
    public void registerAndActivate(String feature, TrialLicenseOptions trialLicenseOptions, Listener listener) {
        // check if one-time trial license has been generated for this feature

        // if not call registerTrialLicense()

        // if trial license generated, onEnabled should return

        // else check actual licenses

        TrialLicense trialLicense = generateTrialLicense(feature, trialLicenseOptions.durationInDays, trialLicenseOptions.maxNodes);
        registerTrialLicense(trialLicense);
    }

    public boolean checkLicenseExpiry(String feature) {
        //TODO make validation cluster state aware
        //check trial license existence
        // if found; use it to do the check

        final TrialLicense trialLicense = trialLicenses.getTrialLicense(FeatureType.fromString(feature));
        if (trialLicense != null) {
            return trialLicense.expiryDate() > System.currentTimeMillis();
        }
        return esLicenseManager.hasLicenseForFeature(FeatureType.fromString(feature));
    }

    private static Set<FeatureType> asFeatureTypes(Set<String> featureTypeStrings) {
        Set<FeatureType> featureTypes = new HashSet<>(featureTypeStrings.size());
        for (String featureString : featureTypeStrings) {
            featureTypes.add(FeatureType.fromString(featureString));
        }
        return featureTypes;
    }

    public static class PutLicenseRequestHolder {
        private final PutLicenseRequest request;
        private final String source;

        public PutLicenseRequestHolder(PutLicenseRequest request, String source) {
            this.request = request;
            this.source = source;
        }
    }

    public static class DeleteLicenseRequestHolder {
        private final DeleteLicenseRequest request;
        private final String source;

        public DeleteLicenseRequestHolder(DeleteLicenseRequest request, String source) {
            this.request = request;
            this.source = source;
        }
    }

    private TrialLicense generateTrialLicense(String feature, int durationInDays, int maxNodes) {
        return TrialLicensesBuilder.trialLicenseBuilder()
                .issuedTo(clusterService.state().getClusterName().value())
                .issueDate(System.currentTimeMillis())
                .durationInDays(durationInDays)
                .feature(FeatureType.fromString(feature))
                .maxNodes(maxNodes)
                .build();
    }


    public static class TrialLicenseOptions {
        final int durationInDays;
        final int maxNodes;

        public TrialLicenseOptions(int durationInDays, int maxNodes) {
            this.durationInDays = durationInDays;
            this.maxNodes = maxNodes;
        }
    }
}
