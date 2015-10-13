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
package com.prelert.rs.data;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.errorcodes.HasErrorCode;


/**
 * Encapsulates the an API error condition.
 * The errorCode identifies the error type and the message
 * provides further details. If the error was caused by
 * a Java Exception {@linkplain #getCause()} will return that
 * exception's error message else <code>null</code>.
 *
 * Note that the cause exception's message is used not the
 * actual exception this is due to problems serialising the
 * exceptions.
 *
 * @see ErrorCodes
 */
@JsonInclude(Include.NON_NULL)
public class ApiError implements HasErrorCode
{
    private ErrorCodes m_ErrorCode;
	private String m_Message;
	private String m_Cause;

	/**
	 * Default cons for serialisation (Jackson)
	 */
	public ApiError()
	{

	}

	/**
	 * Create a new ApiError from one of the list of error codes.
	 *
	 * @param errorCode
	 * @see ErrorCodes
	 */
	public ApiError(ErrorCodes errorCode)
	{
		m_ErrorCode = errorCode;
	}

	/**
	 * The error code
	 * @see ErrorCodes
	 * @return one of {@linkplain ErrorCodes}
	 */
	@Override
	public ErrorCodes getErrorCode()
	{
		return m_ErrorCode;
	}

	/**
	 * Set the error code.
	 * @see ErrorCodes
	 * @param value
	 */
	public void setErrorCode(ErrorCodes value)
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
	 * The message from the exception that caused the error
	 * @return The cause exception message or <code>null</code>
	 */
	public String getCause()
	{
		return m_Cause;
	}

	/**
	 * Set the error message of the cause exception
	 * @param e
	 */
	public void setCause(String e)
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
		JsonStringEncoder encoder = JsonStringEncoder.getInstance();

		StringBuilder builder = new StringBuilder();
		builder.append('{');

		boolean needComma = true;
		if (m_Message != null)
		{
			char [] message = encoder.quoteAsString(m_Message.toString());
			builder.append("\n  \"message\" : \"").append(message).append('"').append(',');
			needComma = false;
		}

		if (m_ErrorCode != null)
		{
			builder.append("\n  \"errorCode\" : ").append(m_ErrorCode.getValueString());
			needComma = true;
		}

		if (m_Cause != null)
		{
			if (needComma)
			{
				builder.append(',');
			}
			char [] cause = encoder.quoteAsString(m_Cause.toString());
			builder.append("\n  \"cause\" : \"").append(cause).append('"');
		}

		builder.append("\n}\n");

		return builder.toString();
	}

    @Override
    public int hashCode()
    {
        return this.toJson().hashCode();
    }


    /**
     * Throwable does not implement toString() so as the method is mainly
     * for testing compare the toJson() result
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        ApiError other = (ApiError) obj;

        return Objects.equals(this.toJson(), other.toJson());
    }
}
