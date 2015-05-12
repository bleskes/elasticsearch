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

package org.elasticsearch.watcher.execution;

import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.watcher.WatcherException;
import org.elasticsearch.watcher.actions.ActionWrapper;
import org.elasticsearch.watcher.condition.Condition;
import org.elasticsearch.watcher.history.HistoryStore;
import org.elasticsearch.watcher.history.WatchRecord;
import org.elasticsearch.watcher.input.Input;
import org.elasticsearch.watcher.support.clock.Clock;
import org.elasticsearch.watcher.trigger.TriggerEvent;
import org.elasticsearch.watcher.watch.Watch;
import org.elasticsearch.watcher.watch.WatchLockService;
import org.elasticsearch.watcher.watch.WatchStore;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.elasticsearch.common.joda.time.DateTimeZone.UTC;

/**
 */
public class ExecutionService extends AbstractComponent {

    private final HistoryStore historyStore;
    private final WatchExecutor executor;
    private final WatchStore watchStore;
    private final WatchLockService watchLockService;
    private final Clock clock;
    private final TimeValue defaultThrottlePeriod;

    private final ConcurrentMap<String, WatchExecution> currentExecutions = ConcurrentCollections.newConcurrentMapWithAggressiveConcurrency();

    private final AtomicBoolean started = new AtomicBoolean(false);

    @Inject
    public ExecutionService(Settings settings, HistoryStore historyStore, WatchExecutor executor, WatchStore watchStore,
                            WatchLockService watchLockService, Clock clock) {
        super(settings);
        this.historyStore = historyStore;
        this.executor = executor;
        this.watchStore = watchStore;
        this.watchLockService = watchLockService;
        this.clock = clock;
        TimeValue throttlePeriod = componentSettings.getAsTime("default_throttle_period", TimeValue.timeValueSeconds(5));
        this.defaultThrottlePeriod = throttlePeriod.millis() == 0 ? null : throttlePeriod;
    }

    public void start(ClusterState state) {
        if (started.get()) {
            return;
        }

        assert executor.queue().isEmpty() : "queue should be empty, but contains " + executor.queue().size() + " elements.";
        Collection<WatchRecord> records = historyStore.loadRecords(state, WatchRecord.State.AWAITS_EXECUTION);
        if (started.compareAndSet(false, true)) {
            logger.debug("starting execution service");
            historyStore.start();
            executeRecords(records);
            logger.debug("started execution service");
        }
    }

    public boolean validate(ClusterState state) {
        return historyStore.validate(state);
    }

    public void stop() {
        if (started.compareAndSet(true, false)) {
            logger.debug("stopping execution service");
            // We could also rely on the shutdown in #updateSettings call, but
            // this is a forceful shutdown that also interrupts the worker threads in the threadpool
            List<Runnable> cancelledTasks = new ArrayList<>();
            executor.queue().drainTo(cancelledTasks);
            historyStore.stop();
            logger.debug("cancelled [{}] queued tasks", cancelledTasks.size());
            logger.debug("stopped execution service");
        }
    }

    public boolean started() {
        return started.get();
    }

    public TimeValue defaultThrottlePeriod() {
        return defaultThrottlePeriod;
    }

    public long queueSize() {
        return executor.queue().size();
    }

    public long largestQueueSize() {
        return executor.largestPoolSize();
    }

    public List<WatchExecutionSnapshot> currentExecutions() {
        List<WatchExecutionSnapshot> currentExecutions = new ArrayList<>();
        for (WatchExecution watchExecution : this.currentExecutions.values()) {
            currentExecutions.add(watchExecution.createSnapshot());
        }
        // Lets show the longest running watch first:
        Collections.sort(currentExecutions, new Comparator<WatchExecutionSnapshot>() {
            @Override
            public int compare(WatchExecutionSnapshot e1, WatchExecutionSnapshot e2) {
                return -e1.executionTime().compareTo(e2.executionTime());
            }
        });
        return currentExecutions;
    }

