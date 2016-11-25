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

package org.elasticsearch.cluster;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.cluster.service.ClusterServiceState;
import org.elasticsearch.cluster.service.ClusterStateStatus;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.ThreadContext;

/**
 * A utility class which simplifies interacting with the cluster state in cases where
 * one tries to take action based on the current state but may want to wait for a new state
 * and retry upon failure.
 */
public class ClusterStateObserver {

    protected final Logger logger;

    public final ChangePredicate MATCH_ALL_CHANGES_PREDICATE = new ChangePredicate() {

        @Override
        public boolean apply(ClusterChangedEvent changedEvent) {
            return changedEvent.previousState().version() != changedEvent.state().version();
        }

        @Override
        public final boolean apply(ClusterServiceState previousState, ClusterServiceState newState) {
            return previousState != newState && newState.getClusterStateStatus() == ClusterStateStatus.APPLIED;
        }

    };

    private final ClusterService clusterService;
    private final ThreadContext contextHolder;

    final TimeoutClusterStateListener clusterStateListener = new ObserverClusterStateListener();

    // volatile for sampling without locking
    protected volatile ObservingContext observingContext;


    public ClusterStateObserver(ClusterService clusterService, Logger logger, ThreadContext contextHolder) {
        this(clusterService, new TimeValue(60000), logger, contextHolder);
    }

    /**
     * @param timeout a global timeout for this observer. After it has expired the observer
     *                will fail any existing or new #waitForNextChange calls. Set to null
     *                to wait indefinitely
     */
    public ClusterStateObserver(ClusterService clusterService, @Nullable TimeValue timeout, Logger logger, ThreadContext contextHolder) {
        this.clusterService = clusterService;
        this.observingContext = new ObservedContext(clusterService.clusterServiceState(), System.nanoTime(), timeout, false);
        this.logger = logger;
        this.contextHolder = contextHolder;
    }

    /** last cluster state observed by this observer. Note that this may not be the current one */
    public ClusterState observedState() {
        ObservingContext context = observingContext;
        if (context instanceof ObservedContext) {
            return context.getObservedState().getClusterState();
        } else {
            assert context instanceof WaitingContext;
            throw new IllegalStateException("observed state is not available while waiting for a new state");
        }
    }

    /** indicates whether this observer has timedout */
    public boolean isTimedOut() {
        final ObservingContext context = observingContext;
        final boolean timedOut;
        if (context instanceof ObservedContext) {
            timedOut = ((ObservedContext) context).isTimedOut();
        } else {
            timedOut = false;
        }
        return timedOut;
    }

    public void waitForNextChange(Listener listener) {
        waitForNextChange(listener, MATCH_ALL_CHANGES_PREDICATE);
    }

    public void waitForNextChange(Listener listener, @Nullable TimeValue timeOutValue) {
        waitForNextChange(listener, MATCH_ALL_CHANGES_PREDICATE, timeOutValue);
    }

    public void waitForNextChange(Listener listener, ChangePredicate changePredicate) {
        waitForNextChange(listener, changePredicate, null);
    }

    /**
     * Wait for the next cluster state which satisfies changePredicate
     *
     * @param listener        callback listener
     * @param changePredicate predicate to check whether cluster state changes are relevant and the callback should be called
     */
    public void waitForNextChange(Listener listener, ChangePredicate changePredicate, @Nullable TimeValue timeOutValue) {
        final boolean nextStateReady;
        synchronized (this) {
            if (observingContext instanceof WaitingContext) {
                throw new ElasticsearchException("already waiting for a cluster state change");
            }

            final long startTimeInNanos;
            if (timeOutValue == null) {
                timeOutValue = observingContext.getTimeOutValue();
                startTimeInNanos = observingContext.getStartTimeInNanos();
            } else {
                startTimeInNanos = System.nanoTime();
            }
            final ClusterServiceState newState = clusterService.clusterServiceState();
            final ClusterServiceState lastState = observingContext.getObservedState();
            final long timeoutTimeLeftNanons =
                timeOutValue == null ? Long.MAX_VALUE : timeOutValue.nanos() - (System.nanoTime() - startTimeInNanos);
            if (timeoutTimeLeftNanons <= 0L) {
                // things have timeout while we were busy -> notify
                logger.trace("observer timed out. notifying listener. timeout setting [{}], time since start [{}]",
                    timeOutValue, TimeValue.timeValueNanos(startTimeInNanos));
                // update to latest, in case people want to retry
                observingContext = new ObservedContext(clusterService.clusterServiceState(), startTimeInNanos, timeOutValue, true);
                nextStateReady = true;
            } else if (changePredicate.apply(lastState, newState)) {
                // good enough, let's go.
                logger.trace("observer: sampled state accepted by predicate ({})", newState);
                observingContext = new ObservedContext(newState, startTimeInNanos, timeOutValue, false);
                nextStateReady = true;
            } else {
                logger.trace("observer: sampled state rejected by predicate ({}). adding listener to ClusterService", newState);
                observingContext = new WaitingContext(new ContextPreservingListener(listener, contextHolder.newStoredContext()),
                    changePredicate, lastState, startTimeInNanos, timeOutValue);
                clusterService.add(timeoutTimeLeftNanons == Long.MAX_VALUE ? null : TimeValue.timeValueNanos(timeoutTimeLeftNanons),
                    clusterStateListener);
                nextStateReady = false;
            }
        }
        // now call listener if needed (out of lock)
        if (nextStateReady) {
            assert observingContext instanceof ObservedContext;
            if (((ObservedContext) observingContext).isTimedOut()) {
                listener.onTimeout(observingContext.getTimeOutValue());
            } else {
                listener.onNewClusterState(observingContext.getObservedState().getClusterState());
            }
        }
    }

