package com.prelert.rs.provider;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.elasticsearch.ElasticsearchException;

import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.ErrorCode;


/**
 * ElasticSearch exception mapper. 
 * Constructs an error message from the rest status code
 * and exception message and returns in a a server error
 * (500) response.
 */
public class ElasticSearchExceptionMapper implements ExceptionMapper<ElasticsearchException>
{
	@Override
	public Response toResponse(ElasticsearchException e)
	{
		ApiError error = new ApiError(ErrorCode.DATA_STORE_ERROR);
		error.setMessage("Error in ElasticSearch: = " + e.getDetailedMessage());
		error.setCause(e);
		
		return Response.serverError().entity(error.toJson()).build();
	}
}
