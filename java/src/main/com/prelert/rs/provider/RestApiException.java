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
package com.prelert.rs.provider;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.ErrorCode;

/**
 * Overrides the default {@linkplain WebApplicationException} 
 * response to include the error code and message 
 */
public class RestApiException extends WebApplicationException 
{
	private static final long serialVersionUID = -4162139513941557651L;
	
	private ErrorCode m_ErrorCode;
	private Response.Status m_Status;
	
	public RestApiException(String msg, ErrorCode errorCode, Response.Status status)
	{
		super(msg);
		m_ErrorCode = errorCode;
		m_Status = status;
	}

	@Override
	public Response getResponse()
	{
		ApiError err = new ApiError(m_ErrorCode);
		err.setMessage(this.getMessage());
		
		return Response.status(m_Status).entity(err.toJson()).build();		
	}
}