    class ObserverClusterStateListener implements TimeoutClusterStateListener {

        @Override
        public void clusterChanged(ClusterChangedEvent event) {
            final Runnable listenerToCallIfNeeded;
            synchronized (ClusterStateObserver.this) {
                // it may be that we are called concurrently with onTimeout, which will change the context
                if (observingContext instanceof WaitingContext) {
                    final WaitingContext waitingContext = (WaitingContext) ClusterStateObserver.this.observingContext;
                    if (waitingContext.getChangePredicate().apply(event)) {
                        listenerToCallIfNeeded = () -> waitingContext.getListener().onNewClusterState(event.state());
                        clusterService.remove(this);
                        ClusterServiceState state = new ClusterServiceState(event.state(), ClusterStateStatus.APPLIED);
                        logger.trace("observer: accepting cluster state change ({})", state);
                        ClusterStateObserver.this.observingContext = new ObservedContext(state, ClusterStateObserver.this.observingContext.getStartTimeInNanos(),
                            ClusterStateObserver.this.observingContext.getTimeOutValue(), false);
                    } else {
                        listenerToCallIfNeeded = null;
                        logger.trace("observer: predicate rejected change (new cluster state version [{}])", event.state().version());
                    }
                } else {
                    listenerToCallIfNeeded = null;
                    logger.trace("observer: predicate approved change but observing context has changed - ignoring (new cluster state version [{}])", event.state().version());
                }
            }
            if (listenerToCallIfNeeded != null) {
                listenerToCallIfNeeded.run();
            }
        }

        @Override
        public void postAdded() {
            final Listener listenerToCallIfNeeded;
            ClusterServiceState newState = clusterService.clusterServiceState();
            synchronized (ClusterStateObserver.this) {
                // it may be that we are called concurrently with onTimeout, which will change the context
                if (observingContext instanceof WaitingContext) {
                    final WaitingContext waitingContext = (WaitingContext) observingContext;
                    ClusterServiceState lastState = waitingContext.getObservedState();
                    if (waitingContext.getChangePredicate().apply(lastState, newState)) {
                        logger.trace("observer: post adding listener: accepting current cluster state ({})", newState);
                        clusterService.remove(this);
                        observingContext =
                            new ObservedContext(newState, waitingContext.getStartTimeInNanos(), observingContext.getTimeOutValue(), false);
                        listenerToCallIfNeeded = waitingContext.getListener();
                    } else {
                        logger.trace("observer: postAdded - predicate rejected state ({})", newState);
                        listenerToCallIfNeeded = null;
                        // clear observed state to free memory and resources
                        waitingContext.clearObservedState();
                    }
                } else {
                    logger.trace("observer: postAdded - observing context has changed - ignoring ({})", newState);
                    listenerToCallIfNeeded = null;
                }
            }
            if (listenerToCallIfNeeded != null) {
                assert newState == observingContext.getObservedState();
                assert observingContext instanceof ObservedContext;
                listenerToCallIfNeeded.onNewClusterState(newState.getClusterState());
            }
        }

        @Override
        public void onClose() {
            final Listener listenerToCallIfNeeded;
            synchronized (ClusterStateObserver.this) {
                // it may be that we are called concurrently with onTimeout, which will change the context
                if (observingContext instanceof WaitingContext) {
                    final WaitingContext waitingContext = (WaitingContext) observingContext;
                    logger.trace("observer: cluster service closed. notifying listener.");
                    clusterService.remove(this);
                    observingContext = new ObservedContext(clusterService.clusterServiceState(),
                        waitingContext.getStartTimeInNanos(), observingContext.getTimeOutValue(), false);
                    listenerToCallIfNeeded = waitingContext.getListener();
                } else {
                    listenerToCallIfNeeded = null;
                }
            }
            if (listenerToCallIfNeeded != null) {
                assert observingContext instanceof ObservedContext;
                listenerToCallIfNeeded.onClusterServiceClose();
            }
        }

