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
package com.prelert.job;

/**
 * This type of exception represents an error where
 * an operation uses a <i>JobId</i> that does not exist. 
 */
public class UnknownJobException extends Exception 
{
	private static final long serialVersionUID = 8603362038035845948L;

	private String m_JobId;
	
	/**
	 * Create a new UnknownJobException.
	 * 
	 * @param jobId The Job Id that could not be found
	 * @param message Details of error explaining the context 
	 */
	public UnknownJobException(String jobId, String message)
	{
		super(message);
		this.m_JobId = jobId;
	}
	
	/**
	 * Get the unknown <i>JobId</i> that was the source of the error.
	 * @return
	 */
	public String getJobId()
	{
		return m_JobId;
	}
}
