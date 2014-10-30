/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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
 ***********************************************************/

package com.prelert.data;

import java.io.Serializable;
import java.util.Date;


/**
 * Status for a process with processname and an optional 
 * status message and the timestamp of that message if
 * it exists.
 */
public class ProcessStatus implements Serializable 
{
	private static final long serialVersionUID = 5219342427207140272L;

	/**
	 * Enum representing the possible status of a 
	 * Prelert process.
	 */
	public enum ProcessRunStatus 
	{
		STARTING, RUNNING, STOPPING, STOPPED, PAUSED, ERROR, FATAL_ERROR, UNKNOWN
	}
	
	
	private String m_ProcessName;
	private ProcessRunStatus m_Status;
	private String m_Message;
	private Date m_TimeStamp;
	
	/**
	 * Default constructed has UNKNOWN status.
	 */
	public ProcessStatus()
	{
		m_ProcessName = "";
		m_Message = "";
		m_Status = ProcessRunStatus.UNKNOWN;
		m_TimeStamp = new Date(0L);
	}
	
	
	/**
	 * Returns the Process name
	 * @return
	 */
	public String getProcessName()
	{
		return m_ProcessName;
	}
	
	public void setProcessName(String name)
	{
		m_ProcessName = name;
	}
	
	
	/**
	 * Process status.
	 * @return
	 */
	public ProcessRunStatus getStatus() 
	{
		return m_Status;
	}

	public void setStatus(ProcessRunStatus status) 
	{
		m_Status = status;
	}


	/**
	 * Optional message associated with the Process status.
	 * @return
	 */
	public String getMessage() 
	{
		return m_Message;
	}

	public void setMessage(String message) 
	{
		m_Message = message;
	}
	
	
	/**
	 * The time stamp of the last message from the process.
	 * @return
	 */
	public Date getTimeStamp()
	{
		return m_TimeStamp;
	}
	
	public void setTimeStamp(Date timestamp)
	{
		m_TimeStamp = timestamp;
	}
	
	@Override
	public String toString()
	{
		StringBuilder strRep = new StringBuilder();
		strRep.append('{');
		strRep.append("ProcessName = " + m_ProcessName + "; ");
		strRep.append("Status = " + m_Status + "; ");
		strRep.append("Message = " + m_Message + "; ");
		strRep.append("Timestamp = " + m_TimeStamp + "; ");
		strRep.append('}');
		
		return strRep.toString();
	}
	
}
