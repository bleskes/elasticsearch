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

package org.elasticsearch.shield;

import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.component.LifecycleListener;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.shield.audit.AuditTrailModule;
import org.elasticsearch.shield.audit.index.IndexAuditTrail;
import org.elasticsearch.shield.authc.esnative.ESNativeUsersStore;
import org.elasticsearch.shield.authz.esnative.ESNativeRolesStore;
import org.elasticsearch.threadpool.ThreadPool;

/**
 * This class is used to provide a lifecycle for services that is based on the cluster's state
 * rather than the typical lifecycle that is used to start services as part of the node startup.
 *
 * This type of lifecycle is necessary for services that need to perform actions that require the cluster to be in a
 * certain state; some examples are storing index templates and creating indices. These actions would most likely fail
 * from within a plugin if executed in the {@link org.elasticsearch.common.component.AbstractLifecycleComponent#doStart()}
 * method. However, if the startup of these services waits for the cluster to form and recover indices then it will be
 * successful. This lifecycle service allows for this to happen by listening for {@link ClusterChangedEvent} and checking
 * if the services can start. Additionally, the service also provides hooks for stop and close functionality.
 */
public class ShieldLifecycleService extends AbstractComponent implements ClusterStateListener {

    private final Settings settings;
    private final ThreadPool threadPool;
    private final IndexAuditTrail indexAuditTrail;
    private final ESNativeUsersStore esUserStore;
    private final ESNativeRolesStore esRolesStore;

    @Inject
    public ShieldLifecycleService(Settings settings, ClusterService clusterService, ThreadPool threadPool,
                                  IndexAuditTrail indexAuditTrail, ESNativeUsersStore esUserStore,
                                  ESNativeRolesStore esRolesStore) {
        super(settings);
        this.settings = settings;
        this.threadPool = threadPool;
        this.indexAuditTrail = indexAuditTrail;
        this.esUserStore = esUserStore;
        this.esRolesStore = esRolesStore;
        clusterService.add(this);
        clusterService.add(esUserStore);
        clusterService.add(esRolesStore);
        clusterService.addLifecycleListener(new LifecycleListener() {

            @Override
            public void beforeStop() {
                stop();
            }

            @Override
            public void beforeClose() {
                close();
            }
        });
    }

    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        final boolean master = event.localNodeMaster();
        try {
            if (esUserStore.canStart(event.state(), master)) {
                threadPool.generic().execute(new AbstractRunnable() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        logger.error("failed to start native user store service", throwable);
                        assert false : "shield lifecycle services startup failed";
                    }

                    @Override
                    public void doRun() {
                        esUserStore.start();
                    }
                });
            }
        } catch (Exception e) {
            logger.error("failed to start native user store", e);
        }

        try {
            if (esRolesStore.canStart(event.state(), master)) {
                threadPool.generic().execute(new AbstractRunnable() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        logger.error("failed to start native roles store services", throwable);
                        assert false : "shield lifecycle services startup failed";
                    }

                    @Override
                    public void doRun() {
                        esRolesStore.start();
                    }
                });
            }
        } catch (Exception e) {
            logger.error("failed to start native roles store", e);
        }

        try {
            if (AuditTrailModule.indexAuditLoggingEnabled(settings) &&
                    indexAuditTrail.state() == IndexAuditTrail.State.INITIALIZED) {
                if (indexAuditTrail.canStart(event, master)) {
                    threadPool.generic().execute(new AbstractRunnable() {

                        @Override
                        public void onFailure(Throwable throwable) {
                            logger.error("failed to start index audit trail services", throwable);
                            assert false : "shield lifecycle services startup failed";
                        }

                        @Override
                        public void doRun() {
                            indexAuditTrail.start(master);
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("failed to start index audit trail", e);
        }

    }

    public void stop() {
        try {
            esUserStore.stop();
        } catch (Exception e) {
            logger.error("failed to stop native user module", e);
        }
        try {
            esRolesStore.stop();
        } catch (Exception e) {
            logger.error("failed to stop native roles module", e);
        }
        try {
            indexAuditTrail.stop();
        } catch (Exception e) {
            logger.error("failed to stop audit trail module", e);
        }
    }

    public void close() {
        // There is no .close() method for the roles module
        try {
            indexAuditTrail.close();
        } catch (Exception e) {
            logger.error("failed to close audit trail module", e);
        }
    }
}
