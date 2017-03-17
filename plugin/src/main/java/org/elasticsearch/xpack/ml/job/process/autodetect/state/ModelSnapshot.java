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
package org.elasticsearch.xpack.ml.job.process.autodetect.state;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentParser.Token;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.utils.time.TimeUtils;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;


/**
 * ModelSnapshot Result POJO
 */
public class ModelSnapshot extends ToXContentToBytes implements Writeable {
    /**
     * Field Names
     */
    public static final ParseField TIMESTAMP = new ParseField("timestamp");
    public static final ParseField DESCRIPTION = new ParseField("description");
    public static final ParseField SNAPSHOT_ID = new ParseField("snapshot_id");
    public static final ParseField SNAPSHOT_DOC_COUNT = new ParseField("snapshot_doc_count");
    public static final ParseField LATEST_RECORD_TIME = new ParseField("latest_record_time_stamp");
    public static final ParseField LATEST_RESULT_TIME = new ParseField("latest_result_time_stamp");

    // Used for QueryPage
    public static final ParseField RESULTS_FIELD = new ParseField("model_snapshots");

    /**
     * Elasticsearch type
     */
    public static final ParseField TYPE = new ParseField("model_snapshot");

    public static final ConstructingObjectParser<ModelSnapshot, Void> PARSER =
            new ConstructingObjectParser<>(TYPE.getPreferredName(), a -> new ModelSnapshot((String) a[0]));

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), Job.ID);
        PARSER.declareField(ModelSnapshot::setTimestamp, p -> {
            if (p.currentToken() == Token.VALUE_NUMBER) {
                return new Date(p.longValue());
            } else if (p.currentToken() == Token.VALUE_STRING) {
                return new Date(TimeUtils.dateStringToEpoch(p.text()));
            }
            throw new IllegalArgumentException("unexpected token [" + p.currentToken() + "] for [" + TIMESTAMP.getPreferredName() + "]");
        }, TIMESTAMP, ValueType.VALUE);
        PARSER.declareString(ModelSnapshot::setDescription, DESCRIPTION);
        PARSER.declareString(ModelSnapshot::setSnapshotId, SNAPSHOT_ID);
        PARSER.declareInt(ModelSnapshot::setSnapshotDocCount, SNAPSHOT_DOC_COUNT);
        PARSER.declareObject(ModelSnapshot::setModelSizeStats, ModelSizeStats.PARSER, ModelSizeStats.RESULT_TYPE_FIELD);
        PARSER.declareField(ModelSnapshot::setLatestRecordTimeStamp, p -> {
            if (p.currentToken() == Token.VALUE_NUMBER) {
                return new Date(p.longValue());
            } else if (p.currentToken() == Token.VALUE_STRING) {
                return new Date(TimeUtils.dateStringToEpoch(p.text()));
            }
            throw new IllegalArgumentException(
                    "unexpected token [" + p.currentToken() + "] for [" + LATEST_RECORD_TIME.getPreferredName() + "]");
        }, LATEST_RECORD_TIME, ValueType.VALUE);
        PARSER.declareField(ModelSnapshot::setLatestResultTimeStamp, p -> {
            if (p.currentToken() == Token.VALUE_NUMBER) {
                return new Date(p.longValue());
            } else if (p.currentToken() == Token.VALUE_STRING) {
                return new Date(TimeUtils.dateStringToEpoch(p.text()));
            }
            throw new IllegalArgumentException(
                    "unexpected token [" + p.currentToken() + "] for [" + LATEST_RESULT_TIME.getPreferredName() + "]");
        }, LATEST_RESULT_TIME, ValueType.VALUE);
        PARSER.declareObject(ModelSnapshot::setQuantiles, Quantiles.PARSER, Quantiles.TYPE);
    }

    private final String jobId;
    private Date timestamp;
    private String description;
    private String snapshotId;
    private int snapshotDocCount;
    private ModelSizeStats modelSizeStats;
    private Date latestRecordTimeStamp;
    private Date latestResultTimeStamp;
    private Quantiles quantiles;

    public ModelSnapshot(String jobId) {
        this.jobId = jobId;
    }

    public ModelSnapshot(StreamInput in) throws IOException {
        jobId = in.readString();
        if (in.readBoolean()) {
            timestamp = new Date(in.readLong());
        }
        description = in.readOptionalString();
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
        out.writeString(jobId);
        boolean hasTimestamp = timestamp != null;
        out.writeBoolean(hasTimestamp);
        if (hasTimestamp) {
            out.writeLong(timestamp.getTime());
        }
        out.writeOptionalString(description);
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
        builder.field(Job.ID.getPreferredName(), jobId);
        if (timestamp != null) {
            builder.dateField(TIMESTAMP.getPreferredName(), TIMESTAMP.getPreferredName() + "_string", timestamp.getTime());
        }
        if (description != null) {
            builder.field(DESCRIPTION.getPreferredName(), description);
        }
        if (snapshotId != null) {
            builder.field(SNAPSHOT_ID.getPreferredName(), snapshotId);
        }
        builder.field(SNAPSHOT_DOC_COUNT.getPreferredName(), snapshotDocCount);
        if (modelSizeStats != null) {
            builder.field(ModelSizeStats.RESULT_TYPE_FIELD.getPreferredName(), modelSizeStats);
        }
        if (latestRecordTimeStamp != null) {
            builder.dateField(LATEST_RECORD_TIME.getPreferredName(), LATEST_RECORD_TIME.getPreferredName() + "_string",
                    latestRecordTimeStamp.getTime());
        }
        if (latestResultTimeStamp != null) {
            builder.dateField(LATEST_RESULT_TIME.getPreferredName(), LATEST_RESULT_TIME.getPreferredName() + "_string",
                    latestResultTimeStamp.getTime());
        }
        if (quantiles != null) {
            builder.field(Quantiles.TYPE.getPreferredName(), quantiles);
        }
        builder.endObject();
        return builder;
    }

    public String getJobId() {
        return jobId;
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

    public void setModelSizeStats(ModelSizeStats.Builder modelSizeStats) {
        this.modelSizeStats = modelSizeStats.build();
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
        return Objects.hash(jobId, timestamp, description, snapshotId, quantiles, snapshotDocCount, modelSizeStats, latestRecordTimeStamp,
                latestResultTimeStamp);
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

        return Objects.equals(this.jobId, that.jobId)
                && Objects.equals(this.timestamp, that.timestamp)
                && Objects.equals(this.description, that.description)
                && Objects.equals(this.snapshotId, that.snapshotId)
                && this.snapshotDocCount == that.snapshotDocCount
                && Objects.equals(this.modelSizeStats, that.modelSizeStats)
                && Objects.equals(this.quantiles, that.quantiles)
                && Objects.equals(this.latestRecordTimeStamp, that.latestRecordTimeStamp)
                && Objects.equals(this.latestResultTimeStamp, that.latestResultTimeStamp);
    }

    public static String documentId(ModelSnapshot snapshot) {
        return documentId(snapshot.getJobId(), snapshot.getSnapshotId());
    }

    public static String documentId(String jobId, String snapshotId) {
        return jobId + "-" + snapshotId;
    }

    public static ModelSnapshot fromJson(BytesReference bytesReference) {
        try (XContentParser parser = XContentFactory.xContent(bytesReference).createParser(NamedXContentRegistry.EMPTY, bytesReference)) {
            return PARSER.apply(parser, null);
        } catch (IOException e) {
            throw new ElasticsearchParseException("failed to parse modelSnapshot", e);
        }
    }
}
