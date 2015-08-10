/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/
package com.prelert.job.status;

import java.math.RoundingMode;
import java.util.Date;

import org.apache.log4j.Logger;

import com.google.common.math.LongMath;

import com.prelert.job.DataCounts;
import com.prelert.job.usage.UsageReporter;


/**
 * Abstract status reporter for tracking all the good/bad
 * records written to the API. Call one of the reportXXX() methods
 * to update the records counts if {@linkplain #isReportingBoundary(int)}
 * returns true then the count will be reported via the abstract
 * {@linkplain #reportStatus(int)} method. If there is a high proportion
 * of errors the {@linkplain StatusReporter#checkStatus(int)} method
 * throws an error.
 */
public abstract class StatusReporter
{
    /**
     * The max percentage of date parse errors allowed before
     * an exception is thrown.
     */
    public static final int ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS = 25;
    public static final String ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP =
            "max.percent.date.errors";

    /**
     * The max percentage of out of order records allowed before
     * an exception is thrown.
     */
    public static final int ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS = 25;
    public static final String ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP =
            "max.percent.outoforder.errors";

    private static final int SECONDS_TO_MS = 1000;

    private DataCounts m_TotalRecordStats;
    private DataCounts m_IncrementalRecordStats;


    private long m_AnalyzedFieldsPerRecord = 1;

    private long m_RecordCountDivisor = 100;
    private long m_LastRecordCountQuotient = 0;
    private long m_LogEvery = 1;
    private long m_LogCount = 0;

    private int m_AcceptablePercentDateParseErrors;
    private int m_AcceptablePercentOutOfOrderErrors;

    private UsageReporter m_UsageReporter;

    protected String m_JobId;
    protected Logger m_Logger;


    protected StatusReporter(String jobId, UsageReporter usageReporter, Logger logger)
    {
        m_JobId = jobId;
        m_UsageReporter = usageReporter;
        m_Logger = logger;

        m_TotalRecordStats = new DataCounts();
        m_IncrementalRecordStats = new DataCounts();

        m_AcceptablePercentDateParseErrors = ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS;

        String prop = System.getProperty(ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP);
        try
        {
            m_AcceptablePercentDateParseErrors = Integer.parseInt(prop);
        }
        catch (NumberFormatException e)
        {
        }

        m_AcceptablePercentOutOfOrderErrors = ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS;

        prop = System.getProperty(ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP);
        try
        {
            m_AcceptablePercentOutOfOrderErrors = Integer.parseInt(prop);
        }
        catch (NumberFormatException e)
        {
        }
    }

    public StatusReporter(String jobId, DataCounts counts,
            UsageReporter usageReporter, Logger logger)
    {
        this(jobId, usageReporter, logger);

        m_TotalRecordStats = new DataCounts(counts);
    }

    /**
     * Increment the number of records written by 1 and increment
     * the total number of fields read.
     *
     * @param inputFieldCount Number of fields in the record.
     * Note this is not the number of processed fields (by field etc)
     * but the actual number of fields in the record
     * @param latestRecordTime The time of the latest record written
     * in seconds from the epoch.
     *
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     */
    public void reportRecordWritten(long inputFieldCount, long latestRecordTime)
    throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        m_UsageReporter.addFieldsRecordsRead(inputFieldCount);

        Date latestDate = new Date(latestRecordTime * SECONDS_TO_MS);

        m_TotalRecordStats.incrementInputFieldCount(inputFieldCount);
        m_TotalRecordStats.incrementProcessedRecordCount(1);
        m_TotalRecordStats.setLatestRecordTimeStamp(latestDate);

        m_IncrementalRecordStats.incrementInputFieldCount(inputFieldCount);
        m_IncrementalRecordStats.incrementProcessedRecordCount(1);
        m_IncrementalRecordStats.setLatestRecordTimeStamp(latestDate);

