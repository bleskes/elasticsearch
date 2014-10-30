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
 * This type of exception represents an error where an operation
 * would result in too many jobs running at the same time.
 */
public class TooManyJobsException extends Exception
{
	private static final long serialVersionUID = 8503362038035845948L;

	private int m_Limit;
	private ErrorCode m_ErrorCode;


	/**
	 * Create a new TooManyJobsException with an error code
	 *
	 * @param limit The limit on the number of jobs
	 * @param message Details of error explaining the context
	 * @param errorCode
	 */
	public TooManyJobsException(int limit, String message, ErrorCode errorCode)
	{
		super(message);
		m_Limit = limit;
		m_ErrorCode = errorCode;
	}


	public TooManyJobsException(int limit, String message, ErrorCode errorCode,
			Throwable cause)
	{
		super(message, cause);
		m_Limit = limit;
		m_ErrorCode = errorCode;
	}


	/**
	 * Get the limit on the number of concurrently running jobs.
	 * @return
	 */
	public int getLimit()
	{
		return m_Limit;
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
