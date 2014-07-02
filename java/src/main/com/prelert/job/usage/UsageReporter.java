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
 */
abstract public class UsageReporter 
{
	static private long UPDATE_AFTER_COUNT_BYTES = 2097152; // 2MB
	private long m_BytesReadSinceLastReport;
	private String m_JobId;
	private Logger m_Logger;
	private long m_TotalByteCount;
	
	public UsageReporter(String jobId, Logger logger)
	{
		m_BytesReadSinceLastReport = 0;
		m_JobId = jobId;
		m_Logger = logger;
		m_TotalByteCount = 0;
	}
	
	/**
	 * Add <code>bytesRead</code> to the running total
	 * @param bytesRead
	 */
	public void addBytesRead(int bytesRead)
	{
		m_BytesReadSinceLastReport += bytesRead;
		
		if (m_BytesReadSinceLastReport > UPDATE_AFTER_COUNT_BYTES)
		{
			reportUsage();
		}
	}
	
	public long getBytesRead()
	{
		return m_BytesReadSinceLastReport;
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
	 * m_BytesReadSinceLastReport is reset to 0 after this has been called.
	 * 
	 */
	public void reportUsage()
	{
		m_TotalByteCount += m_BytesReadSinceLastReport;

		m_Logger.info(String.format("%dkB written to job %s",
				m_TotalByteCount >> 10, m_JobId));
		
		persistUsageCounts();
		
		m_BytesReadSinceLastReport = 0;
	}
		
	/**
	 * Persist the usage counts
	 * @return
	 */
	abstract protected boolean persistUsageCounts();

}
