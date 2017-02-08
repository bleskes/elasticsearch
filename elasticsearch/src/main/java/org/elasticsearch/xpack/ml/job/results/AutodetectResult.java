/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (C) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained therein, is the
 * exclusive property of Elasticsearch BV and its licensors, if any, and
 * is protected under applicable domestic and foreign law, and international
 * treaties. Reproduction, republication or distribution without the express
 * written consent of Elasticsearch BV is strictly prohibited.
 */
package org.elasticsearch.xpack.ml.job.results;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSizeStats;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSnapshot;
import org.elasticsearch.xpack.ml.job.process.autodetect.output.FlushAcknowledgement;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.Quantiles;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class AutodetectResult extends ToXContentToBytes implements Writeable {

    public static final ParseField TYPE = new ParseField("autodetect_result");

    @SuppressWarnings("unchecked")
    public static final ConstructingObjectParser<AutodetectResult, Void> PARSER = new ConstructingObjectParser<>(
            TYPE.getPreferredName(), a -> new AutodetectResult((Bucket) a[0], (List<AnomalyRecord>) a[1], (List<Influencer>) a[2],
                    (Quantiles) a[3], (ModelSnapshot) a[4], a[5] == null ? null : ((ModelSizeStats.Builder) a[5]).build(),
                    (ModelDebugOutput) a[6], (CategoryDefinition) a[7], (FlushAcknowledgement) a[8]));

    static {
        PARSER.declareObject(ConstructingObjectParser.optionalConstructorArg(), Bucket.PARSER, Bucket.RESULT_TYPE_FIELD);
        PARSER.declareObjectArray(ConstructingObjectParser.optionalConstructorArg(), AnomalyRecord.PARSER, AnomalyRecord.RESULTS_FIELD);
        PARSER.declareObjectArray(ConstructingObjectParser.optionalConstructorArg(), Influencer.PARSER, Influencer.RESULTS_FIELD);
        PARSER.declareObject(ConstructingObjectParser.optionalConstructorArg(), Quantiles.PARSER, Quantiles.TYPE);
        PARSER.declareObject(ConstructingObjectParser.optionalConstructorArg(), ModelSnapshot.PARSER, ModelSnapshot.TYPE);
        PARSER.declareObject(ConstructingObjectParser.optionalConstructorArg(), ModelSizeStats.PARSER,
                ModelSizeStats.RESULT_TYPE_FIELD);
        PARSER.declareObject(ConstructingObjectParser.optionalConstructorArg(), ModelDebugOutput.PARSER, ModelDebugOutput.RESULTS_FIELD);
        PARSER.declareObject(ConstructingObjectParser.optionalConstructorArg(), CategoryDefinition.PARSER, CategoryDefinition.TYPE);
        PARSER.declareObject(ConstructingObjectParser.optionalConstructorArg(), FlushAcknowledgement.PARSER, FlushAcknowledgement.TYPE);
    }

    private final Bucket bucket;
    private final List<AnomalyRecord> records;
    private final List<Influencer> influencers;
    private final Quantiles quantiles;
    private final ModelSnapshot modelSnapshot;
    private final ModelSizeStats modelSizeStats;
    private final ModelDebugOutput modelDebugOutput;
    private final CategoryDefinition categoryDefinition;
    private final FlushAcknowledgement flushAcknowledgement;

    public AutodetectResult(Bucket bucket, List<AnomalyRecord> records, List<Influencer> influencers, Quantiles quantiles,
                            ModelSnapshot modelSnapshot, ModelSizeStats modelSizeStats, ModelDebugOutput modelDebugOutput,
                            CategoryDefinition categoryDefinition, FlushAcknowledgement flushAcknowledgement) {
        this.bucket = bucket;
        this.records = records;
        this.influencers = influencers;
        this.quantiles = quantiles;
        this.modelSnapshot = modelSnapshot;
        this.modelSizeStats = modelSizeStats;
        this.modelDebugOutput = modelDebugOutput;
        this.categoryDefinition = categoryDefinition;
        this.flushAcknowledgement = flushAcknowledgement;
    }

    public AutodetectResult(StreamInput in) throws IOException {
        if (in.readBoolean()) {
            this.bucket = new Bucket(in);
        } else {
            this.bucket = null;
        }
        if (in.readBoolean()) {
            this.records = in.readList(AnomalyRecord::new);
        } else {
            this.records = null;
        }
        if (in.readBoolean()) {
            this.influencers = in.readList(Influencer::new);
        } else {
            this.influencers = null;
        }
        if (in.readBoolean()) {
            this.quantiles = new Quantiles(in);
        } else {
            this.quantiles = null;
        }
        if (in.readBoolean()) {
            this.modelSnapshot = new ModelSnapshot(in);
        } else {
            this.modelSnapshot = null;
        }
        if (in.readBoolean()) {
            this.modelSizeStats = new ModelSizeStats(in);
        } else {
            this.modelSizeStats = null;
        }
        if (in.readBoolean()) {
            this.modelDebugOutput = new ModelDebugOutput(in);
        } else {
            this.modelDebugOutput = null;
        }
        if (in.readBoolean()) {
            this.categoryDefinition = new CategoryDefinition(in);
        } else {
            this.categoryDefinition = null;
        }
        if (in.readBoolean()) {
            this.flushAcknowledgement = new FlushAcknowledgement(in);
        } else {
            this.flushAcknowledgement = null;
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        writeNullable(bucket, out);
        writeNullable(records, out);
        writeNullable(influencers, out);
        writeNullable(quantiles, out);
        writeNullable(modelSnapshot, out);
        writeNullable(modelSizeStats, out);
        writeNullable(modelDebugOutput, out);
        writeNullable(categoryDefinition, out);
        writeNullable(flushAcknowledgement, out);
    }

    private static void writeNullable(Writeable writeable, StreamOutput out) throws IOException {
        boolean isPresent = writeable != null;
        out.writeBoolean(isPresent);
        if (isPresent) {
            writeable.writeTo(out);
        }
    }

    private static void writeNullable(List<? extends Writeable> writeables, StreamOutput out) throws IOException {
        boolean isPresent = writeables != null;
        out.writeBoolean(isPresent);
        if (isPresent) {
            out.writeList(writeables);
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        addNullableField(Bucket.RESULT_TYPE_FIELD, bucket, builder);
        addNullableField(AnomalyRecord.RESULTS_FIELD, records, builder);
        addNullableField(Influencer.RESULTS_FIELD, influencers, builder);
        addNullableField(Quantiles.TYPE, quantiles, builder);
        addNullableField(ModelSnapshot.TYPE, modelSnapshot, builder);
        addNullableField(ModelSizeStats.RESULT_TYPE_FIELD, modelSizeStats, builder);
        addNullableField(ModelDebugOutput.RESULTS_FIELD, modelDebugOutput, builder);
        addNullableField(CategoryDefinition.TYPE, categoryDefinition, builder);
        addNullableField(FlushAcknowledgement.TYPE, flushAcknowledgement, builder);
        builder.endObject();
        return builder;
    }

    private static void addNullableField(ParseField field, ToXContent value, XContentBuilder builder) throws IOException {
        if (value != null) {
            builder.field(field.getPreferredName(), value);
        }
    }

    private static void addNullableField(ParseField field, List<? extends ToXContent> values, XContentBuilder builder) throws IOException {
        if (values != null) {
            builder.field(field.getPreferredName(), values);
        }
    }

    public Bucket getBucket() {
        return bucket;
    }

    public List<AnomalyRecord> getRecords() {
        return records;
    }

    public List<Influencer> getInfluencers() {
        return influencers;
    }

    public Quantiles getQuantiles() {
        return quantiles;
    }

    public ModelSnapshot getModelSnapshot() {
        return modelSnapshot;
    }

    public ModelSizeStats getModelSizeStats() {
        return modelSizeStats;
    }

    public ModelDebugOutput getModelDebugOutput() {
        return modelDebugOutput;
    }

    public CategoryDefinition getCategoryDefinition() {
        return categoryDefinition;
    }

    public FlushAcknowledgement getFlushAcknowledgement() {
        return flushAcknowledgement;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucket, records, influencers, categoryDefinition, flushAcknowledgement, modelDebugOutput, modelSizeStats,
                modelSnapshot, quantiles);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AutodetectResult other = (AutodetectResult) obj;
        return Objects.equals(bucket, other.bucket) &&
                Objects.equals(records, other.records) &&
                Objects.equals(influencers, other.influencers) &&
                Objects.equals(categoryDefinition, other.categoryDefinition) &&
                Objects.equals(flushAcknowledgement, other.flushAcknowledgement) &&
                Objects.equals(modelDebugOutput, other.modelDebugOutput) &&
                Objects.equals(modelSizeStats, other.modelSizeStats) &&
                Objects.equals(modelSnapshot, other.modelSnapshot) &&
                Objects.equals(quantiles, other.quantiles);
    }

}
