/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.metadata;

import org.elasticsearch.cluster.AbstractDiffable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Objects;

public class Allocation extends AbstractDiffable<Allocation> implements ToXContent {

    private static final ParseField NODE_ID_FIELD = new ParseField("node_id");
    private static final ParseField JOB_ID_FIELD = new ParseField("job_id");

    static final Allocation PROTO = new Allocation(null, null);

    static final ObjectParser<Builder, ParseFieldMatcherSupplier> PARSER = new ObjectParser<>("allocation", Builder::new);

    static {
        PARSER.declareString(Builder::setNodeId, NODE_ID_FIELD);
        PARSER.declareString(Builder::setJobId, JOB_ID_FIELD);
    }

    private final String nodeId;
    private final String jobId;

    public Allocation(String nodeId, String jobId) {
        this.nodeId = nodeId;
        this.jobId = jobId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getJobId() {
        return jobId;
    }

    @Override
    public Allocation readFrom(StreamInput in) throws IOException {
        String nodeId = in.readString();
        String jobId = in.readString();
        return new Allocation(nodeId, jobId);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(nodeId);
        out.writeString(jobId);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(NODE_ID_FIELD.getPreferredName(), nodeId);
        builder.field(JOB_ID_FIELD.getPreferredName(), jobId);
        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Allocation that = (Allocation) o;
        return Objects.equals(nodeId, that.nodeId) &&
                Objects.equals(jobId, that.jobId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, jobId);
    }

    public static class Builder {

        private String nodeId;
        private String jobId;

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public Allocation build() {
            return new Allocation(nodeId, jobId);
        }

    }
}
