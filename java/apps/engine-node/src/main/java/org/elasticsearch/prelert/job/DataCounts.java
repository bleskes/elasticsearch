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

package org.elasticsearch.prelert.job;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Job processed record counts.
 * <p>
 * The getInput... methods return the actual number of
 * fields/records sent the the API including invalid records.
 * The getProcessed... methods are the number sent to the
 * Engine.
 * <p>
 * The <code>inputRecordCount</code> field is calculated so it
 * should not be set in deserialisation but it should be read in
 * serialistion - hence the annotations and the private setter
 */

@JsonInclude(Include.NON_NULL)
public class DataCounts {
    public static final String BUCKET_COUNT = "bucketCount";
    public static final String PROCESSED_RECORD_COUNT = "processedRecordCount";
    public static final String PROCESSED_FIELD_COUNT = "processedFieldCount";
    public static final String INPUT_BYTES = "inputBytes";
    public static final String INPUT_RECORD_COUNT = "inputRecordCount";
    public static final String INPUT_FIELD_COUNT = "inputFieldCount";
    public static final String INVALID_DATE_COUNT = "invalidDateCount";
    public static final String MISSING_FIELD_COUNT = "missingFieldCount";
    public static final String OUT_OF_ORDER_TIME_COUNT = "outOfOrderTimeStampCount";
    public static final String FAILED_TRANSFORM_COUNT = "failedTransformCount";
    public static final String LATEST_RECORD_TIME = "latestRecordTimeStamp";
    public static final String EXCLUDED_RECORD_COUNT = "excludedRecordCount";


    private Long bucketCount;
    private long processedRecordCount;
    private long processedFieldCount;
    private long inputBytes;
    private long inputFieldCount;
    private long invalidDateCount;
    private long missingFieldCount;
    private long outOfOrderTimeStampCount;
    private long failedTransformCount;
    private long excludedRecordCount;
    private Date latestRecordTimeStamp;

    public DataCounts() {
        bucketCount = new Long(0);
    }

    public DataCounts(DataCounts lhs) {
        bucketCount = lhs.bucketCount;
        processedRecordCount = lhs.processedRecordCount;
        processedFieldCount = lhs.processedFieldCount;
        inputBytes = lhs.inputBytes;
        inputFieldCount = lhs.inputFieldCount;
        invalidDateCount = lhs.invalidDateCount;
        missingFieldCount = lhs.missingFieldCount;
        outOfOrderTimeStampCount = lhs.outOfOrderTimeStampCount;
        failedTransformCount = lhs.failedTransformCount;
        latestRecordTimeStamp = lhs.latestRecordTimeStamp;
        excludedRecordCount = lhs.excludedRecordCount;
    }


    /**
     * The number of bucket results
     *
     * @return May be <code>null</code>
     */
    public Long getBucketCount() {
        return bucketCount;
    }

    public void setBucketCount(Long count) {
        bucketCount = count;
    }

    /**
     * Number of records processed by this job.
     * This value is the number of records sent passed on to
     * the engine i.e. {@linkplain #getInputRecordCount()} minus
     * records with bad dates or out of order
     *
     * @return Number of records processed by this job {@code long}
     */
    public long getProcessedRecordCount() {
        return processedRecordCount;
    }

    public void setProcessedRecordCount(long count) {
        processedRecordCount = count;
    }

    public void incrementProcessedRecordCount(long additional) {
        processedRecordCount += additional;
    }

    /**
     * Number of data points (processed record count * the number
     * of analysed fields) processed by this job. This count does
     * not include the time field.
     *
     * @return Number of data points processed by this job {@code long}
     */
    public long getProcessedFieldCount() {
        return processedFieldCount;
    }

    public void setProcessedFieldCount(long count) {
        processedFieldCount = count;
    }

    public void calcProcessedFieldCount(long analysisFieldsPerRecord) {
        processedFieldCount =
                (processedRecordCount * analysisFieldsPerRecord)
                        - missingFieldCount;

        // processedFieldCount could be a -ve value if no
        // records have been written in which case it should be 0
        processedFieldCount = (processedFieldCount < 0) ? 0 : processedFieldCount;
    }

    /**
     * Total number of input records read.
     * This = processed record count + date parse error records count
     * + out of order record count.
     * <p>
     * Records with missing fields are counted as they are still written.
     *
     * @return Total number of input records read {@code long}
     */
    @JsonProperty
    public long getInputRecordCount() {
        return processedRecordCount + outOfOrderTimeStampCount
                + invalidDateCount + excludedRecordCount;
    }

    /**
     * Only present to keep jackson serialisation happy.
     * This property should not be deserialised
     *
     * @param count
     */
    @JsonIgnore
    private void setInputRecordCount(long count) {
        throw new IllegalStateException();
    }

    /**
     * The total number of bytes sent to this job.
     * This value includes the bytes from any  records
     * that have been discarded for any  reason
     * e.g. because the date cannot be read
     *
     * @return Volume in bytes
     */
    public long getInputBytes() {
        return inputBytes;
    }

