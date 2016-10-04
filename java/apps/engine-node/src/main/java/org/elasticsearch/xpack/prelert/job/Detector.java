
package org.elasticsearch.xpack.prelert.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.elasticsearch.xpack.prelert.job.detectionrules.DetectionRule;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Defines the fields to be used in the analysis.
 * <code>fieldname</code> must be set and only one of <code>byFieldName</code>
 * and <code>overFieldName</code> should be set.
 */
@JsonInclude(Include.NON_NULL)
public class Detector {
    public static final String DETECTOR_DESCRIPTION = "detectorDescription";
    public static final String FUNCTION = "function";
    public static final String FIELD_NAME = "fieldName";
    public static final String BY_FIELD_NAME = "byFieldName";
    public static final String OVER_FIELD_NAME = "overFieldName";
    public static final String PARTITION_FIELD_NAME = "partitionFieldName";
    public static final String USE_NULL = "useNull";
    public static final String EXCLUDE_FREQUENT = "excludeFrequent";
    public static final String DETECTOR_RULES = "detectorRules";


    public static final String COUNT = "count";
    public static final String HIGH_COUNT = "high_count";
    public static final String LOW_COUNT = "low_count";
    public static final String NON_ZERO_COUNT = "non_zero_count";
    public static final String LOW_NON_ZERO_COUNT = "low_non_zero_count";
    public static final String HIGH_NON_ZERO_COUNT = "high_non_zero_count";
    public static final String NZC = "nzc";
    public static final String LOW_NZC = "low_nzc";
    public static final String HIGH_NZC = "high_nzc";
    public static final String DISTINCT_COUNT = "distinct_count";
    public static final String LOW_DISTINCT_COUNT = "low_distinct_count";
    public static final String HIGH_DISTINCT_COUNT = "high_distinct_count";
    public static final String DC = "dc";
    public static final String LOW_DC = "low_dc";
    public static final String HIGH_DC = "high_dc";
    public static final String RARE = "rare";
    public static final String FREQ_RARE = "freq_rare";
    public static final String INFO_CONTENT = "info_content";
    public static final String LOW_INFO_CONTENT = "low_info_content";
    public static final String HIGH_INFO_CONTENT = "high_info_content";
    public static final String METRIC = "metric";
    public static final String MEAN = "mean";
    public static final String MEDIAN = "median";
    public static final String HIGH_MEAN = "high_mean";
    public static final String LOW_MEAN = "low_mean";
    public static final String AVG = "avg";
    public static final String HIGH_AVG = "high_avg";
    public static final String LOW_AVG = "low_avg";
    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String SUM = "sum";
    public static final String LOW_SUM = "low_sum";
    public static final String HIGH_SUM = "high_sum";
    public static final String NON_NULL_SUM = "non_null_sum";
    public static final String LOW_NON_NULL_SUM = "low_non_null_sum";
    public static final String HIGH_NON_NULL_SUM = "high_non_null_sum";
    /**
     * Population variance is called varp to match Splunk
     */
    public static final String POPULATION_VARIANCE = "varp";
    public static final String LOW_POPULATION_VARIANCE = "low_varp";
    public static final String HIGH_POPULATION_VARIANCE = "high_varp";
    public static final String TIME_OF_DAY = "time_of_day";
    public static final String TIME_OF_WEEK = "time_of_week";
    public static final String LAT_LONG = "lat_long";


    /**
     * The set of valid function names.
     */
    public static final Set<String> ANALYSIS_FUNCTIONS =
            new HashSet<>(Arrays.asList(
                    // The convention here is that synonyms (only) go on the same line
                    COUNT,
                    HIGH_COUNT,
                    LOW_COUNT,
                    NON_ZERO_COUNT, NZC,
                    LOW_NON_ZERO_COUNT, LOW_NZC,
                    HIGH_NON_ZERO_COUNT, HIGH_NZC,
                    DISTINCT_COUNT, DC,
                    LOW_DISTINCT_COUNT, LOW_DC,
                    HIGH_DISTINCT_COUNT, HIGH_DC,
                    RARE,
                    FREQ_RARE,
                    INFO_CONTENT,
                    LOW_INFO_CONTENT,
                    HIGH_INFO_CONTENT,
                    METRIC,
                    MEAN, AVG,
                    HIGH_MEAN, HIGH_AVG,
                    LOW_MEAN, LOW_AVG,
                    MEDIAN,
                    MIN,
                    MAX,
                    SUM,
                    LOW_SUM,
                    HIGH_SUM,
                    NON_NULL_SUM,
                    LOW_NON_NULL_SUM,
                    HIGH_NON_NULL_SUM,
                    POPULATION_VARIANCE,
                    LOW_POPULATION_VARIANCE,
                    HIGH_POPULATION_VARIANCE,
                    TIME_OF_DAY,
                    TIME_OF_WEEK,
                    LAT_LONG
            ));

