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

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import com.prelert.job.process.MissingFieldException;

/**
 * Exception -> Response mapper for {@linkplain MissingFieldExceptionWrapper}.
 */
public class MissingFieldExceptionWrapper implements ExceptionMapper<MissingFieldException>
{
	@Override
	public Response toResponse(MissingFieldException e) 
	{
		String msg = String.format("Missing data field '%s'.\n"
				+ "Error message : %s\n",
				e.getMissingFieldName() , e.getMessage());
		
		return Response.status(Response.Status.BAD_REQUEST).
				entity(msg).build();
	}
}



