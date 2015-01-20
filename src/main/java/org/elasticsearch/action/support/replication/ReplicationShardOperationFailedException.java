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

package org.elasticsearch.action.support.replication;

import org.elasticsearch.ElasticsearchWrapperException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.index.sequence.SequenceNo;
import org.elasticsearch.index.shard.IndexShardException;
import org.elasticsearch.index.shard.ShardId;

/**
 * An exception indicating that a failure occurred performing an operation on a (primary) shard.
 * Potentially containing a seq number that should go to all replicas, notifying them of it.
 */
public class ReplicationShardOperationFailedException extends IndexShardException implements ElasticsearchWrapperException {

    @Nullable
    final private SequenceNo sequenceNo;

    public ReplicationShardOperationFailedException(ShardId shardId, Throwable cause) {
        this(shardId, cause, null);
    }

    public ReplicationShardOperationFailedException(ShardId shardId, Throwable cause, @Nullable SequenceNo sequenceNo) {
        super(shardId, "", cause);
        this.sequenceNo = sequenceNo;
    }

    public SequenceNo sequenceNo() {
        return sequenceNo;
    }
}