    /**
     * The set of functions that do not require a field, by field or over field
     */
    public static final Set<String> COUNT_WITHOUT_FIELD_FUNCTIONS =
            new HashSet<>(Arrays.asList(
                    COUNT,
                    HIGH_COUNT,
                    LOW_COUNT,
                    NON_ZERO_COUNT, NZC,
                    LOW_NON_ZERO_COUNT, LOW_NZC,
                    HIGH_NON_ZERO_COUNT, HIGH_NZC,
                    TIME_OF_DAY,
                    TIME_OF_WEEK
            ));

    /**
     * The set of functions that require a fieldname
     */
    public static final Set<String> FIELD_NAME_FUNCTIONS =
            new HashSet<>(Arrays.asList(
                    DISTINCT_COUNT, DC,
                    LOW_DISTINCT_COUNT, LOW_DC,
                    HIGH_DISTINCT_COUNT, HIGH_DC,
                    INFO_CONTENT,
                    LOW_INFO_CONTENT,
                    HIGH_INFO_CONTENT,
                    METRIC,
                    MEAN, AVG,
                    HIGH_MEAN, HIGH_AVG,
                    LOW_MEAN, LOW_AVG,
                    MEDIAN,
                    MIN,
                    MAX,
                    SUM,
                    LOW_SUM,
                    HIGH_SUM,
                    NON_NULL_SUM,
                    LOW_NON_NULL_SUM,
                    HIGH_NON_NULL_SUM,
                    POPULATION_VARIANCE,
                    LOW_POPULATION_VARIANCE,
                    HIGH_POPULATION_VARIANCE,
                    LAT_LONG
            ));

    /**
     * The set of functions that require a by fieldname
     */
    public static final Set<String> BY_FIELD_NAME_FUNCTIONS =
            new HashSet<>(Arrays.asList(
                    RARE,
                    FREQ_RARE
            ));

    /**
     * The set of functions that require a over fieldname
     */
    public static final Set<String> OVER_FIELD_NAME_FUNCTIONS =
            new HashSet<>(Arrays.asList(
                    FREQ_RARE
            ));

    /**
     * The set of functions that cannot have a by fieldname
     */
    public static final Set<String> NO_BY_FIELD_NAME_FUNCTIONS =
            new HashSet<>();

    /**
     * The set of functions that cannot have an over fieldname
     */
    public static final Set<String> NO_OVER_FIELD_NAME_FUNCTIONS =
            new HashSet<>(Arrays.asList(
                    NON_ZERO_COUNT, NZC,
                    LOW_NON_ZERO_COUNT, LOW_NZC,
                    HIGH_NON_ZERO_COUNT, HIGH_NZC
            ));

    /**
     * The set of functions that must not be used with overlapping buckets
     */
    public static final Set<String> NO_OVERLAPPING_BUCKETS_FUNCTIONS =
            new HashSet<>(Arrays.asList(
                    RARE,
                    FREQ_RARE
            ));

    /**
     * The set of functions that should not be used with overlapping buckets
     * as they gain no benefit but have overhead
     */
    public static final Set<String> OVERLAPPING_BUCKETS_FUNCTIONS_NOT_NEEDED =
            new HashSet<>(Arrays.asList(
                    MIN,
                    MAX,
                    TIME_OF_DAY,
                    TIME_OF_WEEK
            ));

