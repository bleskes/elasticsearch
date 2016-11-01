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
package org.elasticsearch.xpack.prelert.job.status;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.persistence.JobDataCountsPersister;
import org.elasticsearch.xpack.prelert.job.usage.UsageReporter;


/**
 * Status reporter for tracking all the good/bad
 * records written to the API. Call one of the reportXXX() methods
 * to update the records counts if {@linkplain #isReportingBoundary(long)}
 * returns true then the count will be logged and the counts persisted
 * via the {@linkplain JobDataCountsPersister}.
 * If there is a high proportion of errors the
 * {@linkplain StatusReporter#checkStatus(long)} method throws an exception.
 */
public class StatusReporter {
    /**
     * The max percentage of date parse errors allowed before
     * an exception is thrown.
     */
    public static final Setting<Integer> ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_SETTING = Setting.intSetting("max.percent.date.errors", 25,
            Property.NodeScope);

    /**
     * The max percentage of out of order records allowed before
     * an exception is thrown.
     */
    public static final Setting<Integer> ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_SETTING = Setting
            .intSetting("max.percent.outoforder.errors", 25, Property.NodeScope);

    private final String jobId;
    private final UsageReporter usageReporter;
    private final JobDataCountsPersister dataCountsPersister;
    private final Logger logger;

    private final DataCounts totalRecordStats;
    private volatile DataCounts incrementalRecordStats;

    private long analyzedFieldsPerRecord = 1;

    private long recordCountDivisor = 100;
    private long lastRecordCountQuotient = 0;
    private long logEvery = 1;
    private long logCount = 0;

    private final int acceptablePercentDateParseErrors;
    private final int acceptablePercentOutOfOrderErrors;

    private final AtomicLong lastRecordTimeEpochMs;

    public StatusReporter(Environment env, Settings settings, String jobId, UsageReporter usageReporter,
            JobDataCountsPersister dataCountsPersister, Logger logger, long bucketSpan) {
        this(env, settings, jobId, usageReporter, dataCountsPersister, logger, new DataCounts(), bucketSpan);
    }

    public StatusReporter(Environment env, Settings settings, String jobId, DataCounts counts, UsageReporter usageReporter,
            JobDataCountsPersister dataCountsPersister, Logger logger, long bucketSpan) {
        this(env, settings, jobId, usageReporter, dataCountsPersister, logger, new DataCounts(counts), bucketSpan);
    }

    private StatusReporter(Environment env, Settings settings, String jobId, UsageReporter usageReporter,
            JobDataCountsPersister dataCountsPersister, Logger logger, DataCounts totalCounts, long bucketSpan) {
        this.jobId = jobId;
        this.usageReporter = usageReporter;
        this.dataCountsPersister = dataCountsPersister;
        this.logger = logger;

        totalRecordStats = totalCounts;
        incrementalRecordStats = new DataCounts();

        lastRecordTimeEpochMs = new AtomicLong();

        acceptablePercentDateParseErrors = ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_SETTING.get(settings);
        acceptablePercentOutOfOrderErrors = ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_SETTING.get(settings);
    }

    /**
     * Increment the number of records written by 1 and increment
     * the total number of fields read.
     *
     * @param inputFieldCount    Number of fields in the record.
     *                           Note this is not the number of processed fields (by field etc)
     *                           but the actual number of fields in the record
     * @param latestRecordTimeMs The time of the latest record written
     *                           in milliseconds from the epoch.
     */
    public void reportRecordWritten(long inputFieldCount, long latestRecordTimeMs)
            throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException {
        usageReporter.addFieldsRecordsRead(inputFieldCount);

        // Only a single thread updates this value so there isn't a
        // race condidtion using lazySet. Changes are visible to get()
        lastRecordTimeEpochMs.lazySet(latestRecordTimeMs);

        Date latestDate = new Date(latestRecordTimeMs);

        totalRecordStats.incrementInputFieldCount(inputFieldCount);
        totalRecordStats.incrementProcessedRecordCount(1);
        totalRecordStats.setLatestRecordTimeStamp(latestDate);

        incrementalRecordStats.incrementInputFieldCount(inputFieldCount);
        incrementalRecordStats.incrementProcessedRecordCount(1);
        incrementalRecordStats.setLatestRecordTimeStamp(latestDate);

        // report at various boundaries
        long totalRecords = getInputRecordCount();
        if (isReportingBoundary(totalRecords)) {
            logStatus(totalRecords);

            dataCountsPersister.persistDataCounts(jobId, runningTotalStats());
            try {
                checkStatus(totalRecords);
            } catch (HighProportionOfBadTimestampsException | OutOfOrderRecordsException e) {
                // report usage and re-throw
                usageReporter.reportUsage();
                throw e;
            }
        }
    }

