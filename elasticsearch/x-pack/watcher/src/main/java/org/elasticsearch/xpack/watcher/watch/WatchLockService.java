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

package org.elasticsearch.xpack.watcher.watch;

import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.lease.Releasable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.KeyedLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.elasticsearch.xpack.watcher.support.Exceptions.illegalState;

/**
 *
 */
public class WatchLockService extends AbstractComponent {

    private final KeyedLock<String> watchLocks = new KeyedLock<>(true);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private static final TimeValue DEFAULT_MAX_STOP_TIMEOUT = new TimeValue(30, TimeUnit.SECONDS);
    private static final String DEFAULT_MAX_STOP_TIMEOUT_SETTING = "xpack.watcher.stop.timeout";

    private final TimeValue maxStopTimeout;

    @Inject
    public WatchLockService(Settings settings){
        super(settings);
        maxStopTimeout = settings.getAsTime(DEFAULT_MAX_STOP_TIMEOUT_SETTING, DEFAULT_MAX_STOP_TIMEOUT);
    }

    WatchLockService(TimeValue maxStopTimeout){
        super(Settings.EMPTY);
        this.maxStopTimeout = maxStopTimeout;
    }

    public Releasable acquire(String name) {
        if (!running.get()) {
            throw illegalState("cannot acquire lock for watch [{}]. lock service is not running", name);
        }

        return watchLocks.acquire(name);
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            // init
        }
    }

    /**
     * @throws ElasticsearchTimeoutException if we have waited longer than maxStopTimeout
     */
    public void stop() throws ElasticsearchTimeoutException {
        if (running.compareAndSet(true, false)) {
            // It can happen we have still ongoing operations and we wait those operations to finish to avoid
            // that watch service or any of its components end up in a illegal state after the state as been set to stopped.
            //
            // For example: A watch action entry may be added while we stopping watcher if we don't wait for
            // ongoing operations to complete. Resulting in once the watch service starts again that more than
            // expected watch records are processed.
            //
            // Note: new operations will fail now because the running has been set to false
            long startWait = System.currentTimeMillis();
            while (watchLocks.hasLockedKeys()) {
                TimeValue timeWaiting = new TimeValue(System.currentTimeMillis() - startWait);
                if (timeWaiting.getSeconds() > maxStopTimeout.getSeconds()) {
                    throw new ElasticsearchTimeoutException("timed out waiting for watches to complete, after waiting for [{}]",
                            timeWaiting);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    KeyedLock<String> getWatchLocks() {
        return watchLocks;
    }
}