    /**
     * field names cannot contain any of these characters
     * ", \
     */
    public static final Character[] PROHIBITED_FIELDNAME_CHARACTERS = {'"', '\\'};
    public static final String PROHIBITED = String.join(",",
            Arrays.stream(PROHIBITED_FIELDNAME_CHARACTERS).map(
                    c -> Character.toString(c)).collect(Collectors.toList()));


    private String detectorDescription;
    private String function;
    private String fieldName;
    private String byFieldName;
    private String overFieldName;
    private String partitionFieldName;
    private Boolean useNull;
    private String excludeFrequent;
    private List<DetectionRule> detectorRules;

    public Detector() {
        // Default constructor
    }

    public String getDetectorDescription() {
        return detectorDescription;
    }

    public void setDetectorDescription(String description) {
        detectorDescription = description;
    }

    /**
     * The analysis function used e.g. count, rare, min etc. There is no
     * validation to check this value is one a predefined set
     *
     * @return The function or <code>null</code> if not set
     */
    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    /**
     * The Analysis field
     *
     * @return The field to analyse
     */
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * The 'by' field or <code>null</code> if not set.
     *
     * @return The 'by' field
     */
    public String getByFieldName() {
        return byFieldName;
    }

    public void setByFieldName(String byFieldName) {
        this.byFieldName = byFieldName;
    }

    /**
     * The 'over' field or <code>null</code> if not set.
     *
     * @return The 'over' field
     */
    public String getOverFieldName() {
        return overFieldName;
    }

    public void setOverFieldName(String overFieldName) {
        this.overFieldName = overFieldName;
    }

    /**
     * Segments the analysis along another field to have completely
     * independent baselines for each instance of partitionfield
     *
     * @return The Partition Field
     */
    public String getPartitionFieldName() {
        return partitionFieldName;
    }

    public void setPartitionFieldName(String partitionFieldName) {
        this.partitionFieldName = partitionFieldName;
    }


    /**
     * Where there isn't a value for the 'by' or 'over' field should a new
     * series be used as the 'null' series.
     *
     * @return true if the 'null' series should be created
     */
    public Boolean isUseNull() {
        return useNull;
    }

    public void setUseNull(Boolean useNull) {
        this.useNull = useNull;
    }

    /**
     * Excludes frequently-occuring metrics from the analysis;
     * can apply to 'by' field, 'over' field, or both
     *
     * @return the value that the user set
     */
    public String getExcludeFrequent() {
        return excludeFrequent;
    }

    public void setExcludeFrequent(String v) {
        excludeFrequent = v;
    }

    public List<DetectionRule> getDetectorRules() {
        return detectorRules;
    }

    public void setDetectorRules(List<DetectionRule> detectorRules) {
        this.detectorRules = detectorRules;
    }

    /**
     * Returns a list with the byFieldName, overFieldName and partitionFieldName that are not null
     *
     * @return a list with the byFieldName, overFieldName and partitionFieldName that are not null
     */
    public List<String> extractAnalysisFields() {
        List<String> analysisFields = Arrays.asList(getByFieldName(),
                getOverFieldName(), getPartitionFieldName());
        return analysisFields.stream().filter(item -> item != null).collect(Collectors.toList());
    }

    public Set<String> extractReferencedLists() {
        return detectorRules == null ? Collections.emptySet()
                : detectorRules.stream().map(DetectionRule::extractReferencedLists)
                .flatMap(Set::stream).collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof Detector == false) {
            return false;
        }

        Detector that = (Detector) other;

        return Objects.equals(this.detectorDescription, that.detectorDescription) &&
                Objects.equals(this.function, that.function) &&
                Objects.equals(this.fieldName, that.fieldName) &&
                Objects.equals(this.byFieldName, that.byFieldName) &&
                Objects.equals(this.overFieldName, that.overFieldName) &&
                Objects.equals(this.partitionFieldName, that.partitionFieldName) &&
                Objects.equals(this.useNull, that.useNull) &&
                Objects.equals(this.excludeFrequent, that.excludeFrequent) &&
                Objects.equals(this.detectorRules, that.detectorRules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(detectorDescription, function, fieldName, byFieldName,
                overFieldName, partitionFieldName, useNull, excludeFrequent,
                detectorRules);
    }
}
