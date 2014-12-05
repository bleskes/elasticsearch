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

package org.elasticsearch.alerts;


import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.alerts.actions.AlertActionEntry;
import org.elasticsearch.alerts.actions.AlertActionManager;
import org.elasticsearch.alerts.actions.AlertActionRegistry;
import org.elasticsearch.alerts.scheduler.AlertScheduler;
import org.elasticsearch.alerts.triggers.TriggerManager;
import org.elasticsearch.alerts.triggers.TriggerResult;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.component.LifecycleListener;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.KeyedLock;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.gateway.GatewayService;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class AlertManager extends AbstractComponent {

    private final AlertScheduler scheduler;
    private final AlertsStore alertsStore;
    private final TriggerManager triggerManager;
    private final AlertActionManager actionManager;
    private final AlertActionRegistry actionRegistry;
    private final ThreadPool threadPool;
    private final ClusterService clusterService;
    private final ScriptService scriptService;
    private final Client client;
    private final KeyedLock<String> alertLock = new KeyedLock<>();
    private final AtomicReference<State> state = new AtomicReference<>(State.STOPPED);

    private volatile boolean manuallyStopped;

    @Inject
    public AlertManager(Settings settings, ClusterService clusterService, AlertScheduler scheduler, AlertsStore alertsStore,
                        IndicesService indicesService, TriggerManager triggerManager, AlertActionManager actionManager,
                        AlertActionRegistry actionRegistry, ThreadPool threadPool, ScriptService scriptService, Client client) {
        super(settings);
        this.scheduler = scheduler;
        this.threadPool = threadPool;
        this.scheduler.setAlertManager(this);
        this.alertsStore = alertsStore;
        this.triggerManager = triggerManager;
        this.actionManager = actionManager;
        this.actionManager.setAlertManager(this);
        this.actionRegistry = actionRegistry;
        this.clusterService = clusterService;

        this.scriptService = scriptService;
        this.client = client;

        clusterService.add(new AlertsClusterStateListener());
        // Close if the indices service is being stopped, so we don't run into search failures (locally) that will
        // happen because we're shutting down and an alert is scheduled.
        indicesService.addLifecycleListener(new LifecycleListener() {
            @Override
            public void beforeStop() {
                stop();
            }
        });
        manuallyStopped = !settings.getAsBoolean("alerts.start_immediately",  true);
    }

    public DeleteResponse deleteAlert(String name) throws InterruptedException, ExecutionException {
        ensureStarted();
        alertLock.acquire(name);
        try {
            DeleteResponse deleteResponse = alertsStore.deleteAlert(name);
            if (deleteResponse.isFound()) {
                scheduler.remove(name);
            }
            return deleteResponse;
        } finally {
            alertLock.release(name);
        }
    }

    public IndexResponse putAlert(String alertName, BytesReference alertSource) {
        ensureStarted();
        alertLock.acquire(alertName);
        try {
            AlertsStore.AlertStoreModification result = alertsStore.putAlert(alertName, alertSource);
            if (result.getPrevious() == null) {
                scheduler.schedule(alertName, result.getCurrent().getSchedule());
            } else if (!result.getPrevious().getSchedule().equals(result.getCurrent().getSchedule())) {
                scheduler.schedule(alertName, result.getCurrent().getSchedule());
            }
            return result.getIndexResponse();
        } finally {
            alertLock.release(alertName);
        }
    }

    public State getState() {
        return state.get();
    }

    public void scheduleAlert(String alertName, DateTime scheduledFireTime, DateTime fireTime){
        ensureStarted();
        alertLock.acquire(alertName);
        try {
            Alert alert = alertsStore.getAlert(alertName);
            if (alert == null) {
                logger.warn("Unable to find [{}] in the alert store, perhaps it has been deleted", alertName);
                return;
            }

            try {
                actionManager.addAlertAction(alert, scheduledFireTime, fireTime);
            } catch (Exception e) {
                logger.error("Failed to schedule alert action for [{}]", e, alert);
            }
        } finally {
            alertLock.release(alertName);
        }
    }

    public TriggerResult executeAlert(AlertActionEntry entry) throws IOException {
        ensureStarted();
        alertLock.acquire(entry.getAlertName());
        try {
            Alert alert = alertsStore.getAlert(entry.getAlertName());
            if (alert == null) {
                throw new ElasticsearchException("Alert is not available");
            }
            TriggerResult triggerResult = triggerManager.isTriggered(alert, entry.getScheduledTime(), entry.getFireTime());

            if (triggerResult.isTriggered()) {
                triggerResult.setThrottled(isActionThrottled(alert));
                if (!triggerResult.isThrottled()) {
                    if (alert.getPayloadSearchRequest() != null) {
                        SearchRequest payloadRequest = AlertUtils.createSearchRequestWithTimes(alert.getPayloadSearchRequest(), entry.getScheduledTime(), entry.getFireTime(), scriptService);
                        SearchResponse payloadResponse = client.search(payloadRequest).actionGet();
                        triggerResult.setPayloadRequest(payloadRequest);
                        XContentBuilder builder = jsonBuilder().startObject().value(payloadResponse).endObject();
                        Map<String, Object> responseMap = XContentHelper.convertToMap(builder.bytes(), false).v2();
                        triggerResult.setPayloadResponse(responseMap);
                    }
                    actionRegistry.doAction(alert, triggerResult);
                    alert.setTimeLastActionExecuted(entry.getScheduledTime());
                    if (alert.getAckState() == AlertAckState.NOT_TRIGGERED) {
                        alert.setAckState(AlertAckState.NEEDS_ACK);
                    }
                }
            } else if (alert.getAckState() == AlertAckState.ACKED) {
                alert.setAckState(AlertAckState.NOT_TRIGGERED);
            }
            alert.lastExecuteTime(entry.getFireTime());
            alertsStore.updateAlert(alert);
            return triggerResult;
        } finally {
            alertLock.release(entry.getAlertName());
        }
    }

    /**
     * Acks the alert if needed
     */
    public AlertAckState ackAlert(String alertName) {
        ensureStarted();
        alertLock.acquire(alertName);
        try {
            Alert alert = alertsStore.getAlert(alertName);
            if (alert == null) {
                throw new ElasticsearchException("Alert does not exist [" + alertName + "]");
            }
            if (alert.getAckState() == AlertAckState.NEEDS_ACK) {
                alert.setAckState(AlertAckState.ACKED);
                try {
                    alertsStore.updateAlert(alert);
                } catch (IOException ioe) {
                    throw new ElasticsearchException("Failed to update the alert", ioe);
                }
                return AlertAckState.ACKED;
            } else {
                return alert.getAckState();
            }
        } finally {
            alertLock.release(alertName);
        }
    }

    /**
     * Manually starts alerting if not already started
     */
    public void start() {
        manuallyStopped = false;
        logger.info("Starting alert manager...");
        ClusterState state = clusterService.state();
        internalStart(state);
    }

    /**
     * Manually stops alerting if not already stopped.
     */
    public void stop() {
        manuallyStopped = true;
        internalStop();
    }

    private void internalStop() {
        if (state.compareAndSet(State.STARTED, State.STOPPING)) {
            logger.info("Stopping alert manager...");
            while (true) {
                // It can happen we have still ongoing operations and we wait those operations to finish to avoid
                // that AlertManager or any of its components end up in a illegal state after the state as been set to stopped.
                //
                // For example: An alert action entry may be added while we stopping alerting if we don't wait for
                // ongoing operations to complete. Resulting in once the alert manager starts again that more than
                // expected alert action entries are processed.
                //
                // Note: new operations will fail now because the state has been set to: stopping
                if (!alertLock.hasLockedKeys()) {
                    break;
                }
            }

            actionManager.stop();
            scheduler.stop();
            alertsStore.stop();
            state.set(State.STOPPED);
            logger.info("Alert manager has stopped");
        }
    }

    private void internalStart(ClusterState initialState) {
        if (state.compareAndSet(State.STOPPED, State.STARTING)) {
            ClusterState clusterState = initialState;

            // Try to load alert store before the action manager, b/c action depends on alert store
            while (true) {
                if (alertsStore.start(clusterState)) {
                    break;
                }
                clusterState = newClusterState(clusterState);
            }
            while (true) {
                if (actionManager.start(clusterState)) {
                    break;
                }
                clusterState = newClusterState(clusterState);
            }

            scheduler.start(alertsStore.getAlerts());
            state.set(State.STARTED);
            logger.info("Alert manager has started");
        }
    }

    private void ensureStarted() {
        if (state.get() != State.STARTED) {
            throw new ElasticsearchIllegalStateException("not started");
        }
    }

    public long getNumberOfAlerts() {
        return alertsStore.getAlerts().size();
    }

    /**
     * Return once a cluster state version appears that is never than the version
     */
    private ClusterState newClusterState(ClusterState previous) {
        ClusterState current;
        while (true) {
            current = clusterService.state();
            if (current.getVersion() > previous.getVersion()) {
                return current;
            }
        }
    }

    private boolean isActionThrottled(Alert alert) {
        if (alert.getThrottlePeriod() != null && alert.getTimeLastActionExecuted() != null) {
            TimeValue timeSinceLastExeuted = new TimeValue((new DateTime()).getMillis() - alert.getTimeLastActionExecuted().getMillis());
            if (timeSinceLastExeuted.getMillis() <= alert.getThrottlePeriod().getMillis()) {
                return true;
            }
        }
        return alert.getAckState() == AlertAckState.ACKED;
    }

    private final class AlertsClusterStateListener implements ClusterStateListener {

        @Override
        public void clusterChanged(final ClusterChangedEvent event) {
            if (!event.localNodeMaster()) {
                // We're no longer the master so we need to stop alerting.
                // Stopping alerting may take a while since it will wait on the scheduler to complete shutdown,
                // so we fork here so that we don't wait too long. Other events may need to be processed and
                // other cluster state listeners may need to be executed as well for this event.
                threadPool.executor(ThreadPool.Names.GENERIC).execute(new Runnable() {
                    @Override
                    public void run() {
                        internalStop();
                    }
                });
            } else {
                if (event.state().blocks().hasGlobalBlock(GatewayService.STATE_NOT_RECOVERED_BLOCK)) {
                    // wait until the gateway has recovered from disk, otherwise we think may not have .alerts and
                    // a .alertshistory index, but they may not have been restored from the cluster state on disk
                    return;
                }
                if (state.get() == State.STOPPED && !manuallyStopped) {
                    threadPool.executor(ThreadPool.Names.GENERIC).execute(new Runnable() {
                        @Override
                        public void run() {
                            internalStart(event.state());
                        }
                    });
                }
            }
        }

    }

}
