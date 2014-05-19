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

import com.prelert.rs.data.ErrorCode;


abstract public class StatusReporter 
{
	public static final int ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS = 25;
	public static final String ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP = 
			"max.percent.date.errors";
	
	public static final int ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS = 25;
	public static final String ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP = 
			"max.percent.outoforder.errors";	
	
	private int m_RecordsWritten = 0;
	private int m_RecordsDiscarded = 0;
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
	
	public void reportRecordsWritten(int recordsWritten, int recordsDiscarded)
	throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException 
	{
		m_RecordsWritten = recordsWritten;
		m_RecordsDiscarded = recordsDiscarded;
		
		// report at various boundaries
		int totalRecords = m_RecordsWritten + m_RecordsDiscarded;
		if (isReportingBoundary(totalRecords))
		{
			reportStatus(totalRecords);
			checkStatus(totalRecords);
		}
		
	}

	public void reportDateParseError(String date)
	throws HighProportionOfBadTimestampsException 
	{
		m_DateParseErrorsCount++;
	}

	public void reportMissingField(String field) 
	{
		m_MissingFieldErrorCount++;
	}


	public void reportOutOfOrderRecord(long date, long previousDate)
	throws OutOfOrderRecordsException 
	{
		m_OutOfOrderRecordCount++;
	}
	
	public int getRecordsWrittenCount() 
	{
		return m_RecordsWritten;
	}

	public int getRecordsDiscardedCount() 
	{
		return m_RecordsDiscarded;
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
		if (totalRecords > 20000)
		{
			// after 20000 records update every 10000
			m_RecordCountDivisor = 10000;
		}
		else if (totalRecords > 1000)
		{
			// after 1000 records update every 1000
			m_RecordCountDivisor = 1000;
		}
		else 
		{
			// for the first 1000 records update every 100
			m_RecordCountDivisor = 100;
		}
		
		int quotient = totalRecords / m_RecordCountDivisor;
		if (quotient > m_LastRecordCountQuotient)
		{
			m_LastRecordCountQuotient = quotient;
			return true;
		}
		
		return false;
	}
	
	private void checkStatus(int totalRecords)
	throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
	{
		int percentBadDate = (getDateParseErrorsCount() * 100) / totalRecords;
		if (percentBadDate > getAcceptablePercentDateParseErrors())
		{
			throw new HighProportionOfBadTimestampsException(
					getDateParseErrorsCount(),
					totalRecords, ErrorCode.TOO_MANY_BAD_DATES);
		}
		
		int percentOutOfOrder = (getOutOfOrderRecordCount() * 100) / totalRecords;
		if (percentOutOfOrder > getAcceptablePercentOutOfOrderErrors())
		{
			throw new OutOfOrderRecordsException(
					getOutOfOrderRecordCount(),
					totalRecords, ErrorCode.TOO_MANY_OUT_OF_ORDER_RECORDS);			
		}
	}
	
	abstract protected void reportStatus(int totalRecords);
}
