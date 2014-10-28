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

package com.prelert.job.process;

import com.prelert.rs.data.ErrorCode;

/**
 * Exception thrown when there is an error running 
 * a native process (autodetect).
 */
public class NativeProcessRunException extends Exception
{
	private static final long serialVersionUID = 5722287151589093943L;		
	
	private ErrorCode m_ErrorCode;
	
	public NativeProcessRunException(String message, ErrorCode errorCode)
	{
		super(message);
		m_ErrorCode = errorCode;
	}
	
	public NativeProcessRunException(String message, ErrorCode errorCode, Throwable cause)
	{
		super(message, cause);
		m_ErrorCode = errorCode;
	}
	
	public ErrorCode getErrorCode()
	{
		return m_ErrorCode;
	}
	
	
}