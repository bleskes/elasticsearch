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

package org.elasticsearch.discovery;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.cluster.AckedClusterStateTaskListener;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterState.Builder;
import org.elasticsearch.cluster.ClusterStateTaskExecutor;
import org.elasticsearch.cluster.ClusterStateTaskExecutor.ClusterTasksResult;
import org.elasticsearch.cluster.ClusterStateTaskListener;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.RoutingTable;
import org.elasticsearch.cluster.service.AbstractClusterTaskExecutor;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.CountDown;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException;
import org.elasticsearch.common.util.concurrent.FutureUtils;
import org.elasticsearch.threadpool.ThreadPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.elasticsearch.cluster.service.ClusterService.CLUSTER_SERVICE_SLOW_TASK_LOGGING_THRESHOLD_SETTING;
import static org.elasticsearch.common.util.concurrent.EsExecutors.daemonThreadFactory;

public class DiscoveryService extends AbstractClusterTaskExecutor {

    public static final String DISCOVERY_UPDATE_THREAD_NAME = "discoveryService#updateTask";

    final private BiConsumer<ClusterChangedEvent, Discovery.AckListener> clusterStatePublisher;

    private TimeValue slowTaskLoggingThreshold;

    private final AtomicReference<ClusterState> lastCommittedState;

    private final DiscoverySettings discoverySettings;

    public DiscoveryService(Settings settings, ThreadPool threadPool,
                                ClusterState initialState, BiConsumer<ClusterChangedEvent, Discovery.AckListener> clusterStatePublisher,
                                DiscoverySettings discoverySettings) {
        super(settings, threadPool);
        Objects.requireNonNull(clusterStatePublisher, "please set a cluster state publisher before starting");
        Objects.requireNonNull(discoverySettings, "please set discovery settings before starting");

        this.lastCommittedState = new AtomicReference<>(initialState);
        this.slowTaskLoggingThreshold = CLUSTER_SERVICE_SLOW_TASK_LOGGING_THRESHOLD_SETTING.get(settings);
        this.clusterStatePublisher = clusterStatePublisher;
        this.discoverySettings = discoverySettings;
        this.threadExecutor = EsExecutors.newSinglePrioritizing(DISCOVERY_UPDATE_THREAD_NAME,
            daemonThreadFactory(settings, DISCOVERY_UPDATE_THREAD_NAME), threadPool.getThreadContext());

    }

    public void setSlowTaskLoggingThreshold(TimeValue slowTaskLoggingThreshold) {
        this.slowTaskLoggingThreshold = slowTaskLoggingThreshold;
    }


    @Override
    protected synchronized void doStop() {
        ThreadPool.terminate(threadExecutor, 10, TimeUnit.SECONDS);
    }

    @Override
    protected synchronized void doClose() {
    }
    /**
     * The current cluster state.
     */
    public ClusterState state() {
        return this.lastCommittedState.get();
    }

    public static boolean assertDiscoveryUpdateThread() {
        assert Thread.currentThread().getName().contains(DISCOVERY_UPDATE_THREAD_NAME) :
            "not called from the discovery state update thread";
        return true;
    }

    public static boolean assertNotDiscoveryUpdateThread(String reason) {
        assert Thread.currentThread().getName().contains(DISCOVERY_UPDATE_THREAD_NAME) == false :
            "Expected current thread [" + Thread.currentThread() + "] to not be the discovery state update thread. Reason: [" +
                reason + "]";
        return true;
    }

