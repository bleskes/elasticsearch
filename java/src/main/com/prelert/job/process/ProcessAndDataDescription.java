/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Inc 2006-2014     *
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

import com.prelert.job.DataDescription;

/**
 * The native process and its data description object.
 * {@link #isInUse()} is true if the processes std in is 
 * being written to. 
 */
public class ProcessAndDataDescription 
{

	final private Process m_Process;
	final private DataDescription m_DataDescription; 
	volatile private boolean m_IsInUse;
	final private long m_TimeoutSeconds;
	
	final private BufferedReader m_ErrorReader;

	public ProcessAndDataDescription(Process process, DataDescription dd,
			long timeout)
	{
		m_Process = process;
		m_DataDescription = dd;
		m_IsInUse = false;
		m_TimeoutSeconds = timeout;
		
		m_ErrorReader = new BufferedReader(
				new InputStreamReader(m_Process.getErrorStream()));		
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
}