    void processEventsAsync(Iterable<TriggerEvent> events) throws WatcherException {
        if (!started.get()) {
            throw new ElasticsearchIllegalStateException("not started");
        }
        final LinkedList<WatchRecord> records = new LinkedList<>();
        final LinkedList<TriggeredExecutionContext> contexts = new LinkedList<>();

        DateTime now = clock.now(UTC);
        for (TriggerEvent event : events) {
            Watch watch = watchStore.get(event.jobName());
            if (watch == null) {
                logger.warn("unable to find watch [{}] in the watch store, perhaps it has been deleted", event.jobName());
                continue;
            }
            TriggeredExecutionContext ctx = new TriggeredExecutionContext(watch, now, event, defaultThrottlePeriod);
            contexts.add(ctx);
            records.add(new WatchRecord(ctx.id(), watch, event));
        }

        logger.debug("saving watch records [{}]", records.size());
        if (records.size() == 0) {
            return;
        }

        if (records.size() == 1) {
            final WatchRecord watchRecord = records.getFirst();
            final TriggeredExecutionContext ctx = contexts.getFirst();
            historyStore.put(watchRecord, new ActionListener<Boolean>() {
                @Override
                public void onResponse(Boolean aBoolean) {
                    executeAsync(ctx, watchRecord);
                }

                @Override
                public void onFailure(Throwable e) {
                    Throwable cause = ExceptionsHelper.unwrapCause(e);
                    if (cause instanceof EsRejectedExecutionException) {
                        logger.debug("failed to store watch record [{}]/[{}] due to overloaded threadpool [{}]", watchRecord, ctx.id(), ExceptionsHelper.detailedMessage(e));
                    } else {
                        logger.warn("failed to store watch record [{}]/[{}]", e, watchRecord, ctx.id());
                    }
                }
            });
        } else {
            historyStore.putAll(records, new ActionListener<List<Integer>>() {
                @Override
                public void onResponse(List<Integer> successFullSlots) {
                    for (Integer slot : successFullSlots) {
                        executeAsync(contexts.get(slot), records.get(slot));
                    }
                }

                @Override
                public void onFailure(Throwable e) {
                    Throwable cause = ExceptionsHelper.unwrapCause(e);
                    if (cause instanceof EsRejectedExecutionException) {
                        logger.debug("failed to store watch records due to overloaded threadpool [{}]", ExceptionsHelper.detailedMessage(e));
                    } else {
                        logger.warn("failed to store watch records", e);
                    }
                }
            });
        }
    }

    void processEventsSync(Iterable<TriggerEvent> events) throws WatcherException {
        if (!started.get()) {
            throw new ElasticsearchIllegalStateException("not started");
        }
        final LinkedList<WatchRecord> records = new LinkedList<>();
        final LinkedList<TriggeredExecutionContext> contexts = new LinkedList<>();

        DateTime now = clock.now(UTC);
        for (TriggerEvent event : events) {
            Watch watch = watchStore.get(event.jobName());
            if (watch == null) {
                logger.warn("unable to find watch [{}] in the watch store, perhaps it has been deleted", event.jobName());
                continue;
            }
            TriggeredExecutionContext ctx = new TriggeredExecutionContext(watch, now, event, defaultThrottlePeriod);
            contexts.add(ctx);
            records.add(new WatchRecord(ctx.id(), watch, event));
        }

        logger.debug("saving watch records [{}]", records.size());
        if (records.size() == 0) {
            return;
        }

        if (records.size() == 1) {
            final WatchRecord watchRecord = records.getFirst();
            final TriggeredExecutionContext ctx = contexts.getFirst();
            historyStore.put(watchRecord);
            executeAsync(ctx, watchRecord);
        } else {
            List<Integer> slots = historyStore.putAll(records);
            for (Integer slot : slots) {
                executeAsync(contexts.get(slot), records.get(slot));
            }
        }
    }

    public WatchRecord execute(WatchExecutionContext ctx) throws IOException {
        WatchRecord watchRecord = new WatchRecord(ctx.id(), ctx.watch(), ctx.triggerEvent());
        WatchLockService.Lock lock = watchLockService.acquire(ctx.watch().id());
        try {
            currentExecutions.put(ctx.watch().id(), new WatchExecution(ctx, Thread.currentThread()));
            WatchExecutionResult result = executeInner(ctx);
            watchRecord.seal(result);
            if (ctx.recordExecution()) {
                watchStore.updateStatus(ctx.watch());
            }
        } catch (VersionConflictEngineException vcee) {
            throw new WatcherException("Failed to update the watch [{}] on execute perhaps it was force deleted", vcee, ctx.watch().id());
        } finally {
            currentExecutions.remove(ctx.watch().id());
            lock.release();
        }
        if (ctx.recordExecution()) {
            historyStore.put(watchRecord);
        }
        return watchRecord;
    }

    /*
       The execution of an watch is split into two phases:
       1. the trigger part which just makes sure to store the associated watch record in the history
       2. the actual processing of the watch

       The reason this split is that we don't want to lose the fact watch was triggered. This way, even if the
       thread pool that executes the watches is completely busy, we don't lose the fact that the watch was
       triggered (it'll have its history record)
    */

    private void executeAsync(WatchExecutionContext ctx, WatchRecord watchRecord) {
        try {
            executor.execute(new WatchExecutionTask(ctx, watchRecord));
        } catch (EsRejectedExecutionException e) {
            logger.debug("failed to execute triggered watch [{}]", watchRecord.watchId());
            watchRecord.update(WatchRecord.State.FAILED, "failed to run triggered watch [" + watchRecord.watchId() + "] due to thread pool capacity");
            historyStore.update(watchRecord);
        }
    }

