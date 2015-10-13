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

import org.elasticsearch.ElasticsearchException;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.rs.data.ApiError;


/**
 * Elasticsearch exception mapper.
 * Constructs an error message from the rest status code
 * and exception message and returns in a a server error
 * (500) response.
 */
public class ElasticsearchExceptionMapper implements ExceptionMapper<ElasticsearchException>
{
	@Override
	public Response toResponse(ElasticsearchException e)
	{
		ApiError error = new ApiError(ErrorCodes.DATA_STORE_ERROR);
		error.setMessage("Error in Elasticsearch: = " + e.getDetailedMessage());
		error.setCause(e.toString());

		return Response.serverError().entity(error.toJson()).build();
	}
}
