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
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Autodetect analysis configuration options describes which fields are
 * analysed and the functions to use.
 * <p>
 * The configuration can contain multiple detectors, a new anomaly detector will
 * be created for each detector configuration. The fields
 * <code>bucketSpan, batchSpan, summaryCountFieldName and categorizationFieldName</code>
 * apply to all detectors.
 * <p>
 * If a value has not been set it will be <code>null</code>
 * Object wrappers are used around integral types &amp; booleans so they can take
 * <code>null</code> values.
 */
@JsonInclude(Include.NON_NULL)
public class AnalysisConfig extends ToXContentToBytes implements Writeable {
    /**
     * Serialisation names
     */
    public static final ParseField BUCKET_SPAN = new ParseField("bucketSpan");
    public static final ParseField BATCH_SPAN = new ParseField("batchSpan");
    public static final ParseField CATEGORIZATION_FIELD_NAME = new ParseField("categorizationFieldName");
    public static final ParseField CATEGORIZATION_FILTERS = new ParseField("categorizationFilters");
    public static final ParseField LATENCY = new ParseField("latency");
    public static final ParseField PERIOD = new ParseField("period");
    public static final ParseField SUMMARY_COUNT_FIELD_NAME = new ParseField("summaryCountFieldName");
    public static final ParseField DETECTORS = new ParseField("detectors");
    public static final ParseField INFLUENCERS = new ParseField("influencers");
    public static final ParseField OVERLAPPING_BUCKETS = new ParseField("overlappingBuckets");
    public static final ParseField RESULT_FINALIZATION_WINDOW = new ParseField("resultFinalizationWindow");
    public static final ParseField MULTIVARIATE_BY_FIELDS = new ParseField("multivariateByFields");
    public static final ParseField MULTIPLE_BUCKET_SPANS = new ParseField("multipleBucketSpans");
    public static final ParseField USER_PER_PARTITION_NORMALIZATION = new ParseField("usePerPartitionNormalization");

    public static final long DEFAULT_BUCKET_SPAN = 300;

    private static final String PRELERT_CATEGORY_FIELD = "prelertcategory";
    public static final Set<String> AUTO_CREATED_FIELDS = new HashSet<>(
            Arrays.asList(PRELERT_CATEGORY_FIELD));

    public static final long DEFAULT_RESULT_FINALIZATION_WINDOW = 2L;

    public static final ObjectParser<AnalysisConfig, ParseFieldMatcherSupplier> PARSER =
            new ObjectParser<>("schedule_config", AnalysisConfig::new);

    static {
        PARSER.declareLong(AnalysisConfig::setBucketSpan, BUCKET_SPAN);
        PARSER.declareLong(AnalysisConfig::setBatchSpan, BATCH_SPAN);
        PARSER.declareString(AnalysisConfig::setCategorizationFieldName, CATEGORIZATION_FIELD_NAME);
        PARSER.declareStringArray(AnalysisConfig::setCategorizationFilters, CATEGORIZATION_FILTERS);
        PARSER.declareLong(AnalysisConfig::setLatency, LATENCY);
        PARSER.declareLong(AnalysisConfig::setPeriod, PERIOD);
        PARSER.declareString(AnalysisConfig::setSummaryCountFieldName, SUMMARY_COUNT_FIELD_NAME);
        PARSER.declareObjectArray(AnalysisConfig::setDetectors, (p, c) -> Detector.PARSER.apply(p, c).build(), DETECTORS);
        PARSER.declareStringArray(AnalysisConfig::setInfluencers, INFLUENCERS);
        PARSER.declareBoolean(AnalysisConfig::setOverlappingBuckets, OVERLAPPING_BUCKETS);
        PARSER.declareLong(AnalysisConfig::setResultFinalizationWindow, RESULT_FINALIZATION_WINDOW);
        PARSER.declareBoolean(AnalysisConfig::setMultivariateByFields, MULTIVARIATE_BY_FIELDS);
        PARSER.declareLongArray(AnalysisConfig::setMultipleBucketSpans, MULTIPLE_BUCKET_SPANS);
        PARSER.declareBoolean(AnalysisConfig::setUsePerPartitionNormalization, USER_PER_PARTITION_NORMALIZATION);
    }

