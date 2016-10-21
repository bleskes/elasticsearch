
package org.elasticsearch.xpack.prelert.job;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

/**
 * Provide access to the C++ model memory usage numbers for the Java process.
 */
@JsonIgnoreProperties({ "modelSizeStatsId" })
public class ModelSizeStats extends ToXContentToBytes implements StorageSerialisable, Writeable {
    /**
     * Field Names
     */
    public static final ParseField MODEL_SIZE_STATS_FIELD = new ParseField("model_size_stats");
    public static final ParseField MODEL_BYTES_FIELD = new ParseField("model_bytes");
    public static final ParseField TOTAL_BY_FIELD_COUNT_FIELD = new ParseField("total_by_field_count");
    public static final ParseField TOTAL_OVER_FIELD_COUNT_FIELD = new ParseField("total_over_field_count");
    public static final ParseField TOTAL_PARTITION_FIELD_COUNT_FIELD = new ParseField("total_partition_field_count");
    public static final ParseField BUCKET_ALLOCATION_FAILURES_COUNT_FIELD = new ParseField("bucket_allocation_failures_count");
    public static final ParseField MEMORY_STATUS_FIELD = new ParseField("memory_status");
    public static final ParseField LOG_TIME_FIELD = new ParseField("log_time");
    public static final ParseField BUCKET_TIME_FIELD = new ParseField("bucket_time");
    public static final ParseField TIMESTAMP_FIELD = new ParseField("timestamp");

    public static final ObjectParser<ModelSizeStats, ParseFieldMatcherSupplier> PARSER = new ObjectParser<>(
            MODEL_SIZE_STATS_FIELD.getPreferredName(), ModelSizeStats::new);

    static {
        PARSER.declareLong(ModelSizeStats::setModelBytes, MODEL_BYTES_FIELD);
        PARSER.declareLong(ModelSizeStats::setBucketAllocationFailuresCount, BUCKET_ALLOCATION_FAILURES_COUNT_FIELD);
        PARSER.declareLong(ModelSizeStats::setTotalByFieldCount, TOTAL_BY_FIELD_COUNT_FIELD);
        PARSER.declareLong(ModelSizeStats::setTotalOverFieldCount, TOTAL_OVER_FIELD_COUNT_FIELD);
        PARSER.declareLong(ModelSizeStats::setTotalPartitionFieldCount, TOTAL_PARTITION_FIELD_COUNT_FIELD);
        PARSER.declareField(ModelSizeStats::setLogTime, p -> new Date(p.longValue()), LOG_TIME_FIELD, ValueType.LONG);
        PARSER.declareField(ModelSizeStats::setTimestamp, p -> new Date(p.longValue()), TIMESTAMP_FIELD, ValueType.LONG);
        PARSER.declareField(ModelSizeStats::setMemoryStatus, p -> MemoryStatus.fromString(p.text()), MEMORY_STATUS_FIELD, ValueType.STRING);
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

    private long modelBytes = 0;
    private long totalByFieldCount = 0;
    private long totalOverFieldCount = 0;
    private long totalPartitionFieldCount = 0;
    private long bucketAllocationFailuresCount = 0;
    private MemoryStatus memoryStatus = MemoryStatus.OK;
    private Date timestamp;
    private Date logTime;
    private String id = TYPE.getPreferredName();

    public ModelSizeStats() {
        this.logTime = new Date();
    }

    public ModelSizeStats(StreamInput in) throws IOException {
        modelBytes = in.readVLong();
        totalByFieldCount = in.readVLong();
        totalOverFieldCount = in.readVLong();
        totalPartitionFieldCount = in.readVLong();
        bucketAllocationFailuresCount = in.readVLong();
        memoryStatus = MemoryStatus.readFromStream(in);
        logTime = new Date(in.readLong());
        if (in.readBoolean()) {
            timestamp = new Date(in.readLong());
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
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

    public String getModelSizeStatsId() {
        return this.id;
    }

    public void setModelSizeStatsId(String id) {
        this.id = id;
    }

    public void setModelBytes(long m) {
        this.modelBytes = m;
    }

    public long getModelBytes() {
        return this.modelBytes;
    }

    public void setTotalByFieldCount(long m) {
        this.totalByFieldCount = m;
    }

    public long getTotalByFieldCount() {
        return this.totalByFieldCount;
    }

    public void setTotalPartitionFieldCount(long m) {
        this.totalPartitionFieldCount = m;
    }

    public long getTotalPartitionFieldCount() {
        return this.totalPartitionFieldCount;
    }

    public void setTotalOverFieldCount(long m) {
        this.totalOverFieldCount = m;
    }

    public long getTotalOverFieldCount() {
        return this.totalOverFieldCount;
    }

    public void setBucketAllocationFailuresCount(long m) {
        this.bucketAllocationFailuresCount = m;
    }

    public long getBucketAllocationFailuresCount() {
        return this.bucketAllocationFailuresCount;
    }

    public void setMemoryStatus(MemoryStatus memoryStatus) {
        Objects.requireNonNull(memoryStatus, "[" + MEMORY_STATUS_FIELD.getPreferredName() + "] must not be null");
        this.memoryStatus = memoryStatus;
    }

    public MemoryStatus getMemoryStatus() {
        return this.memoryStatus;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Date d) {
        this.timestamp = d;
    }

    public Date getLogTime() {
        return this.logTime;
    }

    public void setLogTime(Date d) {
        this.logTime = d;
    }

    // NORELEASE remove this method when we remove Jackson
    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException {
        serialiser.addTimestamp(timestamp)
        .add("modelBytes", modelBytes)
        .add("totalByFieldCount", totalByFieldCount)
        .add("totalOverFieldCount", totalOverFieldCount)
        .add("totalPartitionFieldCount", totalPartitionFieldCount)
        .add("bucketAllocationFailuresCount", bucketAllocationFailuresCount)
        .add("memoryStatus", memoryStatus)
        .add("logTime", logTime);
    }

    @Override
    public int hashCode() {
        // this.id excluded here as it is generated by the datastore
        return Objects.hash(this.modelBytes, totalByFieldCount, totalOverFieldCount, totalPartitionFieldCount,
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

        // this.id excluded here as it is generated by the datastore
        return this.modelBytes == that.modelBytes && this.totalByFieldCount == that.totalByFieldCount
                && this.totalOverFieldCount == that.totalOverFieldCount && this.totalPartitionFieldCount == that.totalPartitionFieldCount
                && this.bucketAllocationFailuresCount == that.bucketAllocationFailuresCount
                && Objects.equals(this.memoryStatus, that.memoryStatus) && Objects.equals(this.timestamp, that.timestamp)
                && Objects.equals(this.logTime, that.logTime);
    }
}
