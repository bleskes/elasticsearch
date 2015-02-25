/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

import org.apache.log4j.Logger;

import com.prelert.job.JobDetails;
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

	private long m_RecordsWritten = 0;
	private long m_InputFieldCount = 0;
	private long m_BytesRead = 0;
	private long m_DateParseErrorsCount = 0;
	private long m_MissingFieldErrorCount = 0;
	private long m_OutOfOrderRecordCount = 0;
	private long m_FailedTransformCount = 0;

	private long m_AnalyzedFieldsPerRecord = 1;

	private long m_RecordCountDivisor = 100;
	private long m_LastRecordCountQuotient = 0;

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

	public StatusReporter(String jobId, JobDetails.Counts counts,
			UsageReporter usageReporter, Logger logger)
	{
		this(jobId, usageReporter, logger);

		m_RecordsWritten = counts.getProcessedRecordCount();
		m_BytesRead = counts.getInputBytes();
		m_DateParseErrorsCount = counts.getInvalidDateCount();
		m_MissingFieldErrorCount = counts.getMissingFieldCount();
		m_OutOfOrderRecordCount = counts.getOutOfOrderTimeStampCount();
		m_FailedTransformCount = counts.getFailedTransformCount();
	}

	/**
	 * Increment the number of records written by 1 and increment
	 * the total number of fields read.
	 *
	 * @param inputFieldCount Number of fields in the record.
	 * Note this is not the number of processed fields (by field etc)
	 * but the actual number of fields in the record
	 *
	 * @throws HighProportionOfBadTimestampsException
	 * @throws OutOfOrderRecordsException
	 */
	public void reportRecordWritten(long inputFieldCount)
	throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
	{
		m_UsageReporter.addFieldsRecordsRead(inputFieldCount);

		m_InputFieldCount += inputFieldCount;
		m_RecordsWritten += 1;

		// report at various boundaries
		long totalRecords = getInputRecordCount() ;
		if (isReportingBoundary(totalRecords))
		{
			reportStatus(totalRecords);
			checkStatus(totalRecords);
		}
	}


	/**
	 * Increments the date parse error count
	 */
	public void reportDateParseError(long inputFieldCount)
	{
		m_DateParseErrorsCount++;
		m_InputFieldCount += inputFieldCount;
		m_UsageReporter.addFieldsRecordsRead(inputFieldCount);
	}

	/**
	 * Increments the missing field count
	 * Records with missing fields are still processed
	 */
	public void reportMissingField()
	{
		m_MissingFieldErrorCount++;
	}

    /**
     * Increments by 1 the failed transform count
     */
    public void reportFailedTransform()
    {
        m_FailedTransformCount++;
    }

	public void reportMissingFields(long missingCount)
	{
		m_MissingFieldErrorCount += missingCount;
	}

	/**
	 * Add <code>newBytes</code> to the total volume processed
	 * @param newBytes
	 */
	public void reportBytesRead(long newBytes)
	{
		m_BytesRead += newBytes;
		m_UsageReporter.addBytesRead(newBytes);
	}

	/**
	 * Increments the out of order record count
	 */
	public void reportOutOfOrderRecord(long inputFieldCount)
	{
		m_OutOfOrderRecordCount++;
		m_InputFieldCount += inputFieldCount;
		m_UsageReporter.addFieldsRecordsRead(inputFieldCount);
	}

	/**
	 * Total records seen = records written + date parse error records count
	 * + out of order record count.
	 *
	 * Missing field records aren't counted as they are still written.
	 *
	 */
	public long getInputRecordCount()
	{
		return m_RecordsWritten + m_OutOfOrderRecordCount + m_DateParseErrorsCount;
	}

	public long getProcessedRecordCount()
	{
		return m_RecordsWritten;
	}

	public long getDateParseErrorsCount()
	{
		return m_DateParseErrorsCount;
	}

	public long getMissingFieldErrorCount()
	{
		return m_MissingFieldErrorCount;
	}

	public long getOutOfOrderRecordCount()
	{
		return m_OutOfOrderRecordCount;
	}

	public long getBytesRead()
	{
		return m_BytesRead;
	}

	public long getFailedTransformCount()
	{
	    return m_FailedTransformCount;
	}

	public long getProcessedFieldCount()
	{
		long processedFieldCount =
				(getProcessedRecordCount() * getAnalysedFieldsPerRecord())
				- getMissingFieldErrorCount();

		// processedFieldCount could be a -ve value if no
		// records have been written in which case it should be 0
		processedFieldCount = (processedFieldCount < 0) ? 0 : processedFieldCount;

		return processedFieldCount;
	}

	public long getInputFieldCount()
	{
		return m_InputFieldCount;
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

	/**
	 * Report the counts and stats for the records.
	 * How the stats are reported is decided by the implementing class.
	 *
	 * @param totalRecords
	 */
	protected abstract void reportStatus(long totalRecords);
}
