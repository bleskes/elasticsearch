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
package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser.Token;
import org.elasticsearch.xpack.prelert.utils.time.TimeUtils;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

/**
 * Provide access to the C++ model memory usage numbers for the Java process.
 */
public class ModelSizeStats extends ToXContentToBytes implements Writeable {

    /**
     * Field Names
     */
    private static final ParseField MODEL_SIZE_STATS_FIELD = new ParseField("modelSizeStats");
    public static final ParseField JOB_ID = new ParseField("jobId");
    public static final ParseField MODEL_BYTES_FIELD = new ParseField("modelBytes");
    public static final ParseField TOTAL_BY_FIELD_COUNT_FIELD = new ParseField("totalByFieldCount");
    public static final ParseField TOTAL_OVER_FIELD_COUNT_FIELD = new ParseField("totalOverFieldCount");
    public static final ParseField TOTAL_PARTITION_FIELD_COUNT_FIELD = new ParseField("totalPartitionFieldCount");
    public static final ParseField BUCKET_ALLOCATION_FAILURES_COUNT_FIELD = new ParseField("bucketAllocationFailuresCount");
    public static final ParseField MEMORY_STATUS_FIELD = new ParseField("memoryStatus");
    public static final ParseField LOG_TIME_FIELD = new ParseField("logTime");
    public static final ParseField TIMESTAMP_FIELD = new ParseField("timestamp");

