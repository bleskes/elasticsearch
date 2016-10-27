
package org.elasticsearch.xpack.prelert.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

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
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;


/**
 * ModelSnapshot Result POJO
 */
@JsonInclude(Include.NON_NULL)
public class ModelSnapshot extends ToXContentToBytes implements Writeable, StorageSerialisable {
    /**
     * Field Names
     */
    public static final ParseField TIMESTAMP = new ParseField("timestamp");
    public static final ParseField DESCRIPTION = new ParseField("description");
    public static final ParseField RESTORE_PRIORITY = new ParseField("restorePriority");
    public static final ParseField SNAPSHOT_ID = new ParseField("snapshotId");
    public static final ParseField SNAPSHOT_DOC_COUNT = new ParseField("snapshotDocCount");
    public static final ParseField LATEST_RECORD_TIME = new ParseField("latestRecordTimeStamp");
    public static final ParseField LATEST_RESULT_TIME = new ParseField("latestResultTimeStamp");

    /**
     * Elasticsearch type
     */
    public static final ParseField TYPE = new ParseField("modelSnapshot");

    public static final ObjectParser<ModelSnapshot, ParseFieldMatcherSupplier> PARSER = new ObjectParser<>(TYPE.getPreferredName(),
            ModelSnapshot::new);
    static {
        PARSER.declareField(ModelSnapshot::setTimestamp, p -> new Date(p.longValue()), TIMESTAMP, ValueType.LONG);
        PARSER.declareString(ModelSnapshot::setDescription, DESCRIPTION);
        PARSER.declareLong(ModelSnapshot::setRestorePriority, RESTORE_PRIORITY);
        PARSER.declareString(ModelSnapshot::setSnapshotId, SNAPSHOT_ID);
        PARSER.declareInt(ModelSnapshot::setSnapshotDocCount, SNAPSHOT_DOC_COUNT);
        PARSER.declareObject(ModelSnapshot::setModelSizeStats, ModelSizeStats.PARSER, ModelSizeStats.TYPE);
        PARSER.declareField(ModelSnapshot::setLatestRecordTimeStamp, p -> new Date(p.longValue()), LATEST_RECORD_TIME, ValueType.LONG);
        PARSER.declareField(ModelSnapshot::setLatestResultTimeStamp, p -> new Date(p.longValue()), LATEST_RESULT_TIME, ValueType.LONG);
        PARSER.declareObject(ModelSnapshot::setQuantiles, Quantiles.PARSER, Quantiles.TYPE);
    }

    private Date timestamp;
    private String description;
    private long restorePriority;
    private String snapshotId;
    private int snapshotDocCount;
    private ModelSizeStats modelSizeStats;
    private Date latestRecordTimeStamp;
    private Date latestResultTimeStamp;
    private Quantiles quantiles;

    public ModelSnapshot() {
    }

