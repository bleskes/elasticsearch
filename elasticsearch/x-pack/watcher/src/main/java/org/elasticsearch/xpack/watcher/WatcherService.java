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

package org.elasticsearch.xpack.watcher;


import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.xpack.support.clock.Clock;
import org.elasticsearch.xpack.watcher.execution.ExecutionService;
import org.elasticsearch.xpack.watcher.support.WatcherIndexTemplateRegistry;
import org.elasticsearch.xpack.watcher.trigger.TriggerService;
import org.elasticsearch.xpack.watcher.watch.Watch;
import org.elasticsearch.xpack.watcher.watch.WatchLockService;
import org.elasticsearch.xpack.watcher.watch.WatchStatus;
import org.elasticsearch.xpack.watcher.watch.WatchStore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.elasticsearch.xpack.watcher.support.Exceptions.illegalArgument;
import static org.elasticsearch.xpack.watcher.support.Exceptions.illegalState;
import static org.elasticsearch.xpack.watcher.support.Exceptions.ioException;


public class WatcherService extends AbstractComponent {

    private final Clock clock;
    private final TriggerService triggerService;
    private final Watch.Parser watchParser;
    private final WatchStore watchStore;
    private final WatchLockService watchLockService;
    private final ExecutionService executionService;
    private final WatcherIndexTemplateRegistry watcherIndexTemplateRegistry;
    // package-private for testing
    final AtomicReference<WatcherState> state = new AtomicReference<>(WatcherState.STOPPED);

    @Inject
    public WatcherService(Settings settings, Clock clock, TriggerService triggerService, WatchStore watchStore,
                          Watch.Parser watchParser, ExecutionService executionService, WatchLockService watchLockService,
                          WatcherIndexTemplateRegistry watcherIndexTemplateRegistry) {
        super(settings);
        this.clock = clock;
        this.triggerService = triggerService;
        this.watchStore = watchStore;
        this.watchParser = watchParser;
        this.watchLockService = watchLockService;
        this.executionService = executionService;
        this.watcherIndexTemplateRegistry = watcherIndexTemplateRegistry;
    }

    public void start(ClusterState clusterState) throws Exception {
        if (state.compareAndSet(WatcherState.STOPPED, WatcherState.STARTING)) {
            try {
                logger.debug("starting watch service...");
                watcherIndexTemplateRegistry.addTemplatesIfMissing();
                watchLockService.start();

                // Try to load watch store before the execution service, b/c action depends on watch store
                watchStore.start(clusterState);
                executionService.start(clusterState);

                triggerService.start(watchStore.activeWatches());
                state.set(WatcherState.STARTED);
                logger.debug("watch service has started");
            } catch (Exception e) {
                state.set(WatcherState.STOPPED);
                throw e;
            }
        } else {
            logger.debug("not starting watcher, because its state is [{}] while [{}] is expected", state, WatcherState.STOPPED);
        }
    }

    public boolean validate(ClusterState state) {
        return watchStore.validate(state) && executionService.validate(state);
    }

    public void stop() {
        if (state.compareAndSet(WatcherState.STARTED, WatcherState.STOPPING)) {
            logger.debug("stopping watch service...");
            triggerService.stop();
            executionService.stop();
            try {
                watchLockService.stop();
            } catch (ElasticsearchTimeoutException te) {
                logger.warn("error stopping WatchLockService", te);
            }
            watchStore.stop();
            state.set(WatcherState.STOPPED);
            logger.debug("watch service has stopped");
        } else {
            logger.debug("not stopping watcher, because its state is [{}] while [{}] is expected", state, WatcherState.STARTED);
        }
    }

    public WatchStore.WatchDelete deleteWatch(String id) {
        ensureStarted();
        WatchStore.WatchDelete delete = watchStore.delete(id);
        if (delete.deleteResponse().getResult() == DocWriteResponse.Result.DELETED) {
            triggerService.remove(id);
        }
        return delete;
    }

