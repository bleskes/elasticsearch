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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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
package com.prelert.job.warnings;

import org.apache.log4j.Logger;

/**
 * Abstract status reporter for tracking all the good/bad
 * records written to the API. Call one of the reportXXX() methods
 * to update the records counts if {@linkplain #isReportingBoundary(int)}
 * returns true then the count will be reported via the abstract 
 * {@linkplain #reportStatus(int)} method. If there is a high proportion
 * of errors the {@linkplain StatusReporter#checkStatus(int)} method 
 * throws an error.
 */
abstract public class StatusReporter 
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
	
	private int m_RecordsWritten = 0;
	private int m_DateParseErrorsCount = 0;
	private int m_MissingFieldErrorCount = 0;
	private int m_OutOfOrderRecordCount = 0;
	
	private int m_RecordCountDivisor = 100;
	private int m_LastRecordCountQuotient = 0;
	
	private int m_AcceptablePercentDateParseErrors;
	private int m_AcceptablePercentOutOfOrderErrors;
	
	protected String m_JobId;
	protected Logger m_Logger;
	
	public StatusReporter(String jobId, Logger logger)
	{
		m_JobId = jobId;
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
	
	/**
	 * Add <code>recordsWritten</code> to the running total
	 * and report status if at a status reporting boundary.
	 * 
	 * @param recordsWritten
	 * @throws HighProportionOfBadTimestampsException
	 * @throws OutOfOrderRecordsException
	 */
	public void reportRecordsWritten(int recordsWritten)
	throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException 
	{
		m_RecordsWritten += recordsWritten;
		
		// report at various boundaries
		int totalRecords = sumTotalRecords();
		if (isReportingBoundary(totalRecords))
		{
			reportStatus(totalRecords);
			checkStatus(totalRecords);
		}
	}
	
	/**
	 * Increment the number of records written by 1.
	 * 
	 * @throws HighProportionOfBadTimestampsException
	 * @throws OutOfOrderRecordsException
	 */
	public void reportRecordWritten()
	throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException 
	{
		reportRecordsWritten(1);
	}
	
	
	/**
	 * Increments the date parse error count
	 */
	public void reportDateParseError(String date)
	{
		m_DateParseErrorsCount++;
	}

	/**
	 * Increments the missing field count
	 */
	public void reportMissingField(String field) 
	{
		m_MissingFieldErrorCount++;
	}

	/**
	 * Increments the out of order record count
	 */
	public void reportOutOfOrderRecord(long date, long previousDate)
	{
		m_OutOfOrderRecordCount++;
	}
	
	public int getRecordsWrittenCount() 
	{
		return m_RecordsWritten;
	}

	public int getDateParseErrorsCount() 
	{
		return m_DateParseErrorsCount;
	}

	public int getMissingFieldErrorCount() 
	{
		return m_MissingFieldErrorCount;
	}

	public int getOutOfOrderRecordCount() 
	{
		return m_OutOfOrderRecordCount;
	}
	
	/**
	 * Total records seen = records written + date parse error records count 
	 * + out of order record count.
	 * 
	 * Missing field records aren't counted as they are still written.
	 * 
	 * @return
	 */
	public int sumTotalRecords()
	{
		return m_RecordsWritten + m_DateParseErrorsCount + 
				+ m_OutOfOrderRecordCount;
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
		int totalRecords = sumTotalRecords();
		reportStatus(totalRecords);
		checkStatus(totalRecords);
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
	private boolean isReportingBoundary(int totalRecords)
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
		
		int quotient = totalRecords / divisor;
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
	private void checkStatus(int totalRecords)
	throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
	{
		int percentBadDate = (getDateParseErrorsCount() * 100) / totalRecords;
		if (percentBadDate > getAcceptablePercentDateParseErrors())
		{
			throw new HighProportionOfBadTimestampsException(
					getDateParseErrorsCount(), totalRecords);
		}
		
		int percentOutOfOrder = (getOutOfOrderRecordCount() * 100) / totalRecords;
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
	abstract protected void reportStatus(int totalRecords);
}