    WatchExecutionResult executeInner(WatchExecutionContext ctx) throws IOException {
        ctx.start();
        Watch watch = ctx.watch();
        ctx.beforeInput();
        Input.Result inputResult = ctx.inputResult();
        if (inputResult == null) {
            inputResult = watch.input().execute(ctx);
            ctx.onInputResult(inputResult);
        }
        ctx.beforeCondition();
        Condition.Result conditionResult = ctx.conditionResult();
        if (conditionResult == null) {
            conditionResult = watch.condition().execute(ctx);
            ctx.onConditionResult(conditionResult);
        }

        if (conditionResult.met()) {
            ctx.beforeAction();
            for (ActionWrapper action : watch.actions()) {
                ActionWrapper.Result actionResult = action.execute(ctx);
                ctx.onActionResult(actionResult);
            }
        }
        return ctx.finish();
    }

    void executeRecords(Collection<WatchRecord> records) {
        assert records != null;
        int counter = 0;
        for (WatchRecord record : records) {
            Watch watch = watchStore.get(record.watchId());
            if (watch == null) {
                String message = "unable to find watch for record [" + record.watchId() + "]/[" + record.id() + "], perhaps it has been deleted, ignoring...";
                record.update(WatchRecord.State.DELETED_WHILE_QUEUED, message);
                historyStore.update(record);
            } else {
                TriggeredExecutionContext ctx = new TriggeredExecutionContext(watch, clock.now(UTC), record.triggerEvent(), defaultThrottlePeriod);
                executeAsync(ctx, record);
                counter++;
            }
        }
        logger.debug("executed [{}] watches from the watch history", counter);
    }

    private final class WatchExecutionTask implements Runnable {

        private final WatchRecord watchRecord;

        private final WatchExecutionContext ctx;

        private WatchExecutionTask(WatchExecutionContext ctx, WatchRecord watchRecord) {
            this.watchRecord = watchRecord;
            this.ctx = ctx;
        }

        @Override
        public void run() {
            if (!started.get()) {
                logger.debug("can't initiate watch execution as execution service is not started, ignoring it...");
                return;
            }
            logger.trace("executing [{}] [{}]", ctx.watch().id(), ctx.id());

            WatchLockService.Lock lock = watchLockService.acquire(ctx.watch().id());
            try {
                currentExecutions.put(ctx.watch().id(), new WatchExecution(ctx, Thread.currentThread()));
                if (watchStore.get(ctx.watch().id()) == null) {
                    //Fail fast if we are trying to execute a deleted watch
                    String message = "unable to find watch for record [" + watchRecord.id() + "], perhaps it has been deleted, ignoring...";
                    watchRecord.update(WatchRecord.State.DELETED_WHILE_QUEUED, message);
                } else {
                    watchRecord.update(WatchRecord.State.CHECKING, null);
                    logger.debug("checking watch [{}]", watchRecord.watchId());
                    WatchExecutionResult result = executeInner(ctx);
                    watchRecord.seal(result);
                    if (ctx.recordExecution()) {
                        watchStore.updateStatus(ctx.watch());
                    }
                }
            } catch (Exception e) {
                if (started()) {
                    String detailedMessage = ExceptionsHelper.detailedMessage(e);
                    logger.warn("failed to execute watch [{}]/[{}], failure [{}]", watchRecord.watchId(), ctx.id(), detailedMessage);
                    watchRecord.update(WatchRecord.State.FAILED, detailedMessage);
                } else {
                    logger.debug("failed to execute watch [{}] after shutdown", e, watchRecord);
                }
            } finally {
                currentExecutions.remove(ctx.watch().id());
                lock.release();
                logger.trace("finished [{}]/[{}]", ctx.watch().id(), ctx.id());
            }

            if (ctx.recordExecution() && started()) {
                try {
                    historyStore.update(watchRecord);
                } catch (Exception e) {
                    logger.error("failed to update watch record [{}]/[{}], failure [{}], record failure if any [{}]", watchRecord.watchId(), ctx.id(), ExceptionsHelper.detailedMessage(e), watchRecord.message());
                }
            }
        }
    }

    public static class WatchExecution {

        private final WatchExecutionContext context;
        private final Thread executionThread;

        public WatchExecution(WatchExecutionContext context, Thread executionThread) {
            this.context = context;
            this.executionThread = executionThread;
        }

        public WatchExecutionSnapshot createSnapshot() {
            return context.createSnapshot(executionThread);
        }

    }
}
