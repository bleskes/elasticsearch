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
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.common.xcontent.XContentParser.Token;
import org.elasticsearch.xpack.prelert.utils.time.TimeUtils;

import java.io.IOException;
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

public class DataCounts extends ToXContentToBytes implements Writeable {

    public static final String BUCKET_COUNT_STR = "bucketCount";
    public static final String PROCESSED_RECORD_COUNT_STR = "processedRecordCount";
    public static final String PROCESSED_FIELD_COUNT_STR = "processedFieldCount";
    public static final String INPUT_BYTES_STR = "inputBytes";
    public static final String INPUT_RECORD_COUNT_STR = "inputRecordCount";
    public static final String INPUT_FIELD_COUNT_STR = "inputFieldCount";
    public static final String INVALID_DATE_COUNT_STR = "invalidDateCount";
    public static final String MISSING_FIELD_COUNT_STR = "missingFieldCount";
    public static final String OUT_OF_ORDER_TIME_COUNT_STR = "outOfOrderTimeStampCount";
    public static final String FAILED_TRANSFORM_COUNT_STR = "failedTransformCount";
    public static final String EXCLUDED_RECORD_COUNT_STR = "excludedRecordCount";
    public static final String LATEST_RECORD_TIME_STR = "latestRecordTimeStamp";

    public static final ParseField BUCKET_COUNT = new ParseField(BUCKET_COUNT_STR);
    public static final ParseField PROCESSED_RECORD_COUNT = new ParseField(PROCESSED_RECORD_COUNT_STR);
    public static final ParseField PROCESSED_FIELD_COUNT = new ParseField(PROCESSED_FIELD_COUNT_STR);
    public static final ParseField INPUT_BYTES = new ParseField(INPUT_BYTES_STR);
    public static final ParseField INPUT_RECORD_COUNT = new ParseField(INPUT_RECORD_COUNT_STR);
    public static final ParseField INPUT_FIELD_COUNT = new ParseField(INPUT_FIELD_COUNT_STR);
    public static final ParseField INVALID_DATE_COUNT = new ParseField(INVALID_DATE_COUNT_STR);
    public static final ParseField MISSING_FIELD_COUNT = new ParseField(MISSING_FIELD_COUNT_STR);
    public static final ParseField OUT_OF_ORDER_TIME_COUNT = new ParseField(OUT_OF_ORDER_TIME_COUNT_STR);
    public static final ParseField FAILED_TRANSFORM_COUNT = new ParseField(FAILED_TRANSFORM_COUNT_STR);
    public static final ParseField EXCLUDED_RECORD_COUNT = new ParseField(EXCLUDED_RECORD_COUNT_STR);
    public static final ParseField LATEST_RECORD_TIME = new ParseField(LATEST_RECORD_TIME_STR);

    public static final ParseField TYPE = new ParseField("dataCounts");

    public static final ObjectParser<DataCounts, ParseFieldMatcherSupplier> PARSER =
            new ObjectParser<>("data_counts", DataCounts::new);

    static {
        PARSER.declareLong(DataCounts::setBucketCount, BUCKET_COUNT);
        PARSER.declareLong(DataCounts::setProcessedRecordCount, PROCESSED_RECORD_COUNT);
        PARSER.declareLong(DataCounts::setProcessedFieldCount, PROCESSED_FIELD_COUNT);
        PARSER.declareLong(DataCounts::setInputBytes, INPUT_BYTES);
        PARSER.declareLong(DataCounts::setInputRecordCount, INPUT_RECORD_COUNT);
        PARSER.declareLong(DataCounts::setInputFieldCount, INPUT_FIELD_COUNT);
        PARSER.declareLong(DataCounts::setInvalidDateCount, INVALID_DATE_COUNT);
        PARSER.declareLong(DataCounts::setMissingFieldCount, MISSING_FIELD_COUNT);
        PARSER.declareLong(DataCounts::setOutOfOrderTimeStampCount, OUT_OF_ORDER_TIME_COUNT);
        PARSER.declareLong(DataCounts::setFailedTransformCount, FAILED_TRANSFORM_COUNT);
        PARSER.declareLong(DataCounts::setExcludedRecordCount, EXCLUDED_RECORD_COUNT);
        PARSER.declareField(DataCounts::setLatestRecordTimeStamp, p -> {
            if (p.currentToken() == Token.VALUE_NUMBER) {
                return new Date(p.longValue());
            } else if (p.currentToken() == Token.VALUE_STRING) {
                return new Date(TimeUtils.dateStringToEpoch(p.text()));
            }
            throw new IllegalArgumentException(
                    "unexpected token [" + p.currentToken() + "] for [" + LATEST_RECORD_TIME.getPreferredName() + "]");
        }, LATEST_RECORD_TIME, ValueType.VALUE);
    }

