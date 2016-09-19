/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.xpack.prelert.job;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.*;
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
public class AnalysisConfig {
    /**
     * Serialisation names
     */
    public static final String BUCKET_SPAN = "bucketSpan";
    public static final String BATCH_SPAN = "batchSpan";
    public static final String CATEGORIZATION_FIELD_NAME = "categorizationFieldName";
    public static final String CATEGORIZATION_FILTERS = "categorizationFilters";
    public static final String LATENCY = "latency";
    public static final String PERIOD = "period";
    public static final String SUMMARY_COUNT_FIELD_NAME = "summaryCountFieldName";
    public static final String DETECTORS = "detectors";
    public static final String INFLUENCERS = "influencers";
    public static final String OVERLAPPING_BUCKETS = "overlappingBuckets";
    public static final String RESULT_FINALIZATION_WINDOW = "resultFinalizationWindow";
    public static final String MULTIVARIATE_BY_FIELDS = "multivariateByFields";
    public static final String MULTIPLE_BUCKET_SPANS = "multipleBucketSpans";

    public static final long DEFAULT_BUCKET_SPAN = 300;

    private static final String PRELERT_CATEGORY_FIELD = "prelertcategory";
    public static final Set<String> AUTO_CREATED_FIELDS = new HashSet<>(
            Arrays.asList(PRELERT_CATEGORY_FIELD));

    public static final long DEFAULT_RESULT_FINALIZATION_WINDOW = 2L;

    /**
     * These values apply to all detectors
     */
    private Long bucketSpan;
    private Long batchSpan;
    private String categorizationFieldName;
    private List<String> categorizationFilters;
    private Long latency = 0L;
    private Long period;
    private String summaryCountFieldName;
    private List<Detector> detectors;
    private List<String> influencers;
    private Boolean overlappingBuckets;
    private Long resultFinalizationWindow;
    private Boolean multivariateByFields;
    private List<Long> multipleBucketSpans;
    private boolean usePerPartitionNormalization;

    /**
     * Default constructor
     */
    public AnalysisConfig() {
        detectors = new ArrayList<>();
        influencers = new ArrayList<>();
        usePerPartitionNormalization = false;
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

    public void setBucketSpan(Long span) {
        bucketSpan = span;
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

    public void setBatchSpan(Long batchSpan) {
        this.batchSpan = batchSpan;
    }

    public String getCategorizationFieldName() {
        return categorizationFieldName;
    }

    public void setCategorizationFieldName(String categorizationFieldName) {
        this.categorizationFieldName = categorizationFieldName;
    }

    public List<String> getCategorizationFilters() {
        return categorizationFilters;
    }

    public void setCategorizationFilters(List<String> filters) {
        categorizationFilters = filters;
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
    public void setLatency(Long latency) {
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

    public void setPeriod(Long period) {
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
        this.summaryCountFieldName = summaryCountFieldName;
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
        this.detectors = detectors;
    }

    /**
     * The list of influence field names
     */
    public List<String> getInfluencers() {
        return influencers;
    }

    public void setInfluencers(List<String> influencers) {
        this.influencers = influencers;
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

    public void setOverlappingBuckets(Boolean b) {
        overlappingBuckets = b;
    }

    public Long getResultFinalizationWindow() {
        return resultFinalizationWindow;
    }

    public void setResultFinalizationWindow(Long l) {
        resultFinalizationWindow = l;
    }

    public Boolean getMultivariateByFields() {
        return multivariateByFields;
    }

    public void setMultivariateByFields(Boolean multivariateByFields) {
        this.multivariateByFields = multivariateByFields;
    }

    public List<Long> getMultipleBucketSpans() {
        return multipleBucketSpans;
    }

    public void setMultipleBucketSpans(List<Long> l) {
        multipleBucketSpans = l;
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

    /**
     * The array of detectors are compared for equality but they are not sorted
     * first so this test could fail simply because the detector arrays are in
     * different orders.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof AnalysisConfig == false) {
            return false;
        }

        AnalysisConfig that = (AnalysisConfig) other;

        if (this.detectors.size() != that.detectors.size()) {
            return false;
        }

        for (int i = 0; i < detectors.size(); i++) {
            if (!this.detectors.get(i).equals(that.detectors.get(i))) {
                return false;
            }
        }

        return Objects.equals(this.bucketSpan, that.bucketSpan) &&
                Objects.equals(this.batchSpan, that.batchSpan) &&
                Objects.equals(this.categorizationFieldName, that.categorizationFieldName) &&
                Objects.equals(this.categorizationFilters, that.categorizationFilters) &&
                Objects.equals(this.latency, that.latency) &&
                Objects.equals(this.period, that.period) &&
                Objects.equals(this.summaryCountFieldName, that.summaryCountFieldName) &&
                Objects.equals(this.influencers, that.influencers) &&
                Objects.equals(this.overlappingBuckets, that.overlappingBuckets) &&
                Objects.equals(this.resultFinalizationWindow, that.resultFinalizationWindow) &&
                Objects.equals(this.multivariateByFields, that.multivariateByFields) &&
                Objects.equals(this.multipleBucketSpans, that.multipleBucketSpans) &&
                Objects.equals(this.usePerPartitionNormalization, that.usePerPartitionNormalization);
    }

    @Override
    public int hashCode() {
        return Objects.hash(detectors, bucketSpan, batchSpan, categorizationFieldName,
                categorizationFilters, latency, period, summaryCountFieldName,
                influencers, overlappingBuckets, resultFinalizationWindow,
                multivariateByFields, multipleBucketSpans, usePerPartitionNormalization);
    }

}