    /**
     * Update only the incremental stats with the newest record time
     *
     * @param latestRecordTimeMs latest record time as epoch millis
     */
    public void reportLatestTimeIncrementalStats(long latestRecordTimeMs) {
        incrementalRecordStats.setLatestRecordTimeStamp(new Date(latestRecordTimeMs));
    }

    /**
     * Increment the excluded record count by 1 and the input field
     * count by <code>inputFieldCount</code>
     */
    public void reportExcludedRecord(long inputFieldCount) {
        totalRecordStats.incrementExcludedRecordCount(1);
        totalRecordStats.incrementInputFieldCount(inputFieldCount);

        incrementalRecordStats.incrementExcludedRecordCount(1);
        incrementalRecordStats.incrementInputFieldCount(inputFieldCount);
    }

    /**
     * Increments the date parse error count
     */
    public void reportDateParseError(long inputFieldCount) {
        totalRecordStats.incrementInvalidDateCount(1);
        totalRecordStats.incrementInputFieldCount(inputFieldCount);

        incrementalRecordStats.incrementInvalidDateCount(1);
        incrementalRecordStats.incrementInputFieldCount(inputFieldCount);

        usageReporter.addFieldsRecordsRead(inputFieldCount);
    }

    /**
     * Increments the missing field count
     * Records with missing fields are still processed
     */
    public void reportMissingField() {
        totalRecordStats.incrementMissingFieldCount(1);
        incrementalRecordStats.incrementMissingFieldCount(1);
    }

    /**
     * Increments by 1 the failed transform count
     */
    public void reportFailedTransform() {
        totalRecordStats.incrementFailedTransformCount(1);
        incrementalRecordStats.incrementFailedTransformCount(1);
    }

    public void reportMissingFields(long missingCount) {
        totalRecordStats.incrementMissingFieldCount(missingCount);
        incrementalRecordStats.incrementMissingFieldCount(missingCount);
    }

    /**
     * Add <code>newBytes</code> to the total volume processed
     */
    public void reportBytesRead(long newBytes) {
        totalRecordStats.incrementInputBytes(newBytes);
        incrementalRecordStats.incrementInputBytes(newBytes);
        usageReporter.addBytesRead(newBytes);
    }

    /**
     * Increments the out of order record count
     */
    public void reportOutOfOrderRecord(long inputFieldCount) {
        totalRecordStats.incrementOutOfOrderTimeStampCount(1);
        totalRecordStats.incrementInputFieldCount(inputFieldCount);

        incrementalRecordStats.incrementOutOfOrderTimeStampCount(1);
        incrementalRecordStats.incrementInputFieldCount(inputFieldCount);

        usageReporter.addFieldsRecordsRead(inputFieldCount);
    }

    /**
     * Total records seen = records written to the Engine (processed record
     * count) + date parse error records count + out of order record count.
     *
     * Records with missing fields are counted as they are still written.
     */
    public long getInputRecordCount() {
        return totalRecordStats.getInputRecordCount();
    }

    public long getProcessedRecordCount() {
        return totalRecordStats.getProcessedRecordCount();
    }

    public long getDateParseErrorsCount() {
        return totalRecordStats.getInvalidDateCount();
    }

    public long getMissingFieldErrorCount() {
        return totalRecordStats.getMissingFieldCount();
    }

    public long getOutOfOrderRecordCount() {
        return totalRecordStats.getOutOfOrderTimeStampCount();
    }

    public long getBytesRead() {
        return totalRecordStats.getInputBytes();
    }

    public long getFailedTransformCount() {
        return totalRecordStats.getFailedTransformCount();
    }

    public Date getLatestRecordTime() {
        return totalRecordStats.getLatestRecordTimeStamp();
    }

    public long getProcessedFieldCount() {
        totalRecordStats.calcProcessedFieldCount(getAnalysedFieldsPerRecord());
        return totalRecordStats.getProcessedFieldCount();
    }

