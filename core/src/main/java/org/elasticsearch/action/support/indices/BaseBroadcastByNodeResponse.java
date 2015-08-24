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

package org.elasticsearch.action.support.indices;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.transport.TransportResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseBroadcastByNodeResponse extends TransportResponse {
    protected String nodeId;
    protected int totalShards;
    protected int successfulShards;
    protected List<BroadcastShardOperationFailedException> exceptions;

    public BaseBroadcastByNodeResponse() {
    }

    public BaseBroadcastByNodeResponse(String nodeId,
                                       int totalShards,
                                       int successfulShards,
                                       List<BroadcastShardOperationFailedException> exceptions) {
        this.nodeId = nodeId;
        this.totalShards = totalShards;
        this.successfulShards = successfulShards;
        this.exceptions = exceptions;
    }

    public String getNodeId() {
        return nodeId;
    }

    public int getTotalShards() {
        return totalShards;
    }

    public int getSuccessfulShards() {
        return successfulShards;
    }

    public List<BroadcastShardOperationFailedException> getExceptions() {
        return exceptions;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        nodeId = in.readString();
        totalShards = in.readVInt();
        successfulShards = in.readVInt();
        if (in.readBoolean()) {
            int failureShards = in.readVInt();
            exceptions = new ArrayList<>(failureShards);
            for (int i = 0; i < failureShards; i++) {
                exceptions.add(new BroadcastShardOperationFailedException(in));
            }
        }
        else {
            exceptions = null;
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(nodeId);
        out.writeVInt(totalShards);
        out.writeVInt(successfulShards);
        out.writeBoolean(exceptions != null);
        if (exceptions != null) {
            int failureShards = exceptions.size();
            out.writeVInt(failureShards);
            for (int i = 0; i < failureShards; i++) {
                exceptions.get(i).writeTo(out);
            }
        }
    }
}