    /**
     * These values apply to all detectors
     */
    private Long bucketSpan;
    private Long batchSpan;
    private String categorizationFieldName;
    private List<String> categorizationFilters;
    private long latency = 0L;
    private Long period;
    private String summaryCountFieldName;
    private List<Detector> detectors = new ArrayList<>();
    private List<String> influencers = new ArrayList<>();
    private Boolean overlappingBuckets;
    private Long resultFinalizationWindow;
    private Boolean multivariateByFields;
    private List<Long> multipleBucketSpans;
    private boolean usePerPartitionNormalization = false;

    public AnalysisConfig() {
    }

    public AnalysisConfig(StreamInput in) throws IOException {
        bucketSpan = in.readOptionalLong();
        batchSpan = in.readOptionalLong();
        categorizationFieldName = in.readOptionalString();
        if (in.readBoolean()) {
            categorizationFilters = in.readList(StreamInput::readString);
        }
        latency = in.readLong();
        period = in.readOptionalLong();
        summaryCountFieldName = in.readOptionalString();
        detectors = in.readList(Detector::new);
        influencers = in.readList(StreamInput::readString);
        overlappingBuckets = in.readOptionalBoolean();
        resultFinalizationWindow = in.readOptionalLong();
        multivariateByFields = in.readOptionalBoolean();
        if (in.readBoolean()) {
            multipleBucketSpans = in.readList(StreamInput::readLong);
        }
        usePerPartitionNormalization = in.readBoolean();
    }

    /**
     * The size of the interval the analysis is aggregated into measured in
     * seconds
     *
     * @return The bucketspan or <code>null</code> if not set
     */
    public Long getBucketSpan() {
        return bucketSpan;
    }

    public void setBucketSpan(long bucketSpan) {
        this.bucketSpan = bucketSpan;
    }

    @JsonIgnore
    public long getBucketSpanOrDefault() {
        return bucketSpan == null ? DEFAULT_BUCKET_SPAN : bucketSpan;
    }

    /**
     * Interval into which to batch seasonal data measured in seconds
     *
     * @return The batchspan or <code>null</code> if not set
     */
    public Long getBatchSpan() {
        return batchSpan;
    }

    public void setBatchSpan(long batchSpan) {
        this.batchSpan = batchSpan;
    }

    public String getCategorizationFieldName() {
        return categorizationFieldName;
    }

    public void setCategorizationFieldName(String categorizationFieldName) {
        this.categorizationFieldName = Objects.requireNonNull(categorizationFieldName);
    }

    public List<String> getCategorizationFilters() {
        return categorizationFilters;
    }

    public void setCategorizationFilters(List<String> filters) {
        categorizationFilters = Objects.requireNonNull(filters);
    }

    /**
     * The latency interval (seconds) during which out-of-order records should be handled.
     *
     * @return The latency interval (seconds) or <code>null</code> if not set
     */
    public Long getLatency() {
        return latency;
    }

    /**
     * Set the latency interval during which out-of-order records should be handled.
     *
     * @param latency the latency interval in seconds
     */
    public void setLatency(long latency) {
        this.latency = latency;
    }

    /**
     * The repeat interval for periodic data in multiples of
     * {@linkplain #getBatchSpan()}
     *
     * @return The period or <code>null</code> if not set
     */
    public Long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    /**
     * The name of the field that contains counts for pre-summarised input
     *
     * @return The field name or <code>null</code> if not set
     */
    public String getSummaryCountFieldName() {
        return summaryCountFieldName;
    }

    public void setSummaryCountFieldName(String summaryCountFieldName) {
        this.summaryCountFieldName = Objects.requireNonNull(summaryCountFieldName);
    }

    /**
     * The list of analysis detectors. In a valid configuration the list should
     * contain at least 1 {@link Detector}
     *
     * @return The Detectors used in this job
     */
    public List<Detector> getDetectors() {
        return detectors;
    }

