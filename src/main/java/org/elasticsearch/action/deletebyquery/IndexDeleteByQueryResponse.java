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

package org.elasticsearch.action.deletebyquery;

import org.elasticsearch.Version;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.ActionWriteResponse;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Delete by query response executed on a specific index.
 */
public class IndexDeleteByQueryResponse extends ActionResponse {

    private String index;
    private ActionWriteResponse.ShardInfo shardInfo;

    IndexDeleteByQueryResponse(String index, List<ShardDeleteByQueryResponse> shardResponses, List<ShardOperationFailedException> failures) {
        this.index = index;
        this.shardInfo = new ActionWriteResponse.ShardInfo();
        this.shardInfo.append(shardResponses);
        // just append the primary failures:
        if (!failures.isEmpty()) {
            List<ActionWriteResponse.ShardInfo.Failure> k = new ArrayList<>();
            for (ShardOperationFailedException failure : failures) {
                // Set the status here, since it is a failure on primary shard
                // The failure doesn't include the node id, maybe add it to ShardOperationFailedException...
                k.add(new ActionWriteResponse.ShardInfo.Failure(failure.index(), failure.shardId(), null, failure.reason(), failure.status()));
            }
            k.addAll(Arrays.asList(this.shardInfo.getFailures()));
            this.shardInfo.setFailures(k.toArray(new ActionWriteResponse.ShardInfo.Failure[k.size()]));
        }
    }

    IndexDeleteByQueryResponse() {
    }

    /**
     * The index the delete by query operation was executed against.
     */
    public String getIndex() {
        return this.index;
    }

    public ActionWriteResponse.ShardInfo getShardInfo() {
        return shardInfo;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        index = in.readString();
        if (in.getVersion().before(Version.V_1_5_0)) {
            in.readVInt();
            in.readVInt();
            in.readVInt();
        }
        if (in.getVersion().onOrAfter(Version.V_1_5_0)) {
            shardInfo = in.readOptionalStreamable(new ActionWriteResponse.ShardInfo());
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(index);
        if (out.getVersion().before(Version.V_1_5_0)) {
            out.writeVInt(0);
            out.writeVInt(0);
            out.writeVInt(0);
        }
        if (out.getVersion().onOrAfter(Version.V_1_5_0)) {
            out.writeOptionalStreamable(shardInfo);
        }
    }
}