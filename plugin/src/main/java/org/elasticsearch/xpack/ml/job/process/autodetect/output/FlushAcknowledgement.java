/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.job.process.autodetect.output;

import org.elasticsearch.Version;
import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.utils.time.TimeUtils;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

/**
 * Simple class to parse and store a flush ID.
 */
public class FlushAcknowledgement extends ToXContentToBytes implements Writeable {
    /**
     * Field Names
     */
    public static final ParseField TYPE = new ParseField("flush");
    public static final ParseField ID = new ParseField("id");
    public static final ParseField LAST_FINALIZED_BUCKET_END = new ParseField("last_finalized_bucket_end");

    public static final ConstructingObjectParser<FlushAcknowledgement, Void> PARSER = new ConstructingObjectParser<>(
            TYPE.getPreferredName(), a -> new FlushAcknowledgement((String) a[0], (Date) a[1]));

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), ID);
        PARSER.declareField(ConstructingObjectParser.optionalConstructorArg(), p -> {
            if (p.currentToken() == XContentParser.Token.VALUE_NUMBER) {
                return new Date(p.longValue());
            } else if (p.currentToken() == XContentParser.Token.VALUE_STRING) {
                return new Date(TimeUtils.dateStringToEpoch(p.text()));
            }
            throw new IllegalArgumentException(
                    "unexpected token [" + p.currentToken() + "] for [" + LAST_FINALIZED_BUCKET_END.getPreferredName() + "]");
        }, LAST_FINALIZED_BUCKET_END, ObjectParser.ValueType.VALUE);
    }

    private String id;
    private Date lastFinalizedBucketEnd;

    public FlushAcknowledgement(String id, Date lastFinalizedBucketEnd) {
        this.id = id;
        this.lastFinalizedBucketEnd = lastFinalizedBucketEnd;
    }

    public FlushAcknowledgement(StreamInput in) throws IOException {
        id = in.readString();
        if (in.getVersion().after(Version.V_5_5_0_UNRELEASED)) {
            lastFinalizedBucketEnd = new Date(in.readVLong());
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(id);
        if (out.getVersion().after(Version.V_5_5_0_UNRELEASED)) {
            out.writeVLong(lastFinalizedBucketEnd.getTime());
        }
    }

    public String getId() {
        return id;
    }

    public Date getLastFinalizedBucketEnd() {
        return lastFinalizedBucketEnd;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(ID.getPreferredName(), id);
        if (lastFinalizedBucketEnd != null) {
            builder.dateField(LAST_FINALIZED_BUCKET_END.getPreferredName(), LAST_FINALIZED_BUCKET_END.getPreferredName() + "_string",
                    lastFinalizedBucketEnd.getTime());
        }
        builder.endObject();
        return builder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, lastFinalizedBucketEnd);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FlushAcknowledgement other = (FlushAcknowledgement) obj;
        return Objects.equals(id, other.id) &&
                Objects.equals(lastFinalizedBucketEnd, other.lastFinalizedBucketEnd);
    }
}

