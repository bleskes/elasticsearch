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

package org.elasticsearch.xpack.watcher.execution;

import org.elasticsearch.common.unit.TimeValue;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.elasticsearch.xpack.watcher.support.Exceptions.illegalState;

public class CurrentExecutions implements Iterable<ExecutionService.WatchExecution> {

    private final ConcurrentMap<String, ExecutionService.WatchExecution> currentExecutions = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition empty = lock.newCondition();
    private boolean seal = false;

    public void put(String id, ExecutionService.WatchExecution execution) {
        lock.lock();
        try {
            if (seal) {
                // We shouldn't get here, because, ExecutionService#started should have been set to false
                throw illegalState("could not register execution [{}]. current executions are sealed and forbid registrations of " +
                        "additional executions.", id);
            }
            currentExecutions.put(id, execution);
        } finally {
            lock.unlock();
        }
    }

    public void remove(String id) {
        lock.lock();
        try {
            currentExecutions.remove(id);
            if (currentExecutions.isEmpty()) {
                empty.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    public void sealAndAwaitEmpty(TimeValue maxStopTimeout) {
        // We may have current executions still going on.
        // We should try to wait for the current executions to have completed.
        // Otherwise we can run into a situation where we didn't delete the watch from the .triggered_watches index,
        // but did insert into the history index. Upon start this can lead to DocumentAlreadyExistsException,
        // because we already stored the history record during shutdown...
        // (we always first store the watch record and then remove the triggered watch)
        lock.lock();
        try {
            seal = true;
            while (currentExecutions.size() > 0) {
                empty.await(maxStopTimeout.millis(), TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<ExecutionService.WatchExecution> iterator() {
        return currentExecutions.values().iterator();
    }
}