    public static final ConstructingObjectParser<Builder, ParseFieldMatcherSupplier> PARSER = new ConstructingObjectParser<>(
            MODEL_SIZE_STATS_FIELD.getPreferredName(), a -> new Builder((String) a[0]));

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), JOB_ID);
        PARSER.declareLong(Builder::setModelBytes, MODEL_BYTES_FIELD);
        PARSER.declareLong(Builder::setBucketAllocationFailuresCount, BUCKET_ALLOCATION_FAILURES_COUNT_FIELD);
        PARSER.declareLong(Builder::setTotalByFieldCount, TOTAL_BY_FIELD_COUNT_FIELD);
        PARSER.declareLong(Builder::setTotalOverFieldCount, TOTAL_OVER_FIELD_COUNT_FIELD);
        PARSER.declareLong(Builder::setTotalPartitionFieldCount, TOTAL_PARTITION_FIELD_COUNT_FIELD);
        PARSER.declareField(Builder::setLogTime, p -> {
            if (p.currentToken() == Token.VALUE_NUMBER) {
                return new Date(p.longValue());
            } else if (p.currentToken() == Token.VALUE_STRING) {
                return new Date(TimeUtils.dateStringToEpoch(p.text()));
            }
            throw new IllegalArgumentException(
                    "unexpected token [" + p.currentToken() + "] for [" + LOG_TIME_FIELD.getPreferredName() + "]");
        }, LOG_TIME_FIELD, ValueType.VALUE);
        PARSER.declareField(Builder::setTimestamp, p -> {
            if (p.currentToken() == Token.VALUE_NUMBER) {
                return new Date(p.longValue());
            } else if (p.currentToken() == Token.VALUE_STRING) {
                return new Date(TimeUtils.dateStringToEpoch(p.text()));
            }
            throw new IllegalArgumentException(
                    "unexpected token [" + p.currentToken() + "] for [" + TIMESTAMP_FIELD.getPreferredName() + "]");
        }, TIMESTAMP_FIELD, ValueType.VALUE);
        PARSER.declareField(Builder::setMemoryStatus, p -> MemoryStatus.fromString(p.text()), MEMORY_STATUS_FIELD, ValueType.STRING);
    }

    /**
     * Elasticsearch type
     */
    public static final ParseField TYPE = new ParseField("modelSizeStats");

    /**
     * The status of the memory monitored by the ResourceMonitor. OK is default,
     * SOFT_LIMIT means that the models have done some aggressive pruning to
     * keep the memory below the limit, and HARD_LIMIT means that samples have
     * been dropped
     */
    public enum MemoryStatus implements Writeable {
        OK("ok"), SOFT_LIMIT("soft_limit"), HARD_LIMIT("hard_limit");

        private String name;

        private MemoryStatus(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static MemoryStatus fromString(String statusName) {
            for (MemoryStatus status : values()) {
                if (status.name.equals(statusName)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown MemoryStatus [" + statusName + "]");
        }

        public static MemoryStatus readFromStream(StreamInput in) throws IOException {
            int ordinal = in.readVInt();
            if (ordinal < 0 || ordinal >= values().length) {
                throw new IOException("Unknown MemoryStatus ordinal [" + ordinal + "]");
            }
            return values()[ordinal];
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeVInt(ordinal());
        }
    }

    private final String jobId;
    private final String id;
    private final long modelBytes;
    private final long totalByFieldCount;
    private final long totalOverFieldCount;
    private final long totalPartitionFieldCount;
    private final long bucketAllocationFailuresCount;
    private final MemoryStatus memoryStatus;
    private final Date timestamp;
    private final Date logTime;

    private ModelSizeStats(String jobId, String id, long modelBytes, long totalByFieldCount, long totalOverFieldCount,
                           long totalPartitionFieldCount, long bucketAllocationFailuresCount, MemoryStatus memoryStatus,
                           Date timestamp, Date logTime) {
        this.jobId = jobId;
        this.id = id;
        this.modelBytes = modelBytes;
        this.totalByFieldCount = totalByFieldCount;
        this.totalOverFieldCount = totalOverFieldCount;
        this.totalPartitionFieldCount = totalPartitionFieldCount;
        this.bucketAllocationFailuresCount = bucketAllocationFailuresCount;
        this.memoryStatus = memoryStatus;
        this.timestamp = timestamp;
        this.logTime = logTime;
    }

    public ModelSizeStats(StreamInput in) throws IOException {
        jobId = in.readString();
        id = null;
        modelBytes = in.readVLong();
        totalByFieldCount = in.readVLong();
        totalOverFieldCount = in.readVLong();
        totalPartitionFieldCount = in.readVLong();
        bucketAllocationFailuresCount = in.readVLong();
        memoryStatus = MemoryStatus.readFromStream(in);
        logTime = new Date(in.readLong());
        timestamp = in.readBoolean() ? new Date(in.readLong()) : null;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(jobId);
        out.writeVLong(modelBytes);
        out.writeVLong(totalByFieldCount);
        out.writeVLong(totalOverFieldCount);
        out.writeVLong(totalPartitionFieldCount);
        out.writeVLong(bucketAllocationFailuresCount);
        memoryStatus.writeTo(out);
        out.writeLong(logTime.getTime());
        boolean hasTimestamp = timestamp != null;
        out.writeBoolean(hasTimestamp);
        if (hasTimestamp) {
            out.writeLong(timestamp.getTime());
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(JOB_ID.getPreferredName(), jobId);
        builder.field(MODEL_BYTES_FIELD.getPreferredName(), modelBytes);
        builder.field(TOTAL_BY_FIELD_COUNT_FIELD.getPreferredName(), totalByFieldCount);
        builder.field(TOTAL_OVER_FIELD_COUNT_FIELD.getPreferredName(), totalOverFieldCount);
        builder.field(TOTAL_PARTITION_FIELD_COUNT_FIELD.getPreferredName(), totalPartitionFieldCount);
        builder.field(BUCKET_ALLOCATION_FAILURES_COUNT_FIELD.getPreferredName(), bucketAllocationFailuresCount);
        builder.field(MEMORY_STATUS_FIELD.getPreferredName(), memoryStatus.getName());
        builder.field(LOG_TIME_FIELD.getPreferredName(), logTime.getTime());
        if (timestamp != null) {
            builder.field(TIMESTAMP_FIELD.getPreferredName(), timestamp.getTime());
        }
        builder.endObject();
        return builder;
    }

    public String getId() {
        return id;
    }

    public long getModelBytes() {
        return modelBytes;
    }

    public long getTotalByFieldCount() {
        return totalByFieldCount;
    }

    public long getTotalPartitionFieldCount() {
        return totalPartitionFieldCount;
    }

    public long getTotalOverFieldCount() {
        return totalOverFieldCount;
    }

    public long getBucketAllocationFailuresCount() {
        return bucketAllocationFailuresCount;
    }

    public MemoryStatus getMemoryStatus() {
        return memoryStatus;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public Date getLogTime() {
        return logTime;
    }

    @Override
    public int hashCode() {
        // this.id excluded here as it is generated by the datastore
        return Objects.hash(jobId, modelBytes, totalByFieldCount, totalOverFieldCount, totalPartitionFieldCount,
                this.bucketAllocationFailuresCount, memoryStatus, timestamp, logTime);
    }

    /**
     * Compare all the fields.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof ModelSizeStats == false) {
            return false;
        }

        ModelSizeStats that = (ModelSizeStats) other;

        return this.modelBytes == that.modelBytes && this.totalByFieldCount == that.totalByFieldCount
                && this.totalOverFieldCount == that.totalOverFieldCount && this.totalPartitionFieldCount == that.totalPartitionFieldCount
                && this.bucketAllocationFailuresCount == that.bucketAllocationFailuresCount
                && Objects.equals(this.memoryStatus, that.memoryStatus) && Objects.equals(this.timestamp, that.timestamp)
                && Objects.equals(this.logTime, that.logTime)
                && Objects.equals(this.jobId, that.jobId);
    }

    // NORELEASE This will not be needed once we are able to parse ModelSizeStats all at once.
    public static class Builder {

        private final String jobId;
        private String id;
        private long modelBytes;
        private long totalByFieldCount;
        private long totalOverFieldCount;
        private long totalPartitionFieldCount;
        private long bucketAllocationFailuresCount;
        private MemoryStatus memoryStatus;
        private Date timestamp;
        private Date logTime;

        public Builder(String jobId) {
            this.jobId = jobId;
            id = TYPE.getPreferredName();
            memoryStatus = MemoryStatus.OK;
            logTime = new Date();
        }

        public void setId(String id) {
            this.id = Objects.requireNonNull(id);
        }

        public void setModelBytes(long modelBytes) {
            this.modelBytes = modelBytes;
        }

        public void setTotalByFieldCount(long totalByFieldCount) {
            this.totalByFieldCount = totalByFieldCount;
        }

        public void setTotalPartitionFieldCount(long totalPartitionFieldCount) {
            this.totalPartitionFieldCount = totalPartitionFieldCount;
        }

        public void setTotalOverFieldCount(long totalOverFieldCount) {
            this.totalOverFieldCount = totalOverFieldCount;
        }

        public void setBucketAllocationFailuresCount(long bucketAllocationFailuresCount) {
            this.bucketAllocationFailuresCount = bucketAllocationFailuresCount;
        }

        public void setMemoryStatus(MemoryStatus memoryStatus) {
            Objects.requireNonNull(memoryStatus, "[" + MEMORY_STATUS_FIELD.getPreferredName() + "] must not be null");
            this.memoryStatus = memoryStatus;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public void setLogTime(Date logTime) {
            this.logTime = logTime;
        }

        public ModelSizeStats build() {
            return new ModelSizeStats(jobId, id, modelBytes, totalByFieldCount, totalOverFieldCount, totalPartitionFieldCount,
                    bucketAllocationFailuresCount, memoryStatus, timestamp, logTime);
        }
    }
}
