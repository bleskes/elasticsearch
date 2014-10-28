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
package com.prelert.rs.provider;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import com.prelert.job.JobConfigurationException;
import com.prelert.rs.data.ApiError;

/**
 * Exception -> Response mapper for {@linkplain JobConfigurationException}.
 */
public class JobConfigurationExceptionMapper implements ExceptionMapper<JobConfigurationException>
{
	@Override
	public Response toResponse(JobConfigurationException configException) 
	{
		ApiError error = new ApiError(configException.getErrorCode());
		error.setMessage(configException.getMessage());
		error.setCause(configException.getCause());
		
		return Response.status(Response.Status.BAD_REQUEST)
				.entity(error.toJson()).build();
	}
}


