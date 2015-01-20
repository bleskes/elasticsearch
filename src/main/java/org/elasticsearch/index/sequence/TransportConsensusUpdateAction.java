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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionWriteResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.replication.ShardReplicationOperationReplicaResponse;
import org.elasticsearch.action.support.replication.ShardReplicationOperationRequest;
import org.elasticsearch.action.support.replication.TransportShardReplicationOperationAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.action.shard.ShardStateAction;
import org.elasticsearch.cluster.routing.ShardIterator;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;

import static org.elasticsearch.action.ValidateActions.addValidationError;

public class TransportConsensusUpdateAction extends TransportShardReplicationOperationAction<TransportConsensusUpdateAction.Request, TransportConsensusUpdateAction.Request, TransportConsensusUpdateAction.Response> {

    final public static String NAME = "indices:seq_no/update_consensus";

    @Inject
    public TransportConsensusUpdateAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                          IndicesService indicesService, ThreadPool threadPool, ShardStateAction shardStateAction,
                                          ActionFilters actionFilters) {
        super(settings, NAME, transportService, clusterService, indicesService, threadPool, shardStateAction, actionFilters);
    }

    @Override
    protected Request newRequestInstance() {
        return new Request();
    }

    @Override
    protected Request newReplicaRequestInstance() {
        return new Request();
    }

    @Override
    protected Response newResponseInstance() {
        return new Response();
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    protected Tuple<Response, Request> shardOperationOnPrimary(ClusterState clusterState, PrimaryOperationRequest shardRequest) {
        IndexShard indexShard = indicesService.indexServiceSafe(shardRequest.shardId.index().name()).shardSafe(shardRequest.shardId.id());
        SequenceNo consensusSeqNo = indexShard.seqNoService().consensusSeqNo();
        logger.trace("updating replicas of {} consensus SeqNo to [{}]", shardRequest.shardId, consensusSeqNo);
        return new Tuple<>(new Response(), new Request(shardRequest.shardId, consensusSeqNo));
    }

    @Override
    protected ShardReplicationOperationReplicaResponse shardOperationOnReplica(ReplicaOperationRequest shardRequest) {
        IndexShard indexShard = indicesService.indexServiceSafe(shardRequest.shardId.index().name()).shardSafe(shardRequest.shardId.id());
        indexShard.seqNoService().incrementConsensusSeqNo(shardRequest.request.consensusSeqNo());
        return new ShardReplicationOperationReplicaResponse(indexShard.seqNoService().maxConsecutiveSeqNo());
    }

    @Override
    protected ShardIterator shards(ClusterState clusterState, InternalRequest request) throws ElasticsearchException {
        return clusterService.operationRouting().getShards(clusterService.state(), request.concreteIndex(), request.request().shard, null);
    }

    @Override
    protected boolean checkWriteConsistency() {
        return false;
    }

    @Override
    protected boolean resolveIndex() {
        return false;
    }

    public void updateConsensus(ShardId shardId) {
        execute(new Request(shardId));
    }

    final static class Request extends ShardReplicationOperationRequest<Request> {

        private SequenceNo consensusSeqNo;
        private int shard;

        Request() {
            consensusSeqNo = SequenceNo.UNKNOWN;
            shard = -1;
        }

        Request(ShardId shardId) {
            this();
            index = shardId.index().name();
            shard = shardId.id();
        }

        Request(ShardId shardId, SequenceNo consensusSeqNo) {
            this(shardId);
            this.consensusSeqNo = consensusSeqNo;
        }

        public SequenceNo consensusSeqNo() {
            return consensusSeqNo;
        }

        @Override
        public ActionRequestValidationException validate() {
            ActionRequestValidationException validationException = super.validate();
            if (shard < 0) {
                validationException = addValidationError("shard is missing", validationException);
            }
            return validationException;

        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeInt(shard);
            consensusSeqNo.writeTo(out);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            shard = in.readInt();
            consensusSeqNo = SequenceNo.readFrom(in);
        }
    }

    final static class Response extends ActionWriteResponse {

    }
}
