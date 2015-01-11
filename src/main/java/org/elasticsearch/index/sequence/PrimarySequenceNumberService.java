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

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.index.shard.AbstractIndexShardComponent;
import org.elasticsearch.index.shard.ShardId;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class PrimarySequenceNumberService extends AbstractIndexShardComponent {

    final AtomicLong sequenceCounterGenerator = new AtomicLong(-1);

    final AtomicReference<SequenceNo> consensusSeqNo = new AtomicReference<>(SequenceNo.UNKNOWN);

    volatile long currentTerm = -1;

    // quorum size, not including the primary
    volatile int quorumSize;

    // TODO: heavy, speedup
    // nocommit
    final private AtomicReferenceArray<SequenceNo> replicaMaxConsecutiveCounters;


    @Inject
    public PrimarySequenceNumberService(ShardId shardId, @IndexSettings Settings indexSettings) {
        super(shardId, indexSettings);

        // TODO: nocommit settings + terms;
        replicaMaxConsecutiveCounters = new AtomicReferenceArray<>(1);
        quorumSize = 1; // assume all replicas are assigned
        setCurrentTerm(0);
    }

    public void setCurrentTerm(long term) {
        assert term >= 0;
        assert term >= currentTerm;
        currentTerm = term;
    }

    public SequenceNo getNextSequenceNo() {
        return new SequenceNo(currentTerm, sequenceCounterGenerator.incrementAndGet());
    }

    public void processShardResponses(AtomicReferenceArray<SequenceNo> replicaMaxConsecutiveCounters) {
        assert replicaMaxConsecutiveCounters.length() == this.replicaMaxConsecutiveCounters.length();
        // TODO: speed up - this is done for ease of programming but will be called on every indexing request
        SequenceNo[] sortedSeqNo = new SequenceNo[replicaMaxConsecutiveCounters.length()];
        for (int i = 0; i < replicaMaxConsecutiveCounters.length(); i++) {
            SequenceNo newSeqNo = replicaMaxConsecutiveCounters.get(i);
            SequenceNo currentSeqNo = replicaMaxConsecutiveCounters.get(i);
            while (newSeqNo.largerThan(currentSeqNo)) {
                if (this.replicaMaxConsecutiveCounters.compareAndSet(i, currentSeqNo, newSeqNo)) {
                    currentSeqNo = newSeqNo;
                    break;
                }
                currentSeqNo = this.replicaMaxConsecutiveCounters.get(i);
            }
            sortedSeqNo[i] = currentSeqNo;
        }
        Arrays.sort(sortedSeqNo, new Comparator<SequenceNo>() {
            @Override
            public int compare(SequenceNo o1, SequenceNo o2) {
                return -o1.compareTo(o2);
            }
        });

        // TODO: deal with the case where we have a single assigned shard
        SequenceNo quorumSeq = sortedSeqNo[quorumSize - 1];
        SequenceNo currentConsensus = consensusSeqNo.get();
        while (quorumSeq.largerThan(currentConsensus)) {
            if (consensusSeqNo.compareAndSet(currentConsensus, quorumSeq)) {
                break;
            }
            currentConsensus = consensusSeqNo.get();
        }
    }
}
