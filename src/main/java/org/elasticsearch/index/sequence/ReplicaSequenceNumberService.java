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

public class ReplicaSequenceNumberService extends AbstractIndexShardComponent {

    ConsecutiveObserver consecutiveObserver = new ConsecutiveObserver();

    volatile long currentTerm = -1;

    @Inject
    public ReplicaSequenceNumberService(ShardId shardId, @IndexSettings Settings indexSettings) {
        super(shardId, indexSettings);

        // no commit
        currentTerm = 0;
    }

    public void setCurrentTerm(long term) {
        assert term >= 0;
        assert term >= currentTerm;
        currentTerm = term;
    }

    public void observe(SequenceNo sequenceNo) {
        if (sequenceNo.term() == currentTerm) {
            consecutiveObserver.observe(sequenceNo.counter());
        }
    }

    public SequenceNo getMaxConsecutiveCounter() {
        return new SequenceNo(currentTerm, consecutiveObserver.maxConsequtiveValueSeen());
    }
}
