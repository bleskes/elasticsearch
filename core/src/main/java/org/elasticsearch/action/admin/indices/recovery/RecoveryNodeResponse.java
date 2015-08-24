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

package org.elasticsearch.action.admin.indices.recovery;

import com.google.common.collect.Lists;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.action.support.indices.BaseNodeBroadcastResponse;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.recovery.RecoveryState;

import java.io.IOException;
import java.util.List;

/**
 * Information regarding the recovery state of a shard.
 */
public class RecoveryNodeResponse extends BaseNodeBroadcastResponse implements ToXContent {

    List<RecoveryState> recoveryStates;

    public RecoveryNodeResponse() { }

    /**
     * Constructs shard recovery in formation for the given index and shard id.
     * @param nodeId Id of the node
     * @param totalShards The total number of shards for which the operation was performed
     * @param successfulShards The number of shards for which the operation was successful
     * @param exceptions The exceptions from the failed shards
     */
    public RecoveryNodeResponse(String nodeId, int totalShards, int successfulShards, List<BroadcastShardOperationFailedException> exceptions) {
        super(nodeId, totalShards, successfulShards, exceptions);
    }

    /**
     * Sets the recovery state information for the shard.
     *
     * @param recoveryStates Recovery states
     */
    public void setRecoveryStates(List<RecoveryState> recoveryStates) {
        this.recoveryStates = recoveryStates;
    }

    /**
     * Gets the recovery state information for the shard. Null if shard wasn't recovered / recovery didn't start yet.
     *
     * @return  Recovery state
     */
    @Nullable
    public List<RecoveryState> recoveryStates() {
        return recoveryStates;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startArray("recovery_states");
        for (RecoveryState recoveryState : recoveryStates) {
            recoveryState.toXContent(builder, params);
        }
        builder.endArray();
        return builder;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(recoveryStates.size());
        for (int i = 0; i < recoveryStates.size(); i++) {
            recoveryStates.get(i).writeTo(out);
        }
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int size = in.readVInt();
        recoveryStates = Lists.newArrayListWithCapacity(size);
        for (int i = 0; i < size; i++) {
            recoveryStates.add(RecoveryState.readRecoveryState(in));
        }
    }

    /**
     * Builds a new RecoveryNodeResponse from the give input stream.
     *
     * @param in    Input stream
     * @return      A new RecoveryNodeResponse
     * @throws IOException
     */
    public static RecoveryNodeResponse readShardRecoveryResponse(StreamInput in) throws IOException {
        RecoveryNodeResponse response = new RecoveryNodeResponse();
        response.readFrom(in);
        return response;
    }
}
