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

import org.elasticsearch.common.Nullable;

import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class FlushListener {

    final ConcurrentMap<String, FlushAcknowledgementHolder> awaitingFlushed = new ConcurrentHashMap<>();
    final AtomicBoolean cleared = new AtomicBoolean(false);

    @Nullable
    FlushAcknowledgement waitForFlush(String flushId, Duration timeout) {
        if (cleared.get()) {
            return null;
        }

        FlushAcknowledgementHolder holder = awaitingFlushed.computeIfAbsent(flushId, (key) -> new FlushAcknowledgementHolder(flushId));
        try {
            if (holder.latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                return holder.flushAcknowledgement;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    void acknowledgeFlush(FlushAcknowledgement flushAcknowledgement) {
        // acknowledgeFlush(...) could be called before waitForFlush(...)
        // a flush api call writes a flush command to the analytical process and then via a different thread the
        // result reader then reads whether the flush has been acked.
        String flushId = flushAcknowledgement.getId();
        FlushAcknowledgementHolder holder = awaitingFlushed.computeIfAbsent(flushId, (key) -> new FlushAcknowledgementHolder(flushId));
        holder.flushAcknowledgement = flushAcknowledgement;
        holder.latch.countDown();
    }

    void clear(String flushId) {
        awaitingFlushed.remove(flushId);
    }

    void clear() {
        if (cleared.compareAndSet(false, true)) {
            Iterator<ConcurrentMap.Entry<String, FlushAcknowledgementHolder>> latches = awaitingFlushed.entrySet().iterator();
            while (latches.hasNext()) {
                latches.next().getValue().latch.countDown();
                latches.remove();
            }
        }
    }

    private static class FlushAcknowledgementHolder {

        private final CountDownLatch latch;
        private volatile FlushAcknowledgement flushAcknowledgement;

        private FlushAcknowledgementHolder(String flushId) {
            this.flushAcknowledgement = new FlushAcknowledgement(flushId, null);
            this.latch = new CountDownLatch(1);
        }
    }
}
