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
package com.prelert.rs.data;

/**
 * Encapsulates the an API error condition.
 * The errorCode identifies the error type and the message 
 * provides further details. If the error was caused by 
 * a Java Exception {@linkplain #getCause()} will return that
 * Exception else it returns <code>null</code>. 
 * 
 * @see ErrorCodes
 */
public class ApiError 
{
	private long m_ErrorCode;
	private String m_Message;
	private Throwable m_Cause;
	
	/**
	 * Create a new ApiError from one of the list of error codes.
	 * 
	 * @param errorCode
	 * @see ErrorCodes 
	 */
	public ApiError(long errorCode)
	{
		m_ErrorCode = errorCode;
	}
	
	/**
	 * The error code
	 * @see ErrorCodes
	 * @return one of {@linkplain ErrorCodes}
	 */
	public long getErrorCode()
	{
		return m_ErrorCode;
	}
	
	/**
	 * Set the error code.
	 * @see ErrorCodes
	 * @param value
	 */
	public void setErrorCode(long value)
	{
		m_ErrorCode = value;
	}
	
	/**
	 * The error message
	 * @return The error string
	 */
	public String getMessage()
	{
		return m_Message;
	}
	
	/**
	 * Set the error message
	 * @param message
	 */
	public void setMessage(String message)
	{
		m_Message = message;
	}
	
	/**
	 * The exception that caused the error
	 * @return The exception that caused the error or <code>null</code> 
	 */
	public Throwable getCause()
	{
		return m_Cause;
	}
	
	/**
	 * Set the cause to the error
	 * @param e
	 */
	public void setCause(Throwable e)
	{
		m_Cause = e;
	}
	
	/**
	 * JSON representation of this object.
	 * If cause is null then it is not written and
	 * if errorCode <= 0 then it is not written.
	 * @return JSON string
	 */
	public String toJson()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("{\n  \"message\" : \"").append(m_Message).append('"');
		
		if (m_ErrorCode > 0)
		{
			builder.append(",\n  \"errorCode\" : ").append(m_ErrorCode);
		}
		
		if (m_Cause != null)
		{
			builder.append(",\n  \"cause\" : \"").append(m_Cause).append('"');
		}
		
		builder.append("\n}\n");
		
		return builder.toString();
	}
}
