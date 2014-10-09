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

package org.elasticsearch.action;

import org.elasticsearch.Version;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for write action responses.
 */
public abstract class ActionWriteResponse extends ActionResponse {

    private ShardInfo shardInfo;

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        if (in.getVersion().onOrAfter(Version.V_1_5_0)) {
            shardInfo = in.readOptionalStreamable(new ShardInfo());
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        if (out.getVersion().onOrAfter(Version.V_1_5_0)) {
            out.writeOptionalStreamable(shardInfo);
        }
    }

    public ShardInfo getShardInfo() {
        return shardInfo;
    }

    public void setShardInfo(ShardInfo shardInfo) {
        this.shardInfo = shardInfo;
    }

    public static class ShardInfo implements Streamable, ToXContent {

        private int total;
        private int successful;
        private int pending;
        private Failure[] failures = new Failure[0];

        /**
         * @return the total number of shards the write should go to.
         */
        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        /**
         * @return the total number of shards the write actually succeeded on.
         */
        public int getSuccessful() {
            return successful;
        }

        public void setSuccessful(int successful) {
            this.successful = successful;
        }

        /**
         * @return the total number of shards a write is still to be performed on at the time this response was
         * created. Typically this will only contain 0, but when async replication is used this number is higher than 0.
         */
        public int getPending() {
            return pending;
        }

        public void setPending(int pending) {
            this.pending = pending;
        }

        /**
         * @return The total number of replication failures.
         */
        public int getFailed() {
            return failures.length;
        }

        /**
         * @return The replication failures that have been captured in the case writes have failed on replica shards.
         */
        public Failure[] getFailures() {
            return failures;
        }

        public void setFailures(Failure[] failures) {
            this.failures = failures;
        }

        public <T extends ActionWriteResponse> void append(List<T> responses) {
            List<Failure> failures = new ArrayList<>();
            for (ActionWriteResponse response : responses) {
                total += response.getShardInfo().getTotal();
                successful += response.getShardInfo().getSuccessful();
                pending += response.getShardInfo().getPending();
                if (response.getShardInfo().failures.length > 0) {
                    failures.addAll(Arrays.asList(response.getShardInfo().failures));
                }
            }
            if (!failures.isEmpty()) {
                failures.addAll(Arrays.asList(this.failures));
                this.failures = failures.toArray(new Failure[failures.size()]);
            }
        }

        public RestStatus status() {
            RestStatus status = RestStatus.OK;
            for (Failure failure : failures) {
                if (failure.getStatus().getStatus() > status.getStatus()) {
                    status = failure.getStatus();
                }
            }
            return status;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            total = in.readVInt();
            successful = in.readVInt();
            pending = in.readVInt();
            int size = in.readVInt();
            failures = new Failure[size];
            for (int i = 0; i < size; i++) {
                Failure failure = new Failure();
                failure.readFrom(in);
                failures[i] = failure;
            }
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeVInt(total);
            out.writeVInt(successful);
            out.writeVInt(pending);
            out.writeVInt(failures.length);
            for (Failure failure : failures) {
                failure.writeTo(out);
            }
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject(Fields._SHARDS);
            builder.field(Fields.TOTAL, total);
            builder.field(Fields.SUCCESSFUL, successful);
            if (pending > 0) {
                builder.field(Fields.PENDING, pending);
            }

            builder.field(Fields.FAILED, getFailed());
            if (failures.length > 0) {
                builder.startArray(Fields.FAILURES);
                for (Failure failure : failures) {
                    failure.toXContent(builder, params);
                }
                builder.endArray();
            }
            return builder.endObject();
        }

        public static class Failure implements Streamable, ToXContent {

            private String index;
            private int shardId;
            private String nodeId;
            private String reason;
            private RestStatus status;

            public Failure(String index, int shardId, String nodeId, String reason) {
                this.index = index;
                this.shardId = shardId;
                this.nodeId = nodeId;
                this.reason = reason;
                this.status = RestStatus.OK; // <-- Replica failures are ok and can happen.
            }

            public Failure(String index, int shardId, String nodeId, String reason, RestStatus status) {
                this.index = index;
                this.shardId = shardId;
                this.nodeId = nodeId;
                this.reason = reason;
                this.status = status;
            }

            Failure() {
            }

            public String getIndex() {
                return index;
            }

            public int getShardId() {
                return shardId;
            }

            public String getNodeId() {
                return nodeId;
            }

            public String getReason() {
                return reason;
            }

            public RestStatus getStatus() {
                return status;
            }

            @Override
            public void readFrom(StreamInput in) throws IOException {
                index = in.readString();
                shardId = in.readVInt();
                nodeId = in.readOptionalString();
                reason = in.readString();
                status = RestStatus.readFrom(in);
            }

            @Override
            public void writeTo(StreamOutput out) throws IOException {
                out.writeString(index);
                out.writeVInt(shardId);
                out.writeOptionalString(nodeId);
                out.writeString(reason);
                RestStatus.writeTo(out, status);
            }

            @Override
            public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                builder.startObject();
                builder.field(Fields._INDEX, index);
                builder.field(Fields._SHARD, shardId);
                builder.field(Fields._NODE, nodeId);
                builder.field(Fields.REASON, reason);
                builder.field(Fields.STATUS, status);
                builder.endObject();
                return builder;
            }

            private static class Fields {

                private static final XContentBuilderString _INDEX = new XContentBuilderString("_index");
                private static final XContentBuilderString _SHARD = new XContentBuilderString("_shard");
                private static final XContentBuilderString _NODE = new XContentBuilderString("_node");
                private static final XContentBuilderString REASON = new XContentBuilderString("reason");
                private static final XContentBuilderString STATUS = new XContentBuilderString("status");

            }
        }

        private static class Fields {

            private static final XContentBuilderString _SHARDS = new XContentBuilderString("_shards");
            private static final XContentBuilderString TOTAL = new XContentBuilderString("total");
            private static final XContentBuilderString SUCCESSFUL = new XContentBuilderString("successful");
            private static final XContentBuilderString PENDING = new XContentBuilderString("pending");
            private static final XContentBuilderString FAILED = new XContentBuilderString("failed");
            private static final XContentBuilderString FAILURES = new XContentBuilderString("failures");

        }
    }
}