    public ModelSnapshot(StreamInput in) throws IOException {
        if (in.readBoolean()) {
            timestamp = new Date(in.readLong());
        }
        description = in.readOptionalString();
        restorePriority = in.readLong();
        snapshotId = in.readOptionalString();
        snapshotDocCount = in.readInt();
        if (in.readBoolean()) {
            modelSizeStats = new ModelSizeStats(in);
        }
        if (in.readBoolean()) {
            latestRecordTimeStamp = new Date(in.readLong());
        }
        if (in.readBoolean()) {
            latestResultTimeStamp = new Date(in.readLong());
        }
        if (in.readBoolean()) {
            quantiles = new Quantiles(in);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        boolean hasTimestamp = timestamp != null;
        out.writeBoolean(hasTimestamp);
        if (hasTimestamp) {
            out.writeLong(timestamp.getTime());
        }
        out.writeOptionalString(description);
        out.writeLong(restorePriority);
        out.writeOptionalString(snapshotId);
        out.writeInt(snapshotDocCount);
        boolean hasModelSizeStats = modelSizeStats != null;
        out.writeBoolean(hasModelSizeStats);
        if (hasModelSizeStats) {
            modelSizeStats.writeTo(out);
        }
        boolean hasLatestRecordTimeStamp = latestRecordTimeStamp != null;
        out.writeBoolean(hasLatestRecordTimeStamp);
        if (hasLatestRecordTimeStamp) {
            out.writeLong(latestRecordTimeStamp.getTime());
        }
        boolean hasLatestResultTimeStamp = latestResultTimeStamp != null;
        out.writeBoolean(hasLatestResultTimeStamp);
        if (hasLatestResultTimeStamp) {
            out.writeLong(latestResultTimeStamp.getTime());
        }
        boolean hasQuantiles = quantiles != null;
        out.writeBoolean(hasQuantiles);
        if (hasQuantiles) {
            quantiles.writeTo(out);
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (timestamp != null) {
            builder.field(TIMESTAMP.getPreferredName(), timestamp.getTime());
        }
        if (description != null) {
            builder.field(DESCRIPTION.getPreferredName(), description);
        }
        builder.field(RESTORE_PRIORITY.getPreferredName(), restorePriority);
        if (snapshotId != null) {
            builder.field(SNAPSHOT_ID.getPreferredName(), snapshotId);
        }
        builder.field(SNAPSHOT_DOC_COUNT.getPreferredName(), snapshotDocCount);
        if (modelSizeStats != null) {
            builder.field(ModelSizeStats.TYPE.getPreferredName(), modelSizeStats);
        }
        if (latestRecordTimeStamp != null) {
            builder.field(LATEST_RECORD_TIME.getPreferredName(), latestRecordTimeStamp.getTime());
        }
        if (latestResultTimeStamp != null) {
            builder.field(LATEST_RESULT_TIME.getPreferredName(), latestResultTimeStamp.getTime());
        }
        if (quantiles != null) {
            builder.field(Quantiles.TYPE.getPreferredName(), quantiles);
        }
        builder.endObject();
        return builder;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getRestorePriority() {
        return restorePriority;
    }

    public void setRestorePriority(long restorePriority) {
        this.restorePriority = restorePriority;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public int getSnapshotDocCount() {
        return snapshotDocCount;
    }

    public void setSnapshotDocCount(int snapshotDocCount) {
        this.snapshotDocCount = snapshotDocCount;
    }

    public ModelSizeStats getModelSizeStats() {
        return modelSizeStats;
    }

    public void setModelSizeStats(ModelSizeStats modelSizeStats) {
        this.modelSizeStats = modelSizeStats;
    }

    public Quantiles getQuantiles() {
        return quantiles;
    }

    public void setQuantiles(Quantiles q) {
        quantiles = q;
    }

    public Date getLatestRecordTimeStamp() {
        return latestRecordTimeStamp;
    }

    public void setLatestRecordTimeStamp(Date latestRecordTimeStamp) {
        this.latestRecordTimeStamp = latestRecordTimeStamp;
    }

    public Date getLatestResultTimeStamp() {
        return latestResultTimeStamp;
    }

    public void setLatestResultTimeStamp(Date latestResultTimeStamp) {
        this.latestResultTimeStamp = latestResultTimeStamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, description, restorePriority, snapshotId, quantiles,
                snapshotDocCount, modelSizeStats, latestRecordTimeStamp, latestResultTimeStamp);
    }

    /**
     * Compare all the fields.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof ModelSnapshot == false) {
            return false;
        }

        ModelSnapshot that = (ModelSnapshot) other;

        return Objects.equals(this.timestamp, that.timestamp)
                && Objects.equals(this.description, that.description)
                && this.restorePriority == that.restorePriority
                && Objects.equals(this.snapshotId, that.snapshotId)
                && this.snapshotDocCount == that.snapshotDocCount
                && Objects.equals(this.modelSizeStats, that.modelSizeStats)
                && Objects.equals(this.quantiles, that.quantiles)
                && Objects.equals(this.latestRecordTimeStamp, that.latestRecordTimeStamp)
                && Objects.equals(this.latestResultTimeStamp, that.latestResultTimeStamp);
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException {
        serialiser.addTimestamp(timestamp)
        .add(DESCRIPTION.getPreferredName(), description).add(RESTORE_PRIORITY.getPreferredName(), restorePriority)
        .add(SNAPSHOT_ID.getPreferredName(), snapshotId).add(SNAPSHOT_DOC_COUNT.getPreferredName(), snapshotDocCount);

        if (modelSizeStats != null) {
            serialiser.startObject(ModelSizeStats.TYPE.getPreferredName());
            modelSizeStats.serialise(serialiser);
            serialiser.endObject();
        }
        if (quantiles != null) {
            serialiser.startObject(Quantiles.TYPE.getPreferredName());
            quantiles.serialise(serialiser);
            serialiser.endObject();
        }
        if (latestRecordTimeStamp != null) {
            serialiser.add(LATEST_RECORD_TIME.getPreferredName(), latestRecordTimeStamp);
        }
        if (latestResultTimeStamp != null) {
            serialiser.add(LATEST_RESULT_TIME.getPreferredName(), latestResultTimeStamp);
        }
    }
}
