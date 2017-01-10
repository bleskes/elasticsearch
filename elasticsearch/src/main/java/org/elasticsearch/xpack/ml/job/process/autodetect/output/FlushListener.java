/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.job.process.autodetect.output;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class FlushListener {

    final ConcurrentMap<String, CountDownLatch> awaitingFlushed = new ConcurrentHashMap<>();
    final AtomicBoolean cleared = new AtomicBoolean(false);

    boolean waitForFlush(String flushId, long timeout) {
        if (cleared.get()) {
            return false;
        }

        CountDownLatch latch = awaitingFlushed.computeIfAbsent(flushId, (key) -> new CountDownLatch(1));
        try {
            return latch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            // the flush id will no longer be used from this point, so we can remove it.
            awaitingFlushed.remove(flushId);
        }
    }

    void acknowledgeFlush(String flushId) {
        // acknowledgeFlush(...) could be called before waitForFlush(...)
        // a flush api call writes a flush command to the analytical process and then via a different thread the
        // result reader then reads whether the flush has been acked.
        CountDownLatch latch = awaitingFlushed.computeIfAbsent(flushId, (key) -> new CountDownLatch(1));
        latch.countDown();
    }

    void clear() {
        if (cleared.compareAndSet(false, true)) {
            Iterator<ConcurrentMap.Entry<String, CountDownLatch>> latches = awaitingFlushed.entrySet().iterator();
            while (latches.hasNext()) {
                latches.next().getValue().countDown();
                latches.remove();
            }
        }
    }

}
