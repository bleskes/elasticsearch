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
package com.prelert.job.process;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.job.DataDescription;

/**
 * The native process and its data description object.
 * {@link #isInUse()} is true if the processes std in is 
 * being written to. ErrorReader is a buffered reader \
 * connected to the Process's error output.
 * {@link #getLogger} returns a logger that logs to the
 * jobs log directory
 */
public class ProcessAndDataDescription 
{
	final private Process m_Process;
	final private DataDescription m_DataDescription; 
	volatile private boolean m_IsInUse;
	final private long m_TimeoutSeconds;
	private BufferedReader m_ErrorReader;	
	private List<String> m_InterestingFields;
	private Logger m_JobLogger;

	/**
	 * Object for grouping the native process, its data description
	 * and interesting fields and its timeout period.
	 * 
	 * @param process The native process.
	 * @param jobId
	 * @param dd
	 * @param timeout
	 * @param interestingFields The list of fields used in the analysis
	 * @param logger The job's logger
	 */
	public ProcessAndDataDescription(Process process, String jobId, 
			DataDescription dd,
			long timeout, List<String> interestingFields,
			Logger logger)
	{
		m_Process = process;
		m_DataDescription = dd;
		m_IsInUse = false;
		m_TimeoutSeconds = timeout;
		
		m_ErrorReader = new BufferedReader(
				new InputStreamReader(m_Process.getErrorStream()));		
		
		m_InterestingFields = interestingFields;
		
		m_JobLogger = logger;
	}

	public Process getProcess()
	{
		return m_Process;			
	}

	public DataDescription getDataDescription()
	{
		return m_DataDescription;
	}

	/**
	 * True if the process is currently in use
	 * Thread safe without synchronisation as this is a volatile field
	 * @return
	 */
	public boolean isInUse()
	{
		return m_IsInUse;
	}

	/**
	 * Set the process as in use
	 * Thread safe without synchronisation as this is a volatile field
	 * @param inUse
	 */
	public void setInUse(boolean inUse)
	{
		m_IsInUse = inUse;
	}

	/**
	 * The timeout value in seconds. 
	 * The process is stopped once this interval has expired 
	 * without any new data.
	 */
	public long getTimeout()
	{
		return m_TimeoutSeconds;
	}
	
	/**
	 * Get the reader attached to the process's error output.
	 * The reader is mutable and <b>not</b> thread safe.
	 * @return
	 */
	public BufferedReader getErrorReader()
	{
		return m_ErrorReader;
	}
	
	/**
	 * The list of fields required for the analysis. 
	 * The remaining fields can be filtered out.
	 * @return
	 */
	public List<String> getInterestingFields()
	{
		return m_InterestingFields;
	}
	
	
	public Logger getLogger()
	{
		return m_JobLogger;
	}
	
}
