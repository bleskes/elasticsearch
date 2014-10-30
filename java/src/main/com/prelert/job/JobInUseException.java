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
package com.prelert.job;

import com.prelert.rs.data.ErrorCode;

/**
 * This exception is thrown is an operation is attempted on a job 
 * that can't be executed as the job is already being used.  
 */
public class JobInUseException extends Exception 
{
	private static final long serialVersionUID = -2759814168552580059L;
		
	private String m_JobId;
	private ErrorCode m_ErrorCode;
	
	/**
	 * Create a new JobInUseException.
	 * 
	 * @param jobId The Id of the job some operation was attempted on. 
	 * @param message Details of error explaining the context 
	 * @param The error code
	 * @see ErrorCode
	 */
	public JobInUseException(String jobId, String message, ErrorCode errorCode)
	{
		super(message);
		m_JobId = jobId;
		m_ErrorCode = errorCode;
	}
	
	/**
	 * Get the <i>JobId</i> that was the source of the error.
	 * @return The job id string
	 */
	public String getJobId()
	{
		return m_JobId;
	}
	
	public ErrorCode getErrorCode()
	{
		return m_ErrorCode;
	}
	
	@Override
	public String getMessage()
	{
		String msg = "JobId = " + m_JobId + ". "  + super.getMessage();
		return msg;
	}
}