    private long bucketCount;
    private long processedRecordCount;
    private long processedFieldCount;
    private long inputBytes;
    private long inputFieldCount;
    private long invalidDateCount;
    private long missingFieldCount;
    private long outOfOrderTimeStampCount;
    private long failedTransformCount;
    private long excludedRecordCount;
    // NORELEASE: Use Jodatime instead
    private Date latestRecordTimeStamp;

    public DataCounts() {
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
        excludedRecordCount = lhs.excludedRecordCount;
        latestRecordTimeStamp = lhs.latestRecordTimeStamp;
    }

    public DataCounts(StreamInput in) throws IOException {
        bucketCount = in.readVLong();
        processedRecordCount = in.readVLong();
        processedFieldCount = in.readVLong();
        inputBytes = in.readVLong();
        inputFieldCount = in.readVLong();
        invalidDateCount = in.readVLong();
        missingFieldCount = in.readVLong();
        outOfOrderTimeStampCount = in.readVLong();
        failedTransformCount = in.readVLong();
        excludedRecordCount = in.readVLong();
        if (in.readBoolean()) {
            latestRecordTimeStamp = new Date(in.readVLong());
        }
    }


    /**
     * The number of bucket results
     *
     * @return May be <code>null</code>
     */
    public long getBucketCount() {
        return bucketCount;
    }

    public void setBucketCount(long count) {
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
    public long getInputRecordCount() {
        return processedRecordCount + outOfOrderTimeStampCount
                + invalidDateCount + excludedRecordCount;
    }

    /**
     * Only present to keep jackson serialisation happy.
     * This property should not be deserialised
     */
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
        map.put(BUCKET_COUNT.getPreferredName(), bucketCount);
        map.put(PROCESSED_RECORD_COUNT.getPreferredName(), processedRecordCount);
        map.put(PROCESSED_FIELD_COUNT.getPreferredName(), processedFieldCount);
        map.put(INPUT_BYTES.getPreferredName(), inputBytes);
        map.put(INPUT_RECORD_COUNT.getPreferredName(), getInputRecordCount());
        map.put(INPUT_FIELD_COUNT.getPreferredName(), inputFieldCount);
        map.put(INVALID_DATE_COUNT.getPreferredName(), invalidDateCount);
        map.put(MISSING_FIELD_COUNT.getPreferredName(), missingFieldCount);
        map.put(OUT_OF_ORDER_TIME_COUNT.getPreferredName(), outOfOrderTimeStampCount);
        map.put(FAILED_TRANSFORM_COUNT.getPreferredName(), failedTransformCount);
        map.put(LATEST_RECORD_TIME.getPreferredName(), latestRecordTimeStamp);
        map.put(EXCLUDED_RECORD_COUNT.getPreferredName(), excludedRecordCount);
        return map;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVLong(bucketCount);
        out.writeVLong(processedRecordCount);
        out.writeVLong(processedFieldCount);
        out.writeVLong(inputBytes);
        out.writeVLong(inputFieldCount);
        out.writeVLong(invalidDateCount);
        out.writeVLong(missingFieldCount);
        out.writeVLong(outOfOrderTimeStampCount);
        out.writeVLong(failedTransformCount);
        out.writeVLong(excludedRecordCount);
        if (latestRecordTimeStamp != null) {
            out.writeBoolean(true);
            out.writeVLong(latestRecordTimeStamp.getTime());
        } else {
            out.writeBoolean(false);
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        doXContentBody(builder, params);
        builder.endObject();
        return builder;
    }

    public XContentBuilder doXContentBody(XContentBuilder builder, Params params) throws IOException {
        builder.field(BUCKET_COUNT.getPreferredName(), bucketCount);
        builder.field(PROCESSED_RECORD_COUNT.getPreferredName(), processedRecordCount);
        builder.field(PROCESSED_FIELD_COUNT.getPreferredName(), processedFieldCount);
        builder.field(INPUT_BYTES.getPreferredName(), inputBytes);
        builder.field(INPUT_FIELD_COUNT.getPreferredName(), inputFieldCount);
        builder.field(INVALID_DATE_COUNT.getPreferredName(), invalidDateCount);
        builder.field(MISSING_FIELD_COUNT.getPreferredName(), missingFieldCount);
        builder.field(OUT_OF_ORDER_TIME_COUNT.getPreferredName(), outOfOrderTimeStampCount);
        builder.field(FAILED_TRANSFORM_COUNT.getPreferredName(), failedTransformCount);
        builder.field(EXCLUDED_RECORD_COUNT.getPreferredName(), excludedRecordCount);
        if (latestRecordTimeStamp != null) {
            builder.field(LATEST_RECORD_TIME.getPreferredName(), latestRecordTimeStamp.getTime());
        }

        return builder;
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

        return this.bucketCount == that.bucketCount &&
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