    @Override
    protected void runTasks(TaskInputs taskInputs) {
        if (!lifecycle.started()) {
            logger.debug("failing [{}]: local node is no longer master", taskInputs.summary);
            taskInputs.onNoLongerMaster();
            return;
        }

        logger.debug("processing [{}]: execute", taskInputs.summary);
        final ClusterState previousClusterState = state();

        long startTimeNS = currentTimeInNanos();
        TaskOutputs taskOutputs = calculateTaskOutputs(taskInputs, previousClusterState, startTimeNS);
        taskOutputs.notifyFailedTasks();

        if (taskOutputs.clusterStateUnchanged()) {
            taskOutputs.notifySuccessfulTasksOnUnchangedClusterState();
            TimeValue executionTime = TimeValue.timeValueMillis(Math.max(0, TimeValue.nsecToMSec(currentTimeInNanos() - startTimeNS)));
            logger.debug("processing [{}]: took [{}] no change in cluster_state", taskInputs.summary, executionTime);
            warnAboutSlowTaskIfNeeded(executionTime, taskInputs.summary);
        } else {
            ClusterState newClusterState = taskOutputs.newClusterState;
            if (logger.isTraceEnabled()) {
                logger.trace("cluster state updated, source [{}]\n{}", taskInputs.summary, newClusterState);
            } else if (logger.isDebugEnabled()) {
                logger.debug("cluster state updated, version [{}], source [{}]", newClusterState.version(), taskInputs.summary);
            }
            assert newClusterState.nodes().isLocalNodeElectedMaster() :
                "new cluster state indicate local node as master :" + newClusterState;
            try {
                publishChangesAndWaitForLocalAck(taskInputs, taskOutputs);
                TimeValue executionTime = TimeValue.timeValueMillis(Math.max(0, TimeValue.nsecToMSec(currentTimeInNanos() - startTimeNS)));
                logger.debug("processing [{}]: took [{}] done publishing updated cluster_state (version: {}, uuid: {})", taskInputs.summary,
                    executionTime, newClusterState.version(),
                    newClusterState.stateUUID());
                warnAboutSlowTaskIfNeeded(executionTime, taskInputs.summary);
            } catch (Exception e) {
                TimeValue executionTime = TimeValue.timeValueMillis(Math.max(0, TimeValue.nsecToMSec(currentTimeInNanos() - startTimeNS)));
                final long version = newClusterState.version();
                final String stateUUID = newClusterState.stateUUID();
                final String fullState = newClusterState.toString();
                logger.warn(
                    (Supplier<?>) () -> new ParameterizedMessage(
                        "failed to apply updated cluster state in [{}]:\nversion [{}], uuid [{}], source [{}]\n{}",
                        executionTime,
                        version,
                        stateUUID,
                        taskInputs.summary,
                        fullState),
                    e);
                // TODO: do we want to call updateTask.onFailure here?
            }
        }
    }

    public TaskOutputs calculateTaskOutputs(TaskInputs taskInputs, ClusterState previousClusterState, long startTimeNS) {
        ClusterTasksResult<Object> clusterTasksResult = executeTasks(taskInputs, startTimeNS, previousClusterState);
        ClusterState newClusterState = patchVersions(previousClusterState, clusterTasksResult);
        List<UpdateTask> nonFailedTasks = getNonFailedTasks(taskInputs, clusterTasksResult);
        return new TaskOutputs(taskInputs, previousClusterState, newClusterState, nonFailedTasks, clusterTasksResult.executionResults);
    }

    private ClusterState patchVersions(ClusterState previousClusterState, ClusterTasksResult<Object> executionResult) {
        ClusterState newClusterState = executionResult.resultingState;
        assert newClusterState.nodes().isLocalNodeElectedMaster();
        assert newClusterState.blocks().hasGlobalBlock(discoverySettings.getNoMasterBlock().id()) == false :
            "cluster state with master node must not have NO_MASTER_BLOCK";

        if (previousClusterState != newClusterState) {
            // only the master controls the version numbers
            Builder builder = ClusterState.builder(newClusterState).incrementVersion();
            if (previousClusterState.routingTable() != newClusterState.routingTable()) {
                builder.routingTable(RoutingTable.builder(newClusterState.routingTable())
                    .version(newClusterState.routingTable().version() + 1).build());
            }
            if (previousClusterState.metaData() != newClusterState.metaData()) {
                builder.metaData(MetaData.builder(newClusterState.metaData()).version(newClusterState.metaData().version() + 1));
            }
            newClusterState = builder.build();
        }

        return newClusterState;
    }