    public void setDetectors(List<Detector> detectors) {
        this.detectors = Objects.requireNonNull(detectors);
    }

    /**
     * The list of influence field names
     */
    public List<String> getInfluencers() {
        return influencers;
    }

    public void setInfluencers(List<String> influencers) {
        this.influencers = Objects.requireNonNull(influencers);
    }

    /**
     * Return the list of term fields.
     * These are the influencer fields, partition field,
     * by field and over field of each detector.
     * <code>null</code> and empty strings are filtered from the
     * config.
     *
     * @return Set of term fields - never <code>null</code>
     */
    public Set<String> termFields() {
        Set<String> termFields = new TreeSet<>();

        for (Detector d : getDetectors()) {
            addIfNotNull(termFields, d.getByFieldName());
            addIfNotNull(termFields, d.getOverFieldName());
            addIfNotNull(termFields, d.getPartitionFieldName());
        }

        for (String i : getInfluencers()) {
            addIfNotNull(termFields, i);
        }

        // remove empty strings
        termFields.remove("");

        return termFields;
    }

    public Set<String> extractReferencedLists() {
        return detectors.stream().map(Detector::extractReferencedLists)
                .flatMap(Set::stream).collect(Collectors.toSet());
    }

    public Boolean getOverlappingBuckets() {
        return overlappingBuckets;
    }

    public void setOverlappingBuckets(boolean b) {
        overlappingBuckets = b;
    }

    public Long getResultFinalizationWindow() {
        return resultFinalizationWindow;
    }

    public void setResultFinalizationWindow(long l) {
        resultFinalizationWindow = l;
    }

    public Boolean getMultivariateByFields() {
        return multivariateByFields;
    }

    public void setMultivariateByFields(boolean multivariateByFields) {
        this.multivariateByFields = multivariateByFields;
    }

    public List<Long> getMultipleBucketSpans() {
        return multipleBucketSpans;
    }

    public void setMultipleBucketSpans(List<Long> multipleBucketSpans) {
        this.multipleBucketSpans = Objects.requireNonNull(multipleBucketSpans);
    }

    public boolean getUsePerPartitionNormalization() {
        return usePerPartitionNormalization;
    }

    public void setUsePerPartitionNormalization(boolean usePerPartitionNormalization) {
        this.usePerPartitionNormalization = usePerPartitionNormalization;
    }

    /**
     * Return the list of fields required by the analysis.
     * These are the influencer fields, metric field, partition field,
     * by field and over field of each detector, plus the summary count
     * field and the categorization field name of the job.
     * <code>null</code> and empty strings are filtered from the
     * config.
     *
     * @return List of required analysis fields - never <code>null</code>
     */
    public List<String> analysisFields() {
        Set<String> analysisFields = termFields();

        addIfNotNull(analysisFields, categorizationFieldName);
        addIfNotNull(analysisFields, summaryCountFieldName);

        for (Detector d : getDetectors()) {
            addIfNotNull(analysisFields, d.getFieldName());
        }

        // remove empty strings
        analysisFields.remove("");

        return new ArrayList<>(analysisFields);
    }

    private static void addIfNotNull(Set<String> fields, String field) {
        if (field != null) {
            fields.add(field);
        }
    }

    public List<String> fields() {
        return collectNonNullAndNonEmptyDetectorFields(d -> d.getFieldName());
    }

    private List<String> collectNonNullAndNonEmptyDetectorFields(
            Function<Detector, String> fieldGetter) {
        Set<String> fields = new HashSet<>();

        for (Detector d : getDetectors()) {
            addIfNotNull(fields, fieldGetter.apply(d));
        }

        // remove empty strings
        fields.remove("");

        return new ArrayList<>(fields);
    }

    public List<String> byFields() {
        return collectNonNullAndNonEmptyDetectorFields(d -> d.getByFieldName());
    }

    public List<String> overFields() {
        return collectNonNullAndNonEmptyDetectorFields(d -> d.getOverFieldName());
    }


