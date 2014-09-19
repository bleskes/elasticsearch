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

package com.prelert.job.usage;

import org.apache.log4j.Logger;

/**
 * Reports the number of bytes read.
 * This is an abstract class sub classes should implement
 * {@link UsageReporter#persistUsageCounts()} 
 * 
 * The main difference betweeen this and the {@linkplain com.prelert.job.warnings.StatusReporter}
 * is that this writes hourly reports i.e. how much data was read in an hour
 */
abstract public class UsageReporter 
{
	static final public String UPDATE_INTERVAL_PROP = "usage.update.interval";
	static final private long UPDATE_AFTER_COUNT_SECS = 300;
	
	private String m_JobId;
	private Logger m_Logger;
	
	private long m_BytesReadSinceLastReport;
	private long m_FieldsReadSinceLastReport;
	private long m_RecordsReadSinceLastReport;

	private long m_LastUpdateTimeMs;
	private long m_UpdateIntervalMs = UPDATE_AFTER_COUNT_SECS * 1000;
	
	public UsageReporter(String jobId, Logger logger)
	{
		m_BytesReadSinceLastReport = 0;
		m_FieldsReadSinceLastReport = 0;
		m_RecordsReadSinceLastReport = 0;
		
		m_JobId = jobId;
		m_Logger = logger;
		
		m_LastUpdateTimeMs = System.currentTimeMillis();
		
		String interval = System.getProperty(UPDATE_INTERVAL_PROP);
		if (interval != null)
		{
			try
			{
				m_UpdateIntervalMs = Long.parseLong(interval) * 1000;
				m_Logger.info("Setting usage update interval to " + interval + " seconds");				
			}
			catch (NumberFormatException e)
			{
				m_Logger.warn("Cannot parse '" + UPDATE_INTERVAL_PROP + 
						"' property = " + interval, e);
			}
		}
	}
	
	/**
	 * Add <code>bytesRead</code> to the running total
	 * @param bytesRead
	 */
	public void addBytesRead(long bytesRead)
	{
		m_BytesReadSinceLastReport += bytesRead;
		
		long now = System.currentTimeMillis();
		
		if (now - m_LastUpdateTimeMs > m_UpdateIntervalMs)
		{
			reportUsage(now);
		}
	}
	
	public void addFieldsRecordsRead(long fieldsRead)
	{
		m_FieldsReadSinceLastReport += fieldsRead;
		++m_RecordsReadSinceLastReport;
	}
	
	public void addFieldsRecordsRead(long fieldsRead, long recordsRead)
	{
		m_FieldsReadSinceLastReport += fieldsRead;
		m_RecordsReadSinceLastReport += recordsRead;
	}
	
	public long getBytesReadSinceLastReport()
	{
		return m_BytesReadSinceLastReport;
	}
	
	public long getFieldsReadSinceLastReport()
	{
		return m_FieldsReadSinceLastReport;
	}
	
	public long getRecordsReadSinceLastReport()
	{
		return m_RecordsReadSinceLastReport;
	}
	
	
	public String getJobId()
	{
		return m_JobId;
	}
	
	public Logger getLogger()
	{
		return m_Logger;
	}
	
	/**
	 * Logs total bytes written and calls {@linkplain persistUsageCounts()}
	 * m_BytesReadSinceLastReport, m_FieldsReadSinceLastReport and
	 * m_RecordsReadSinceLastReport are reset to 0 after this has been called.
	 */
	public void reportUsage()
	{	
		reportUsage(System.currentTimeMillis());
	}
		
	/**
	 * See {@linkplain #reportUsage()}
	 * 
	 * @param epoch_ms The time now - saved as the last update time
	 */
	private void reportUsage(long epoch_ms)
	{
		m_Logger.info(String.format("An additional %dKiB, %d fields and %d records read by job %s",
				m_BytesReadSinceLastReport >> 10, m_FieldsReadSinceLastReport, 
				m_RecordsReadSinceLastReport, m_JobId));

		persistUsageCounts();

		m_LastUpdateTimeMs = epoch_ms;
		
		m_BytesReadSinceLastReport = 0;
		m_FieldsReadSinceLastReport = 0;
		m_RecordsReadSinceLastReport = 0;
	}
	
	/**
	 * Persist the usage counts
	 * @return
	 */
	abstract protected boolean persistUsageCounts();

}