    private void publishChangesAndWaitForLocalAck(TaskInputs taskInputs, TaskOutputs taskOutputs) {
        ClusterState previousClusterState = taskOutputs.previousClusterState;
        ClusterState newClusterState = taskOutputs.newClusterState;

        ClusterChangedEvent clusterChangedEvent = new ClusterChangedEvent(taskInputs.summary, newClusterState, previousClusterState);
        // new cluster state, notify all listeners
        final DiscoveryNodes.Delta nodesDelta = clusterChangedEvent.nodesDelta();
        if (nodesDelta.hasChanges() && logger.isInfoEnabled()) {
            String summary = nodesDelta.shortSummary();
            if (summary.length() > 0) {
                logger.info("{}, reason: {}", summary, taskInputs.summary);
            }
        }

        final MasterAwareDelegetingAckListener ackListener = taskOutputs.createAckListener(threadPool, newClusterState);

        logger.debug("publishing cluster state version [{}]", newClusterState.version());
        try {
            clusterStatePublisher.accept(clusterChangedEvent, ackListener);
        } catch (Discovery.FailedToCommitClusterStateException t) {
            final long version = newClusterState.version();
            logger.warn(
                (Supplier<?>) () -> new ParameterizedMessage(
                    "failing [{}]: failed to commit cluster state version [{}]", taskInputs.summary, version),
                t);
            taskOutputs.publishingFailed(t);
            return;
        }

        try {
            // TODO: remove indefinite wait
            ackListener.waitForMasterAck();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        lastCommittedState.set(newClusterState);

        taskOutputs.processedDifferentClusterState(previousClusterState, newClusterState);

        try {
            taskOutputs.clusterStatePublished(clusterChangedEvent);
        } catch (Exception e) {
            logger.error(
                (Supplier<?>) () -> new ParameterizedMessage(
                    "exception thrown while notifying executor of new cluster state publication [{}]",
                    taskInputs.summary),
                e);
        }
    }

    /**
     * Output created by executing a set of tasks provided as TaskInputs
     */
    class TaskOutputs {
        public final TaskInputs taskInputs;
        public final ClusterState previousClusterState;
        public final ClusterState newClusterState;
        public final List<UpdateTask> nonFailedTasks;
        public final Map<Object, ClusterStateTaskExecutor.TaskResult> executionResults;

        public TaskOutputs(TaskInputs taskInputs, ClusterState previousClusterState,
                           ClusterState newClusterState, List<UpdateTask> nonFailedTasks,
                           Map<Object, ClusterStateTaskExecutor.TaskResult> executionResults) {
            this.taskInputs = taskInputs;
            this.previousClusterState = previousClusterState;
            this.newClusterState = newClusterState;
            this.nonFailedTasks = nonFailedTasks;
            this.executionResults = executionResults;
        }

        public void publishingFailed(Discovery.FailedToCommitClusterStateException t) {
            nonFailedTasks.forEach(task -> task.listener.onFailure(task.source(), t));
        }

        public void processedDifferentClusterState(ClusterState previousClusterState, ClusterState newClusterState) {
            nonFailedTasks.forEach(task -> task.listener.clusterStateProcessed(task.source(), previousClusterState, newClusterState));
        }

        public void clusterStatePublished(ClusterChangedEvent clusterChangedEvent) {
            taskInputs.executor.clusterStatePublished(clusterChangedEvent);
        }

        public MasterAwareDelegetingAckListener createAckListener(ThreadPool threadPool, ClusterState newClusterState) {
            ArrayList<Discovery.AckListener> ackListeners = new ArrayList<>();

            //timeout straightaway, otherwise we could wait forever as the timeout thread has not started
            nonFailedTasks.stream().filter(task -> task.listener instanceof AckedClusterStateTaskListener).forEach(task -> {
                final AckedClusterStateTaskListener ackedListener = (AckedClusterStateTaskListener) task.listener;
                if (ackedListener.ackTimeout() == null || ackedListener.ackTimeout().millis() == 0) {
                    ackedListener.onAckTimeout();
                } else {
                    try {
                        ackListeners.add(new AckCountDownListener(ackedListener, newClusterState.version(), newClusterState.nodes(),
                            threadPool));
                    } catch (EsRejectedExecutionException ex) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Couldn't schedule timeout thread - node might be shutting down", ex);
                        }
                        //timeout straightaway, otherwise we could wait forever as the timeout thread has not started
                        ackedListener.onAckTimeout();
                    }
                }
            });

            return new MasterAwareDelegetingAckListener(ackListeners, newClusterState.nodes().getMasterNode());
        }

