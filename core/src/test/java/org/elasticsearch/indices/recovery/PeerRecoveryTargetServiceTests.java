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

package org.elasticsearch.indices.recovery;

import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.mapper.SourceToParse;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.index.shard.IndexShardTestCase;

import static org.elasticsearch.indices.recovery.PeerRecoveryTargetService.shouldTryOpsRecovery;
import static org.hamcrest.Matchers.equalTo;

public class PeerRecoveryTargetServiceTests extends IndexShardTestCase {

    public void testGetStartingSeqNo() throws Exception {
        IndexShard replica = newShard(false);
        OpsRecoveryTarget recoveryTarget = null;
        try {
            recoveryEmptyReplica(replica);
            int docs = randomIntBetween(1, 10);
            final String index = replica.shardId().getIndexName();
            long seqNo = 0;
            for (int i = 0; i < docs; i++) {
                Engine.Index indexOp = replica.prepareIndexOnReplica(
                    SourceToParse.source(SourceToParse.Origin.REPLICA, index, "type", "doc_" + i, new BytesArray("{}"), XContentType.JSON),
                    seqNo++, 1, VersionType.EXTERNAL, IndexRequest.UNSET_AUTO_GENERATED_TIMESTAMP, false);
                replica.index(indexOp);
                if (rarely()) {
                    // insert a gap
                    seqNo++;
                }
            }

            recoveryTarget = new OpsRecoveryTarget(replica, null, null);

            final long maxSeqNo = replica.seqNoStats().getMaxSeqNo();
            final long localCheckpoint = replica.getLocalCheckpoint();

            assertFalse("shard is not ready for ops recovery, global checkpoint is not known", shouldTryOpsRecovery(replica));

            replica.updateGlobalCheckpointOnReplica(maxSeqNo - 1);
            replica.getTranslog().sync();

            // commit is enough, global checkpoint is below max *committed* which is NO_OPS_PERFORMED
            assertTrue("shard is should be ready for ops recovery", shouldTryOpsRecovery(replica));
            assertThat(
                ((StartOpsRecoveryRequest)recoveryTarget.createStartRecoveryRequest(null, null)).getStartingSeqNo(),
                equalTo(localCheckpoint + 1));

            replica.flush(new FlushRequest());

            assertFalse("commit is still not good enough, global checkpoint is below max", shouldTryOpsRecovery(replica));

            replica.updateGlobalCheckpointOnReplica(maxSeqNo);
            replica.getTranslog().sync();
            assertTrue("commit is enough, global checkpoint is below max", shouldTryOpsRecovery(replica));
            assertThat(
                ((StartOpsRecoveryRequest)recoveryTarget.createStartRecoveryRequest(null, null)).getStartingSeqNo(),
                equalTo(localCheckpoint + 1));
        } finally {
            closeShards(replica);
            if (recoveryTarget != null) {
                recoveryTarget.decRef();
            }
        }
    }
}
