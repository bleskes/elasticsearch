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
package org.elasticsearch.index.sequence;

import org.elasticsearch.common.logging.ESLogger;

import java.util.concurrent.atomic.AtomicLong;

public class SequenceNumberGenerator {

    final AtomicLong counterGenerator = new AtomicLong(-1);
    final SequenceNumbersService seqNoService;
    final ESLogger logger;

    public SequenceNumberGenerator(ESLogger logger, SequenceNumbersService sequenceNumbersServices) {
        this.logger = logger;
        this.seqNoService = sequenceNumbersServices;
    }

    public SequenceNo getNextSequenceNo() {
        SequenceNo sequenceNo = new SequenceNo(seqNoService.currentTerm(), counterGenerator.incrementAndGet());
        if (logger.isTraceEnabled()) {
            logger.trace("generated [{}]", sequenceNo);
        }
        return sequenceNo;
    }

    public SequenceNo current() {
        long counter = counterGenerator.get();
        if (counter < 0) {
            return SequenceNo.UNKNOWN;
        }
        return new SequenceNo(seqNoService.currentTerm(), counter);
    }

    public void observe(SequenceNo sequenceNo) {
        long counter = counterGenerator.get();
        while (counter < sequenceNo.counter()) {
            if (counterGenerator.compareAndSet(counter, sequenceNo.counter())) {
                logger.trace("maximum counter seen set to [{}]", sequenceNo.counter());
                return;
            }
            counter = counterGenerator.get();
        }
    }
}