        public boolean clusterStateUnchanged() {
            return previousClusterState == newClusterState;
        }

        public void notifyFailedTasks() {
            // fail all tasks that have failed
            for (UpdateTask updateTask : taskInputs.updateTasks) {
                assert executionResults.containsKey(updateTask.task) : "missing " + updateTask;
                final ClusterStateTaskExecutor.TaskResult taskResult = executionResults.get(updateTask.task);
                if (taskResult.isSuccess() == false) {
                    updateTask.listener.onFailure(updateTask.source(), taskResult.getFailure());
                }
            }
        }

        public void notifySuccessfulTasksOnUnchangedClusterState() {
            nonFailedTasks.forEach(task -> {
                if (task.listener instanceof AckedClusterStateTaskListener) {
                    //no need to wait for ack if nothing changed, the update can be counted as acknowledged
                    ((AckedClusterStateTaskListener) task.listener).onAllNodesAcked(null);
                }
                task.listener.clusterStateProcessed(task.source(), newClusterState, newClusterState);
            });
        }
    }

    @Override
    protected SafeClusterStateTaskListener safe(ClusterStateTaskListener listener) {
        if (listener instanceof AckedClusterStateTaskListener) {
            return new SafeAckedClusterStateTaskListener((AckedClusterStateTaskListener) listener, logger);
        } else {
            return new SafeClusterStateTaskListener(listener, logger);
        }
    }

    private static class SafeClusterStateTaskListener implements ClusterStateTaskListener {
        private final ClusterStateTaskListener listener;
        private final Logger logger;

        public SafeClusterStateTaskListener(ClusterStateTaskListener listener, Logger logger) {
            this.listener = listener;
            this.logger = logger;
        }

        @Override
        public void onFailure(String source, Exception e) {
            try {
                listener.onFailure(source, e);
            } catch (Exception inner) {
                inner.addSuppressed(e);
                logger.error(
                    (Supplier<?>) () -> new ParameterizedMessage(
                        "exception thrown by listener notifying of failure from [{}]", source), inner);
            }
        }

        @Override
        public void onNoLongerMaster(String source) {
            try {
                listener.onNoLongerMaster(source);
            } catch (Exception e) {
                logger.error(
                    (Supplier<?>) () -> new ParameterizedMessage(
                        "exception thrown by listener while notifying no longer master from [{}]", source), e);
            }
        }

        @Override
        public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
            try {
                listener.clusterStateProcessed(source, oldState, newState);
            } catch (Exception e) {
                logger.error(
                    (Supplier<?>) () -> new ParameterizedMessage(
                        "exception thrown by listener while notifying of cluster state processed from [{}], old cluster state:\n" +
                            "{}\nnew cluster state:\n{}",
                        source, oldState, newState),
                    e);
            }
        }
    }

    private static class SafeAckedClusterStateTaskListener extends SafeClusterStateTaskListener implements AckedClusterStateTaskListener {
        private final AckedClusterStateTaskListener listener;
        private final Logger logger;

        public SafeAckedClusterStateTaskListener(AckedClusterStateTaskListener listener, Logger logger) {
            super(listener, logger);
            this.listener = listener;
            this.logger = logger;
        }

        @Override
        public boolean mustAck(DiscoveryNode discoveryNode) {
            return listener.mustAck(discoveryNode);
        }

        @Override
        public void onAllNodesAcked(@Nullable Exception e) {
            try {
                listener.onAllNodesAcked(e);
            } catch (Exception inner) {
                inner.addSuppressed(e);
                logger.error("exception thrown by listener while notifying on all nodes acked", inner);
            }
        }

        @Override
        public void onAckTimeout() {
            try {
                listener.onAckTimeout();
            } catch (Exception e) {
                logger.error("exception thrown by listener while notifying on ack timeout", e);
            }
        }

        @Override
        public TimeValue ackTimeout() {
            return listener.ackTimeout();
        }
    }

    @Override
    protected void warnAboutSlowTaskIfNeeded(TimeValue executionTime, String source) {
        if (executionTime.getMillis() > slowTaskLoggingThreshold.getMillis()) {
            logger.warn("cluster state update task [{}] took [{}] above the warn threshold of {}", source, executionTime,
                slowTaskLoggingThreshold);
        }
    }

    private static class MasterAwareDelegetingAckListener implements Discovery.AckListener {

        private final List<Discovery.AckListener> listeners;
        private final DiscoveryNode masterNode;
        private final CountDownLatch masterAcked = new CountDownLatch(1);

        private MasterAwareDelegetingAckListener(List<Discovery.AckListener> listeners, DiscoveryNode masterNode) {
            this.listeners = listeners;
            this.masterNode = masterNode;
        }

        @Override
        public void onNodeAck(DiscoveryNode node, @Nullable Exception e) {
            if (node.equals(masterNode)) {
                masterAcked.countDown();
            }
            for (Discovery.AckListener listener : listeners) {
                listener.onNodeAck(node, e);
            }
        }

        public void waitForMasterAck() throws InterruptedException {
            masterAcked.await();
        }

        @Override
        public void onTimeout() {
            throw new UnsupportedOperationException("no timeout delegation");
        }
    }

    private static class AckCountDownListener implements Discovery.AckListener {

        private static final Logger logger = Loggers.getLogger(AckCountDownListener.class);

        private final AckedClusterStateTaskListener ackedTaskListener;
        private final CountDown countDown;
        private final DiscoveryNodes nodes;
        private final long clusterStateVersion;
        private final Future<?> ackTimeoutCallback;
        private Exception lastFailure;

        AckCountDownListener(AckedClusterStateTaskListener ackedTaskListener, long clusterStateVersion, DiscoveryNodes nodes,
                             ThreadPool threadPool) {
            this.ackedTaskListener = ackedTaskListener;
            this.clusterStateVersion = clusterStateVersion;
            this.nodes = nodes;
            int countDown = 0;
            for (DiscoveryNode node : nodes) {
                if (ackedTaskListener.mustAck(node)) {
                    countDown++;
                }
            }
            //we always wait for at least 1 node (the master)
            countDown = Math.max(1, countDown);
            logger.trace("expecting {} acknowledgements for cluster_state update (version: {})", countDown, clusterStateVersion);
            this.countDown = new CountDown(countDown);
            this.ackTimeoutCallback = threadPool.schedule(ackedTaskListener.ackTimeout(), ThreadPool.Names.GENERIC, () -> onTimeout());
        }

        @Override
        public void onNodeAck(DiscoveryNode node, @Nullable Exception e) {
            if (!ackedTaskListener.mustAck(node)) {
                //we always wait for the master ack anyway
                if (!node.equals(nodes.getMasterNode())) {
                    return;
                }
            }
            if (e == null) {
                logger.trace("ack received from node [{}], cluster_state update (version: {})", node, clusterStateVersion);
            } else {
                this.lastFailure = e;
                logger.debug(
                    (Supplier<?>) () -> new ParameterizedMessage(
                        "ack received from node [{}], cluster_state update (version: {})", node, clusterStateVersion),
                    e);
            }

            if (countDown.countDown()) {
                logger.trace("all expected nodes acknowledged cluster_state update (version: {})", clusterStateVersion);
                FutureUtils.cancel(ackTimeoutCallback);
                ackedTaskListener.onAllNodesAcked(lastFailure);
            }
        }

        @Override
        public void onTimeout() {
            if (countDown.fastForward()) {
                logger.trace("timeout waiting for acknowledgement for cluster_state update (version: {})", clusterStateVersion);
                ackedTaskListener.onAckTimeout();
            }
        }
    }
}