        // report at various boundaries
        long totalRecords = getInputRecordCount() ;
        if (isReportingBoundary(totalRecords))
        {
            logStatus(totalRecords);
            reportStatus(totalRecords);
            try
            {
                checkStatus(totalRecords);
            }
            catch (HighProportionOfBadTimestampsException | OutOfOrderRecordsException e)
            {
                // report usage and re-throw
                m_UsageReporter.reportUsage();
                throw e;
            }
        }
    }

    /**
     * Update only the incremental stats with the newest record time
     * @param latestRecordTime
     */
    public void reportLatestTimeIncrementalStats(long latestRecordTime)
    {
        m_IncrementalRecordStats.setLatestRecordTimeStamp(new Date(latestRecordTime * SECONDS_TO_MS));
    }

    /**
     * Increment the excluded record count by 1 and the input field
     * count by <code>inputFieldCount</code>
     * @param inputFieldCount
     */
    public void reportExcludedRecord(long inputFieldCount)
    {
        m_TotalRecordStats.incrementExcludedRecordCount(1);
        m_TotalRecordStats.incrementInputFieldCount(inputFieldCount);

        m_IncrementalRecordStats.incrementExcludedRecordCount(1);
        m_IncrementalRecordStats.incrementInputFieldCount(inputFieldCount);
    }

    /**
     * Increments the date parse error count
     */
    public void reportDateParseError(long inputFieldCount)
    {
        m_TotalRecordStats.incrementInvalidDateCount(1);
        m_TotalRecordStats.incrementInputFieldCount(inputFieldCount);

        m_IncrementalRecordStats.incrementInvalidDateCount(1);
        m_IncrementalRecordStats.incrementInputFieldCount(inputFieldCount);

        m_UsageReporter.addFieldsRecordsRead(inputFieldCount);
    }

    /**
     * Increments the missing field count
     * Records with missing fields are still processed
     */
    public void reportMissingField()
    {
        m_TotalRecordStats.incrementMissingFieldCount(1);
        m_IncrementalRecordStats.incrementMissingFieldCount(1);
    }

    /**
     * Increments by 1 the failed transform count
     */
    public void reportFailedTransform()
    {
        m_TotalRecordStats.incrementFailedTransformCount(1);
        m_IncrementalRecordStats.incrementFailedTransformCount(1);
    }

    public void reportMissingFields(long missingCount)
    {
        m_TotalRecordStats.incrementMissingFieldCount(missingCount);
        m_IncrementalRecordStats.incrementMissingFieldCount(missingCount);
    }

    /**
     * Add <code>newBytes</code> to the total volume processed
     * @param newBytes
     */
    public void reportBytesRead(long newBytes)
    {
        m_TotalRecordStats.incrementInputBytes(newBytes);
        m_IncrementalRecordStats.incrementInputBytes(newBytes);
        m_UsageReporter.addBytesRead(newBytes);
    }

    /**
     * Increments the out of order record count
     * @param
     */
    public void reportOutOfOrderRecord(long inputFieldCount)
    {
        m_TotalRecordStats.incrementOutOfOrderTimeStampCount(1);
        m_TotalRecordStats.incrementInputFieldCount(inputFieldCount);

        m_IncrementalRecordStats.incrementOutOfOrderTimeStampCount(1);
        m_IncrementalRecordStats.incrementInputFieldCount(inputFieldCount);

        m_UsageReporter.addFieldsRecordsRead(inputFieldCount);
    }

    /**
     * Total records seen = records written to the Engine (processed
     * record count) + date parse error records count + out of order record count.
     *
     * Records with missing fields are counted as they are still written.
     */
    public long getInputRecordCount()
    {
        return m_TotalRecordStats.getInputRecordCount();
    }

    public long getProcessedRecordCount()
    {
        return m_TotalRecordStats.getProcessedRecordCount();
    }

    public long getDateParseErrorsCount()
    {
        return m_TotalRecordStats.getInvalidDateCount();
    }

    public long getMissingFieldErrorCount()
    {
        return m_TotalRecordStats.getMissingFieldCount();
    }

    public long getOutOfOrderRecordCount()
    {
        return m_TotalRecordStats.getOutOfOrderTimeStampCount();
    }

    public long getBytesRead()
    {
        return m_TotalRecordStats.getInputBytes();
    }

    public long getFailedTransformCount()
    {
        return m_TotalRecordStats.getFailedTransformCount();
    }

    public Date getLatestRecordTime()
    {
        return m_TotalRecordStats.getLatestRecordTimeStamp();
    }

    public long getProcessedFieldCount()
    {
        m_TotalRecordStats.calcProcessedFieldCount(getAnalysedFieldsPerRecord());
        return m_TotalRecordStats.getProcessedFieldCount();
    }

    public long getInputFieldCount()
    {
        return m_TotalRecordStats.getInputFieldCount();
    }

    public long getExcludedRecordCount()
    {
        return m_TotalRecordStats.getExcludedRecordCount();
    }

    public int getAcceptablePercentDateParseErrors()
    {
        return m_AcceptablePercentDateParseErrors;
    }

    public void setAcceptablePercentDateParseErrors(int value)
    {
        m_AcceptablePercentDateParseErrors = value;
    }


    public int getAcceptablePercentOutOfOrderErrors()
    {
        return m_AcceptablePercentOutOfOrderErrors;
    }

    public void setAcceptablePercentOutOfOrderErrors(int value)
    {
        m_AcceptablePercentOutOfOrderErrors = value;
    }


    public void setAnalysedFieldsPerRecord(long value)
    {
        m_AnalyzedFieldsPerRecord = value;
    }

    public long getAnalysedFieldsPerRecord()
    {
        return m_AnalyzedFieldsPerRecord;
    }


    /**
     * Report the the status now regardless of whether or
     * not we are at a reporting boundary.
     *
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     */
    public void finishReporting()
    throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        m_UsageReporter.reportUsage();

        long totalRecords = getInputRecordCount();
        reportStatus(totalRecords);

        if (totalRecords > 0) // because of a divide by zero error
        {
            checkStatus(totalRecords);
        }
    }

    /**
     * Log the status.  This is done progressively less frequently as the job
     * processes more data.  Logging every 10000 records when the data rate is
     * 40000 per second quickly rolls the logs.
     * @param totalRecords
     */
    private void logStatus(long totalRecords)
    {
        if (m_Logger == null)
        {
            // This can happen in unit tests
            return;
        }

        if (++m_LogCount % m_LogEvery != 0)
        {
            return;
        }

        String status = String.format("%d records written to autodetect with %d "
                + "missing fields, %d were discarded because the date could not be "
                + "read and %d were ignored because they weren't in ascending "
                + "chronological order and %d transforms failed.", getProcessedRecordCount(),
                getMissingFieldErrorCount(), getDateParseErrorsCount(),
                getOutOfOrderRecordCount(), getFailedTransformCount());

        m_Logger.info(status);

        int log10TotalRecords = LongMath.log10(totalRecords, RoundingMode.DOWN);
        // Start reducing the logging rate after 10 million records have been seen
        if (log10TotalRecords > 6)
        {
            m_LogEvery = LongMath.pow(10L, log10TotalRecords - 6);
            m_LogCount = 0;
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
     *
     * @param totalRecords
     * @return
     */
    private boolean isReportingBoundary(long totalRecords)
    {
        // after 20,000 records update every 10,000
        int divisor = 10000;

        if (totalRecords <= 1000)
        {
            // for the first 1000 records update every 100
            divisor = 100;
        }
        else if (totalRecords <= 20000)
        {
            // before 20,000 records update every 1000
            divisor = 1000;
        }

        if (divisor != m_RecordCountDivisor)
        {
            // have crossed one of the reporting bands
            m_RecordCountDivisor = divisor;
            m_LastRecordCountQuotient = totalRecords / divisor;

            return false;
        }

        long quotient = totalRecords / divisor;
        if (quotient > m_LastRecordCountQuotient)
        {
            m_LastRecordCountQuotient = quotient;
            return true;
        }

        return false;
    }

    /**
     * Throws an exception if too high a proportion of the records
     * contains errors (bad dates, out of order). See
     * {@linkplain #ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS} and
     * {@linkplain #ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS}
     *
     * @param totalRecords
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     *
     */
    private void checkStatus(long totalRecords)
    throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        long percentBadDate = (getDateParseErrorsCount() * 100) / totalRecords;
        if (percentBadDate > getAcceptablePercentDateParseErrors())
        {
            throw new HighProportionOfBadTimestampsException(
                    getDateParseErrorsCount(), totalRecords);
        }

        long percentOutOfOrder = (getOutOfOrderRecordCount() * 100) / totalRecords;
        if (percentOutOfOrder > getAcceptablePercentOutOfOrderErrors())
        {
            throw new OutOfOrderRecordsException(
                    getOutOfOrderRecordCount(), totalRecords);
        }
    }


    public void startNewIncrementalCount()
    {
        m_IncrementalRecordStats = new DataCounts();
    }

    public DataCounts incrementalStats()
    {
        m_IncrementalRecordStats.calcProcessedFieldCount(getAnalysedFieldsPerRecord());
        return m_IncrementalRecordStats;
    }

    public DataCounts runningTotalStats()
    {
        m_TotalRecordStats.calcProcessedFieldCount(getAnalysedFieldsPerRecord());
        return m_TotalRecordStats;
    }

    /**
     * Report the counts and stats for the records.
     * How the stats are reported is decided by the implementing class.
     *
     * @param totalRecords
     */
    protected abstract void reportStatus(long totalRecords);
}