    public void setInputBytes(long volume) {
        inputBytes = volume;
    }

    public void incrementInputBytes(long additional) {
        inputBytes += additional;
    }

    /**
     * The total number of fields sent to the job
     * including fields that aren't analysed.
     *
     * @return The total number of fields sent to the job
     */
    public long getInputFieldCount() {
        return inputFieldCount;
    }

    public void setInputFieldCount(long count) {
        inputFieldCount = count;
    }

    public void incrementInputFieldCount(long additional) {
        inputFieldCount += additional;
    }

    /**
     * The number of records with an invalid date field that could
     * not be parsed or converted to epoch time.
     *
     * @return The number of records with an invalid date field
     */
    public long getInvalidDateCount() {
        return invalidDateCount;
    }

    public void setInvalidDateCount(long count) {
        invalidDateCount = count;
    }

    public void incrementInvalidDateCount(long additional) {
        invalidDateCount += additional;
    }


    /**
     * The number of missing fields that had been
     * configured for analysis.
     *
     * @return The number of missing fields
     */
    public long getMissingFieldCount() {
        return missingFieldCount;
    }

    public void setMissingFieldCount(long count) {
        missingFieldCount = count;
    }

    public void incrementMissingFieldCount(long additional) {
        missingFieldCount += additional;
    }

    /**
     * The number of records with a timestamp that is
     * before the time of the latest record. Records should
     * be in ascending chronological order
     *
     * @return The number of records with a timestamp that is before the time of the latest record
     */
    public long getOutOfOrderTimeStampCount() {
        return outOfOrderTimeStampCount;
    }

    public void setOutOfOrderTimeStampCount(long count) {
        outOfOrderTimeStampCount = count;
    }

    public void incrementOutOfOrderTimeStampCount(long additional) {
        outOfOrderTimeStampCount += additional;
    }


    /**
     * The number of transforms that failed.
     * In theory this could be more than the number of records
     * if multiple transforms are applied to each record
     *
     * @return The number of transforms that failed
     */
    public long getFailedTransformCount() {
        return failedTransformCount;
    }

    public void setFailedTransformCount(long failedTransformCount) {
        this.failedTransformCount = failedTransformCount;
    }

    public void incrementFailedTransformCount(long additional) {
        failedTransformCount += additional;
    }

    /**
     * The number of records excluded by a transform
     *
     * @return Number of excluded records
     */
    public long getExcludedRecordCount() {
        return excludedRecordCount;
    }

    public void setExcludedRecordCount(long excludedRecordCount) {
        this.excludedRecordCount = excludedRecordCount;
    }

    public void incrementExcludedRecordCount(long additional) {
        excludedRecordCount += additional;
    }

    /**
     * The time of the latest record seen.
     *
     * @return Latest record time
     */
    public Date getLatestRecordTimeStamp() {
        return latestRecordTimeStamp;
    }

    public void setLatestRecordTimeStamp(Date latestRecordTime) {
        latestRecordTimeStamp = latestRecordTime;
    }

    public Map<String, Object> toObjectMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(BUCKET_COUNT, bucketCount);
        map.put(PROCESSED_RECORD_COUNT, processedRecordCount);
        map.put(PROCESSED_FIELD_COUNT, processedFieldCount);
        map.put(INPUT_BYTES, inputBytes);
        map.put(INPUT_RECORD_COUNT, getInputRecordCount());
        map.put(INPUT_FIELD_COUNT, inputFieldCount);
        map.put(INVALID_DATE_COUNT, invalidDateCount);
        map.put(MISSING_FIELD_COUNT, missingFieldCount);
        map.put(OUT_OF_ORDER_TIME_COUNT, outOfOrderTimeStampCount);
        map.put(FAILED_TRANSFORM_COUNT, failedTransformCount);
        map.put(LATEST_RECORD_TIME, latestRecordTimeStamp);
        map.put(EXCLUDED_RECORD_COUNT, excludedRecordCount);
        return map;
    }

    /**
     * Equality test
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof DataCounts == false) {
            return false;
        }

        DataCounts that = (DataCounts) other;

        return this.bucketCount.equals(that.bucketCount) &&
                this.processedRecordCount == that.processedRecordCount &&
                this.processedFieldCount == that.processedFieldCount &&
                this.inputBytes == that.inputBytes &&
                this.inputFieldCount == that.inputFieldCount &&
                this.invalidDateCount == that.invalidDateCount &&
                this.missingFieldCount == that.missingFieldCount &&
                this.outOfOrderTimeStampCount == that.outOfOrderTimeStampCount &&
                this.failedTransformCount == that.failedTransformCount &&
                this.excludedRecordCount == that.excludedRecordCount &&
                Objects.equals(this.latestRecordTimeStamp, that.latestRecordTimeStamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketCount, processedRecordCount, processedFieldCount,
                inputBytes, inputFieldCount, invalidDateCount, missingFieldCount,
                outOfOrderTimeStampCount, failedTransformCount, excludedRecordCount,
                latestRecordTimeStamp);
    }
}
