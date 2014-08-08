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
package com.prelert.job;

import com.prelert.rs.data.ErrorCode;

/**
 * This type of exception represents an error where
 * an operation uses a <i>JobId</i> that does not exist. 
 */
public class UnknownJobException extends Exception 
{
	private static final long serialVersionUID = 8603362038035845948L;

	private String m_JobId;
	private ErrorCode m_ErrorCode;
	
	/**
	 * Create with the default message and error code 
	 * set to ErrorCode.MISSING_JOB_ERROR
	 * @param jobId
	 */
	public UnknownJobException(String jobId)
	{
		super("No job with id '" + jobId + "'");
		m_JobId = jobId;
		m_ErrorCode = ErrorCode.MISSING_JOB_ERROR;
	}
	
	/**
	 * Create a new UnknownJobException with an error code
	 * 
	 * @param jobId The Job Id that could not be found
	 * @param message Details of error explaining the context 
	 * @param errorCode
	 */
	public UnknownJobException(String jobId, String message, ErrorCode errorCode)
	{
		super(message);
		m_JobId = jobId;
		m_ErrorCode = errorCode;
	}
	
	public UnknownJobException(String jobId, String message, ErrorCode errorCode,
			Throwable cause)
	{
		super(message, cause);
		m_JobId = jobId;
		m_ErrorCode = errorCode;
	}
	
	/**
	 * Get the unknown <i>JobId</i> that was the source of the error.
	 * @return
	 */
	public String getJobId()
	{
		return m_JobId;
	}
	
	/**
	 * Get the error code or 0 if not set.
	 * 
	 * @return
	 */
	public ErrorCode getErrorCode()
	{
		return m_ErrorCode;
	}
}