    public long getInputFieldCount() {
        return totalRecordStats.getInputFieldCount();
    }

    public long getExcludedRecordCount() {
        return totalRecordStats.getExcludedRecordCount();
    }

    public int getAcceptablePercentDateParseErrors() {
        return acceptablePercentDateParseErrors;
    }

    public int getAcceptablePercentOutOfOrderErrors() {
        return acceptablePercentOutOfOrderErrors;
    }

    public void setAnalysedFieldsPerRecord(long value) {
        analyzedFieldsPerRecord = value;
    }

    public long getAnalysedFieldsPerRecord() {
        return analyzedFieldsPerRecord;
    }


    /**
     * Report the the status now regardless of whether or
     * not we are at a reporting boundary.
     */
    public void finishReporting()
            throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException {
        usageReporter.reportUsage();

        long totalRecords = getInputRecordCount();
        dataCountsPersister.persistDataCounts(jobId, runningTotalStats());

        if (totalRecords > 0) // because of a divide by zero error
        {
            checkStatus(totalRecords);
        }
    }

    /**
     * Log the status.  This is done progressively less frequently as the job
     * processes more data.  Logging every 10000 records when the data rate is
     * 40000 per second quickly rolls the logs.
     */
    private void logStatus(long totalRecords) {
        if (++logCount % logEvery != 0) {
            return;
        }

        String status = String.format(Locale.ROOT,
                "%d records written to autodetect; missingFieldCount=%d, "
                        + "invalidDateCount=%d, outOfOrderCount=%d, failedTransformCount=%d",
                        getProcessedRecordCount(), getMissingFieldErrorCount(), getDateParseErrorsCount(),
                        getOutOfOrderRecordCount(), getFailedTransformCount());

        logger.info(status);

        int log10TotalRecords = (int) Math.floor(Math.log10(totalRecords));
        // Start reducing the logging rate after 10 million records have been seen
        if (log10TotalRecords > 6) {
            logEvery = (int) Math.pow(10.0, log10TotalRecords - 6);
            logCount = 0;
        }
    }

    /**
     * Don't update status for every update instead update on these
     * boundaries
     * <ol>
     * <li>For the first 1000 records update every 100</li>
     * <li>After 1000 records update every 1000</li>
     * <li>After 20000 records update every 10000</li>
     * </ol>
     */
    private boolean isReportingBoundary(long totalRecords) {
        // after 20,000 records update every 10,000
        int divisor = 10000;

        if (totalRecords <= 1000) {
            // for the first 1000 records update every 100
            divisor = 100;
        } else if (totalRecords <= 20000) {
            // before 20,000 records update every 1000
            divisor = 1000;
        }

        if (divisor != recordCountDivisor) {
            // have crossed one of the reporting bands
            recordCountDivisor = divisor;
            lastRecordCountQuotient = totalRecords / divisor;

            return false;
        }

        long quotient = totalRecords / divisor;
        if (quotient > lastRecordCountQuotient) {
            lastRecordCountQuotient = quotient;
            return true;
        }

        return false;
    }

    /**
     * Throws an exception if too high a proportion of the records contains
     * errors (bad dates, out of order). See
     * {@linkplain #ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_SETTING} and
     * {@linkplain #ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_SETTING}
     */
    protected void checkStatus(long totalRecords)
            throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException {
        long percentBadDate = (getDateParseErrorsCount() * 100) / totalRecords;
        if (percentBadDate > getAcceptablePercentDateParseErrors()) {
            throw new HighProportionOfBadTimestampsException(
                    getDateParseErrorsCount(), totalRecords);
        }

        long percentOutOfOrder = (getOutOfOrderRecordCount() * 100) / totalRecords;
        if (percentOutOfOrder > getAcceptablePercentOutOfOrderErrors()) {
            throw new OutOfOrderRecordsException(
                    getOutOfOrderRecordCount(), totalRecords);
        }
    }


    public void startNewIncrementalCount() {
        incrementalRecordStats = new DataCounts();
    }

    public DataCounts incrementalStats() {
        incrementalRecordStats.calcProcessedFieldCount(getAnalysedFieldsPerRecord());
        return incrementalRecordStats;
    }

    public DataCounts runningTotalStats() {
        totalRecordStats.calcProcessedFieldCount(getAnalysedFieldsPerRecord());
        return totalRecordStats;
    }
}
