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

import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.common.util.concurrent.KeyedLock;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class AlertLockService {

    private final KeyedLock<String> alertLock = new KeyedLock<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public Lock acquire(String name) {
        if (!running.get()) {
            throw new ElasticsearchIllegalStateException("not started");
        }

        alertLock.acquire(name);
        return new Lock(name, alertLock);
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            // init
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            // It can happen we have still ongoing operations and we wait those operations to finish to avoid
            // that AlertManager or any of its components end up in a illegal state after the state as been set to stopped.
            //
            // For example: An alert action entry may be added while we stopping alerting if we don't wait for
            // ongoing operations to complete. Resulting in once the alert service starts again that more than
            // expected alert action entries are processed.
            //
            // Note: new operations will fail now because the running has been set to false
            while (alertLock.hasLockedKeys()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    KeyedLock<String> getAlertLock() {
        return alertLock;
    }

    public static class Lock {

        private final String name;
        private final KeyedLock<String> alertLock;

        private Lock(String name, KeyedLock<String> alertLock) {
            this.name = name;
            this.alertLock = alertLock;

        }

        public void release() {
            alertLock.release(name);
        }
    }
}
