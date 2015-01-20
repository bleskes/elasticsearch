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

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ConsensusNumberService {

    final AtomicReference<SequenceNo> consensusSeqNo = new AtomicReference<>(SequenceNo.UNKNOWN);
    final ESLogger logger;

    // quorum size, including the primary
    volatile int quorumSize;

    // TODO: this is currently locked, potentially optimize for reads
    final private Map<String, SequenceNo> shardMaxConsecutiveCounters = new HashMap<>();


    public ConsensusNumberService(ESLogger logger) {
        this.logger = logger;
        // TODO: nocommit settings + terms;
        quorumSize = 1;
    }

    public SequenceNo consensusSeqNo() {
        return consensusSeqNo.get();
    }

    // TODO: speed up and de-synchronize - this is done for ease of programming but will be called on every indexing request
    public boolean processShardMaxConsecCounters(String primaryNodeId, SequenceNo primaryMaxConesutiveSeqNo, Map<String, SequenceNo> replicaMaxConsecutiveSeqNo) {
        ArrayList<SequenceNo> sortedSeqNo = new ArrayList<>(quorumSize * 2);
        synchronized (this) {
            maybeUpdateNodeSeqNo(sortedSeqNo, primaryNodeId, primaryMaxConesutiveSeqNo);
            for (Map.Entry<String, SequenceNo> node : replicaMaxConsecutiveSeqNo.entrySet()) {
                maybeUpdateNodeSeqNo(sortedSeqNo, node.getKey(), node.getValue());
            }
        }
        Collections.sort(sortedSeqNo, new Comparator<SequenceNo>() {
            @Override
            public int compare(SequenceNo o1, SequenceNo o2) {
                return -o1.compareTo(o2);
            }
        });

        // TODO: deal with the case where we have a single assigned shard
        SequenceNo quorumSeq = sortedSeqNo.get(quorumSize - 1);
        SequenceNo currentConsensus = consensusSeqNo.get();
        while (quorumSeq.largerThan(currentConsensus)) {
            if (consensusSeqNo.compareAndSet(currentConsensus, quorumSeq)) {
                return true;
            }
            currentConsensus = consensusSeqNo.get();
        }
        return false;
    }

    private void maybeUpdateNodeSeqNo(ArrayList<SequenceNo> currentSeqNos, String nodeId, SequenceNo newSeqNo) {
        SequenceNo currentSeqNo;
        currentSeqNo = shardMaxConsecutiveCounters.get(nodeId);
        if (currentSeqNo == null || newSeqNo.largerThan(currentSeqNo)) {
            shardMaxConsecutiveCounters.put(nodeId, newSeqNo);
            currentSeqNo = newSeqNo;
        }
        currentSeqNos.add(currentSeqNo);
    }

    public void incrementConsensusSeqNo(SequenceNo sequenceNo) {
        while (true) {
            SequenceNo current = consensusSeqNo.get();
            if (current.largerThan(sequenceNo)) {
                logger.trace("ignoring consensus seq no increment as current [{}] is higher than [{}]", current, sequenceNo);
                return;
            }
            if (consensusSeqNo.compareAndSet(current, sequenceNo)) {
                logger.trace("incrementing consensus seq no with [{}]", sequenceNo);
                return;
            }
        }
    }
}
