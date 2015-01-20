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

import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.index.shard.AbstractIndexShardComponent;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.threadpool.ThreadPool;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SequenceNumbersService extends AbstractIndexShardComponent {

    final ConsecutiveObserver consecutiveCounterObserver = new ConsecutiveObserver();
    final ConsensusNumberService consensusNumberService;
    final SequenceNumberGenerator seqNoGenerator;
    final ClusterService clusterService;
    final ThreadPool threadPool;
    final TransportConsensusUpdateAction consensusUpdateAction;

    volatile TimeValue consensusUpdateInterval;
    AtomicReference<ConsensusUpdater> consensusUpdater = new AtomicReference<>();

    volatile long currentTerm = -1;

    @Inject
    public SequenceNumbersService(ShardId shardId, @IndexSettings Settings indexSettings, ClusterService clusterService, ThreadPool threadPool, TransportConsensusUpdateAction consensusUpdateAction) {
        super(shardId, indexSettings);
        this.clusterService = clusterService;
        this.threadPool = threadPool;
        this.consensusUpdateAction = consensusUpdateAction;
        this.consensusNumberService = new ConsensusNumberService(logger);
        ;

        // TODO: settings
        consensusUpdateInterval = new TimeValue(1000);

        // no commit
        currentTerm = 0;
        seqNoGenerator = new SequenceNumberGenerator(logger, this);
    }

    public void startConsensusUpdater() {
        ConsensusUpdater consensusUpdater = new ConsensusUpdater();
        consensusUpdater.reschedule();
        ConsensusUpdater old = this.consensusUpdater.getAndSet(consensusUpdater);
        if (old != null) {
            old.stop = true;
        }
    }

    public void close() {
        ConsensusUpdater old = this.consensusUpdater.getAndSet(null);
        if (old != null) {
            old.stop = true;
        }
    }

    public void currentTerm(long term) {
        assert term >= 0;
        assert term >= currentTerm;
        currentTerm = term;
    }

    public long currentTerm() {
        return currentTerm;
    }

    public SequenceNo consensusSeqNo() {
        return consensusNumberService.consensusSeqNo();
    }

    public void notifyUsedSeqNo(SequenceNo sequenceNo) {
        seqNoGenerator.observe(sequenceNo);
        if (sequenceNo.term() == currentTerm) {
            consecutiveCounterObserver.observe(sequenceNo.counter());
        }
    }

    public SequenceNo maxConsecutiveSeqNo() {
        long counter = consecutiveCounterObserver.maxConsequtiveValueSeen();
        if (counter < 0) {
            return SequenceNo.UNKNOWN;
        }
        return new SequenceNo(currentTerm, counter);
    }

    public SequenceNumberGenerator seqNoGenerator() {
        return seqNoGenerator;
    }

    public boolean updateConsensusSeqNo(Map<String, SequenceNo> replicaMaxConsecutiveSeqNo) {
        boolean b = consensusNumberService.processShardMaxConsecCounters(clusterService.localNode().id(),
                new SequenceNo(currentTerm, consecutiveCounterObserver.maxConsequtiveValueSeen()),
                replicaMaxConsecutiveSeqNo);
        assert consensusSeqNo().term() <= currentTerm;
        return b;
    }

    public void incrementConsensusSeqNo(SequenceNo sequenceNo) {
        consensusNumberService.incrementConsensusSeqNo(sequenceNo);
    }

    public SequenceStats stats() {
        return new SequenceStats(consensusSeqNo(), maxConsecutiveSeqNo(), seqNoGenerator().current());
    }

    class ConsensusUpdater extends AbstractRunnable {

        public volatile boolean stop;
        volatile SequenceNo lastProcessedNo;

        public void reschedule() {
            if (!stop) {
                threadPool.schedule(consensusUpdateInterval, ThreadPool.Names.GENERIC, this);
            }
        }

        @Override
        public void onFailure(Throwable t) {
            logger.warn("unexpected failure in consensus updater. re-scheduling.", t);
            reschedule();
        }

        @Override
        protected void doRun() throws Exception {
            if (stop) {
                return;
            }
            SequenceNo currentNo = consensusSeqNo();
            if (!currentNo.equals(lastProcessedNo)) {
                logger.trace("updating consensus# on replicas (current [{}])", currentNo);
                consensusUpdateAction.updateConsensus(shardId());
                lastProcessedNo = currentNo;
            }
            reschedule();
        }
    }

}