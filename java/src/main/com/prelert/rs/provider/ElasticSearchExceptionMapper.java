package com.prelert.rs.provider;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.elasticsearch.ElasticSearchException;


/**
 * ElasticSearch exception mapper. 
 * Constructs an error message from the rest status code
 * and exception message and returns in a a server error
 * (500) response.
 */
public class ElasticSearchExceptionMapper implements ExceptionMapper<ElasticSearchException>
{
	@Override
	public Response toResponse(ElasticSearchException e)
	{
		String msg = String.format("Error in ElasticSearch status = %s, " 
					+ "detailed error: %s\n", e.status().toString(), e.getDetailedMessage());
		
		return Response.serverError().entity(msg).build();
	}
}