        @Override
        public void onTimeout(TimeValue timeout) {
            final Runnable listenerToCallIfNeeded;
            synchronized (ClusterStateObserver.this) {
                // it may be that we are called concurrently with onTimeout, which will change the context
                if (observingContext instanceof WaitingContext) {
                    final WaitingContext waitingContext = (WaitingContext) observingContext;
                    clusterService.remove(this);
                    TimeValue timeSinceStart = TimeValue.timeValueNanos(System.nanoTime() - waitingContext.getStartTimeInNanos());
                    logger.trace("observer: timeout notification from cluster service. timeout setting [{}], time since start [{}]",
                        waitingContext.getTimeOutValue(), timeSinceStart);
                    // update to latest, in case people want to retry
                    observingContext = new ObservedContext(clusterService.clusterServiceState(),
                        waitingContext.getStartTimeInNanos(), observingContext.getTimeOutValue(), true);
                    listenerToCallIfNeeded = () -> waitingContext.getListener().onTimeout(waitingContext.getTimeOutValue());
                } else {
                    listenerToCallIfNeeded = null;
                }
            }
            if (listenerToCallIfNeeded != null) {
                assert observingContext instanceof ObservedContext;
                assert ((ObservedContext) observingContext).isTimedOut();
                listenerToCallIfNeeded.run();
            }
        }
    }

    public interface Listener {

        /** called when a new state is observed */
        void onNewClusterState(ClusterState state);

        /** called when the cluster service is closed */
        void onClusterServiceClose();

        void onTimeout(TimeValue timeout);
    }

    public interface ChangePredicate {

        /**
         * a rough check used when starting to monitor for a new change. Called infrequently can be less accurate.
         *
         * @return true if newState should be accepted
         */
        boolean apply(ClusterServiceState previousState,
                      ClusterServiceState newState);

        /**
         * called to see whether a cluster change should be accepted
         *
         * @return true if changedEvent.state() should be accepted
         */
        boolean apply(ClusterChangedEvent changedEvent);
    }


    public abstract static class ValidationPredicate implements ChangePredicate {

        @Override
        public final boolean apply(ClusterServiceState previousState, ClusterServiceState newState) {
            return previousState != newState && newState.getClusterStateStatus() == ClusterStateStatus.APPLIED
                && validate(newState.getClusterState());
        }

        protected abstract boolean validate(ClusterState newState);

        @Override
        public final boolean apply(ClusterChangedEvent changedEvent) {
            return changedEvent.previousState().version() != changedEvent.state().version() &&
                validate(changedEvent.state());
        }
    }

    protected abstract static class ObservingContext {
        protected final long startTimeInNanos;
        @Nullable
        protected final TimeValue timeOutValue;

        protected ObservingContext(long startTimeInNanos, @Nullable TimeValue timeOutValue) {
            this.startTimeInNanos = startTimeInNanos;
            this.timeOutValue = timeOutValue;
        }

        public TimeValue getTimeOutValue() {
            return timeOutValue;
        }

        public abstract ClusterServiceState getObservedState();

        public long getStartTimeInNanos() {
            return startTimeInNanos;
        }
    }

    private static class ObservedContext extends ObservingContext {

        private ClusterServiceState observedState;
        private boolean isTimedOut;

        public ObservedContext(ClusterServiceState observedState, long startTimeInNanos, TimeValue timeOutValue, boolean isTimedOut) {
            super(startTimeInNanos, timeOutValue);
            this.observedState = observedState;
            this.isTimedOut = isTimedOut;
        }

        public boolean isTimedOut() {
            return isTimedOut;
        }

        @Override
        public ClusterServiceState getObservedState() {
            return observedState;
        }
    }

    private static class WaitingContext extends ObservingContext {
        private final Listener listener;
        private final ChangePredicate changePredicate;
        private ClusterServiceState observedState;

        public WaitingContext(Listener listener, ChangePredicate changePredicate, ClusterServiceState observedState,
                              long startTimeInNanos, @Nullable TimeValue timeOutValue) {
            super(startTimeInNanos, timeOutValue);
            this.listener = listener;
            this.changePredicate = changePredicate;
            this.observedState = observedState;
        }

        @Override
        public ClusterServiceState getObservedState() {
            if (observedState == null) {
                throw new IllegalStateException("cluster state was cleared");
            }
            return observedState;
        }

        public void clearObservedState() {
            observedState = null;
        }

        public ChangePredicate getChangePredicate() {
            return changePredicate;
        }

        public Listener getListener() {
            return listener;
        }
    }

    private static final class ContextPreservingListener implements Listener {
        private final Listener delegate;
        private final ThreadContext.StoredContext tempContext;


        private ContextPreservingListener(Listener delegate, ThreadContext.StoredContext storedContext) {
            this.tempContext = storedContext;
            this.delegate = delegate;
        }

        @Override
        public void onNewClusterState(ClusterState state) {
            tempContext.restore();
            delegate.onNewClusterState(state);
        }

        @Override
        public void onClusterServiceClose() {
            tempContext.restore();
            delegate.onClusterServiceClose();
        }

        @Override
        public void onTimeout(TimeValue timeout) {
            tempContext.restore();
            delegate.onTimeout(timeout);
        }
    }
}
