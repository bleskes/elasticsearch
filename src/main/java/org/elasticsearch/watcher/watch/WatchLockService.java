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

package org.elasticsearch.watcher.watch;

import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.watcher.support.concurrent.FairKeyedLock;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class WatchLockService {

    private final FairKeyedLock<String> watchLocks = new FairKeyedLock<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public Lock acquire(String name) {
        if (!running.get()) {
            throw new ElasticsearchIllegalStateException("not started");
        }

        watchLocks.acquire(name);
        return new Lock(name, watchLocks);
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            // init
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            // It can happen we have still ongoing operations and we wait those operations to finish to avoid
            // that watch service or any of its components end up in a illegal state after the state as been set to stopped.
            //
            // For example: A watch action entry may be added while we stopping watcher if we don't wait for
            // ongoing operations to complete. Resulting in once the watch service starts again that more than
            // expected watch records are processed.
            //
            // Note: new operations will fail now because the running has been set to false
            while (watchLocks.hasLockedKeys()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    FairKeyedLock<String> getWatchLocks() {
        return watchLocks;
    }

    public static class Lock {

        private final String name;
        private final FairKeyedLock<String> watchLocks;

        private Lock(String name, FairKeyedLock<String> watchLocks) {
            this.name = name;
            this.watchLocks = watchLocks;

        }

        public void release() {
            watchLocks.release(name);
        }
    }
}