    public List<String> partitionFields() {
        return collectNonNullAndNonEmptyDetectorFields(d -> d.getPartitionFieldName());
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeOptionalLong(bucketSpan);
        out.writeOptionalLong(batchSpan);
        out.writeOptionalString(categorizationFieldName);
        if (categorizationFilters != null) {
            out.writeBoolean(true);
            out.writeStringList(categorizationFilters);
        } else {
            out.writeBoolean(false);
        }
        out.writeLong(latency);
        out.writeOptionalLong(period);
        out.writeOptionalString(summaryCountFieldName);
        out.writeList(detectors);
        out.writeStringList(influencers);
        out.writeOptionalBoolean(overlappingBuckets);
        out.writeOptionalLong(resultFinalizationWindow);
        out.writeOptionalBoolean(multivariateByFields);
        if (multipleBucketSpans != null) {
            out.writeBoolean(true);
            out.writeVInt(multipleBucketSpans.size());
            for (Long bucketSpan : multipleBucketSpans) {
                out.writeLong(bucketSpan);
            }
        } else {
            out.writeBoolean(false);
        }
        out.writeBoolean(usePerPartitionNormalization);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (bucketSpan != null) {
            builder.field(BUCKET_SPAN.getPreferredName(), bucketSpan);
        }
        if (batchSpan != null) {
            builder.field(BATCH_SPAN.getPreferredName(), batchSpan);
        }
        if (categorizationFieldName != null) {
            builder.field(CATEGORIZATION_FIELD_NAME.getPreferredName(), categorizationFieldName);
        }
        if (categorizationFilters != null) {
            builder.field(CATEGORIZATION_FILTERS.getPreferredName(), categorizationFilters);
        }
        builder.field(LATENCY.getPreferredName(), latency);
        if (period != null) {
            builder.field(PERIOD.getPreferredName(), period);
        }
        if (summaryCountFieldName != null) {
            builder.field(SUMMARY_COUNT_FIELD_NAME.getPreferredName(), summaryCountFieldName);
        }
        builder.field(DETECTORS.getPreferredName(), detectors);
        builder.field(INFLUENCERS.getPreferredName(), influencers);
        if (overlappingBuckets != null) {
            builder.field(OVERLAPPING_BUCKETS.getPreferredName(), overlappingBuckets);
        }
        if (resultFinalizationWindow != null) {
            builder.field(RESULT_FINALIZATION_WINDOW.getPreferredName(), resultFinalizationWindow);
        }
        if (multivariateByFields != null) {
            builder.field(MULTIVARIATE_BY_FIELDS.getPreferredName(), multivariateByFields);
        }
        if (multipleBucketSpans != null) {
            builder.field(MULTIPLE_BUCKET_SPANS.getPreferredName(), multipleBucketSpans);
        }
        builder.field(USER_PER_PARTITION_NORMALIZATION.getPreferredName(), usePerPartitionNormalization);
        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisConfig that = (AnalysisConfig) o;
        return latency == that.latency &&
                usePerPartitionNormalization == that.usePerPartitionNormalization &&
                Objects.equals(bucketSpan, that.bucketSpan) &&
                Objects.equals(batchSpan, that.batchSpan) &&
                Objects.equals(categorizationFieldName, that.categorizationFieldName) &&
                Objects.equals(categorizationFilters, that.categorizationFilters) &&
                Objects.equals(period, that.period) &&
                Objects.equals(summaryCountFieldName, that.summaryCountFieldName) &&
                Objects.equals(detectors, that.detectors) &&
                Objects.equals(influencers, that.influencers) &&
                Objects.equals(overlappingBuckets, that.overlappingBuckets) &&
                Objects.equals(resultFinalizationWindow, that.resultFinalizationWindow) &&
                Objects.equals(multivariateByFields, that.multivariateByFields) &&
                Objects.equals(multipleBucketSpans, that.multipleBucketSpans);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                bucketSpan, batchSpan, categorizationFieldName, categorizationFilters, latency, period,
                summaryCountFieldName, detectors, influencers, overlappingBuckets, resultFinalizationWindow,
                multivariateByFields, multipleBucketSpans, usePerPartitionNormalization
                );
    }
}
