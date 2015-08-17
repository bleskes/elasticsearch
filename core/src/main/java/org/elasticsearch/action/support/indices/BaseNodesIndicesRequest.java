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

import com.google.common.collect.Lists;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.OriginalIndices;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.transport.TransportRequest;

import java.io.IOException;
import java.util.List;

public abstract class BaseNodesIndicesRequest<T extends IndicesLevelRequest> extends TransportRequest implements IndicesRequest {
    private String nodeId;

    private List<ShardRouting> shards;

    protected OriginalIndices originalIndices;

    protected T indicesLevelRequest;

    protected BaseNodesIndicesRequest() {
    }

    protected BaseNodesIndicesRequest(String nodeId, T request, List<ShardRouting> shards) {
        super(request);
        this.indicesLevelRequest = request;
        this.shards = shards;
        this.nodeId = nodeId;
        this.originalIndices = new OriginalIndices(request);
    }

    public List<ShardRouting> getShards() {
        return shards;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String[] indices() {
        return originalIndices.indices();
    }

    public IndicesOptions indicesOptions() {
        return originalIndices.indicesOptions();
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        indicesLevelRequest = newRequest();
        indicesLevelRequest.readFrom(in);
        int size = in.readVInt();
        shards = Lists.newArrayListWithCapacity(size);
        for (int i = 0; i < size; i++) {
            shards.add(ShardRouting.readShardRoutingEntry(in));
        }
        nodeId = in.readString();
        originalIndices = OriginalIndices.readOriginalIndices(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        indicesLevelRequest.writeTo(out);
        int size = shards.size();
        out.writeVInt(size);
        for (int i = 0; i < size; i++) {
            shards.get(i).writeTo(out);
        }
        out.writeString(nodeId);
        OriginalIndices.writeOriginalIndices(originalIndices, out);
    }

    protected abstract T newRequest();

    public T getIndicesLevelRequest() {
        return indicesLevelRequest;
    }
}
