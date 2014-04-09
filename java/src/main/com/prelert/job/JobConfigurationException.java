
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

/**
 * Represents the invalid configuration of a job.
 */
public class JobConfigurationException extends Exception 
{
	private static final long serialVersionUID = -563428978300447381L;

	
	private int m_ErrorCode;
	
	/**
	 * Create a new JobConfigurationException.
	 * 
	 * @param message Details of error explaining the context 
	 * @param errorCode See {@linkplain com.prelert.rs.data.ErrorCodes}
	 */
	public JobConfigurationException(String message, int errorCode)
	{
		super(message);
		m_ErrorCode = errorCode;
	}
	
	public JobConfigurationException(String message, int errorCode, Throwable cause)
	{
		super(message, cause);
	}
	
	
	public int getErrorCode()
	{
		return m_ErrorCode;
	}

}