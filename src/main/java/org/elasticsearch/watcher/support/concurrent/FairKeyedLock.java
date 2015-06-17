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

package org.elasticsearch.watcher.support.concurrent;

import org.elasticsearch.common.util.concurrent.ConcurrentCollections;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 */
public class FairKeyedLock<T>  {

    private final ConcurrentMap<T, KeyLock> map = ConcurrentCollections.newConcurrentMap();

    protected final ThreadLocal<KeyLock> threadLocal = new ThreadLocal<>();

    public void acquire(T key) {
        while (true) {
            if (threadLocal.get() != null) {
                // if we are here, the thread already has the lock
                throw new IllegalArgumentException("Lock already acquired in Thread" + Thread.currentThread().getId()
                        + " for key " + key);
            }
            KeyLock perNodeLock = map.get(key);
            if (perNodeLock == null) {
                KeyLock newLock = new KeyLock(true);
                perNodeLock = map.putIfAbsent(key, newLock);
                if (perNodeLock == null) {
                    newLock.lock();
                    threadLocal.set(newLock);
                    return;
                }
            }
            assert perNodeLock != null;
            int i = perNodeLock.count.get();
            if (i > 0 && perNodeLock.count.compareAndSet(i, i + 1)) {
                perNodeLock.lock();
                threadLocal.set(perNodeLock);
                return;
            }
        }
    }

    public boolean tryAcquire(T key, long timeout, TimeUnit timeUnit) throws InterruptedException {
        while (true) {
            if (threadLocal.get() != null) {
                // if we are here, the thread already has the lock
                throw new IllegalArgumentException("Lock already acquired in Thread" + Thread.currentThread().getId()
                        + " for key " + key);
            }
            KeyLock perNodeLock = map.get(key);
            if (perNodeLock == null) {
                KeyLock newLock = new KeyLock(true);
                perNodeLock = map.putIfAbsent(key, newLock);
                if (perNodeLock == null) {
                    if (newLock.tryLock(timeout, timeUnit)) {
                        threadLocal.set(newLock);
                        return true;
                    }
                    return false;
                }
            }
            assert perNodeLock != null;
            int i = perNodeLock.count.get();
            if (i > 0 && perNodeLock.count.compareAndSet(i, i + 1)) {
                if (perNodeLock.tryLock(timeout, timeUnit)) {
                    threadLocal.set(perNodeLock);
                    return true;
                }
                return false;
            }
        }
    }

    public void release(T key) {
        KeyLock lock = threadLocal.get();
        if (lock == null) {
            throw new IllegalArgumentException("Lock not acquired");
        }
        release(key, lock);
    }

    void release(T key, KeyLock lock) {
        assert lock.isHeldByCurrentThread();
        assert lock == map.get(key);
        lock.unlock();
        threadLocal.set(null);
        int decrementAndGet = lock.count.decrementAndGet();
        if (decrementAndGet == 0) {
            map.remove(key, lock);
        }
    }


    @SuppressWarnings("serial")
    private final static class KeyLock extends ReentrantLock {
        private final AtomicInteger count = new AtomicInteger(1);

        public KeyLock(boolean fair) {
            super(fair);
        }
    }

    public boolean hasLockedKeys() {
        return !map.isEmpty();
    }

    /**
     * A {@link FairKeyedLock} that allows to acquire a global lock that guarantees
     * exclusive access to the resource the KeyedLock is guarding.
     */
    public final static class GlobalLockable<T> extends FairKeyedLock<T> {

        private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

        @Override
        public void acquire(T key) {
            boolean success = false;
            lock.readLock().lock();
            try {
                super.acquire(key);
                success = true;
            } finally {
                if (!success) {
                    lock.readLock().unlock();
                }
            }
        }

        @Override
        public void release(T key) {
            KeyLock keyLock = threadLocal.get();
            if (keyLock == null) {
                throw new IllegalArgumentException("Lock not acquired");
            }
            try {
                release(key, keyLock);
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * Returns a global lock guaranteeing exclusive access to the resource
         * this KeyedLock is guarding.
         */
        public Lock globalLock() {
            return lock.writeLock();
        }
    }


}