    public IndexResponse putWatch(String id, BytesReference watchSource, boolean active) throws IOException {
        ensureStarted();
        DateTime now = clock.nowUTC();
        Watch watch = watchParser.parseWithSecrets(id, false, watchSource, now);
        watch.setState(active, now);
        WatchStore.WatchPut result = watchStore.put(watch);

        if (result.previous() == null) {
            // this is a newly created watch, so we only need to schedule it if it's active
            if (result.current().status().state().isActive()) {
                triggerService.add(result.current());
            }

        } else if (result.current().status().state().isActive()) {

            if (!result.previous().status().state().isActive()) {
                // the replaced watch was inactive, which means it wasn't scheduled. The new watch is active
                // so we need to schedule it
                triggerService.add(result.current());

            } else if (!result.previous().trigger().equals(result.current().trigger())) {
                // the previous watch was active and its schedule is different than the schedule of the
                // new watch, so we need to
                triggerService.add(result.current());
            }
        } else {
            // if the current is inactive, we'll just remove it from the trigger service
            // just to be safe
            triggerService.remove(result.current().id());
        }
        return result.indexResponse();
    }

    /**
     * TODO: add version, fields, etc support that the core get api has as well.
     */
    public Watch getWatch(String name) {
        return watchStore.get(name);
    }

    public WatcherState state() {
        return state.get();
    }

    /**
     * Acks the watch if needed
     */
    public WatchStatus ackWatch(String id, String[] actionIds) throws IOException {
        ensureStarted();
        if (actionIds == null || actionIds.length == 0) {
            actionIds = new String[] { Watch.ALL_ACTIONS_ID };
        }
        Watch watch = watchStore.get(id);
        if (watch == null) {
            throw illegalArgument("watch [{}] does not exist", id);
        }
        // we need to create a safe copy of the status
        if (watch.ack(clock.now(DateTimeZone.UTC), actionIds)) {
            try {
                watchStore.updateStatus(watch);
            } catch (IOException ioe) {
                throw ioException("failed to update the watch [{}] on ack", ioe, watch.id());
            } catch (VersionConflictEngineException vcee) {
                throw illegalState("failed to update the watch [{}] on ack, perhaps it was force deleted", vcee, watch.id());
            }
        }
        return new WatchStatus(watch.status());
    }

    public WatchStatus activateWatch(String id) throws IOException {
        return setWatchState(id, true);
    }

    public WatchStatus deactivateWatch(String id) throws IOException {
        return setWatchState(id, false);
    }

    WatchStatus setWatchState(String id, boolean active) throws IOException {
        ensureStarted();
        // for now, when a watch is deactivated we don't remove its runtime representation
        // that is, the store will still keep the watch in memory. We only mark the watch
        // as inactive (both in runtime and also update the watch in the watches index)
        // and remove the watch from the trigger service, such that it will not be triggered
        // nor its trigger be evaluated.
        //
        // later on we can consider removing the watch runtime representation from memory
        // as well. This will mean that the in-memory loaded watches will no longer be a
        // complete representation of the watches in the index. This requires careful thought
        // to make sure, such incompleteness doesn't hurt any other part of watcher (we need
        // to run this exercise anyway... and make sure that nothing in watcher relies on the
        // fact that the watch store holds all watches in memory.

        Watch watch = watchStore.get(id);
        if (watch == null) {
            throw illegalArgument("watch [{}] does not exist", id);
        }
        if (watch.setState(active, clock.nowUTC())) {
            try {
                watchStore.updateStatus(watch);
                if (active) {
                    triggerService.add(watch);
                } else {
                    triggerService.remove(watch.id());
                }
            } catch (IOException ioe) {
                throw ioException("failed to update the watch [{}] on ack", ioe, watch.id());
            } catch (VersionConflictEngineException vcee) {
                throw illegalState("failed to update the watch [{}] on ack, perhaps it was force deleted", vcee, watch.id());
            }
        }
        // we need to create a safe copy of the status
        return new WatchStatus(watch.status());
    }

    public long watchesCount() {
        return watchStore.watches().size();
    }

    private void ensureStarted() {
        if (state.get() != WatcherState.STARTED) {
            throw new IllegalStateException("not started");
        }
    }

    public Map<String, Object> usageStats() {
        Map<String, Object> innerMap = executionService.usageStats();
        innerMap.putAll(watchStore.usageStats());
        return innerMap;
    }

    /**
     * Something deleted or closed the {@link WatchStore#INDEX} and thus we need to do some cleanup to prevent further execution of watches
     * as those watches cannot be updated anymore
     */
    public void watchIndexDeletedOrClosed() {
        watchStore.clearWatchesInMemory();
        executionService.clearExecutions();
    }
}
