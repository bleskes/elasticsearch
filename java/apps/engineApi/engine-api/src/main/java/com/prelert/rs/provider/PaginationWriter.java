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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prelert.rs.data.Pagination;


/**
 * Web service provider converts generic Pagination objects to JSON.
 * As all results are wrapped in generic Pagination objects this
 * MessageBodyWriter will write all specialisations.
 * Conversion to JSON is done using the Jackson ObjectMapper
 *
 * @param <T>
 */
public class PaginationWriter<T> implements MessageBodyWriter<Pagination<T>>
{
	/**
	 * The Object to JSON mapper.
	 * Writes dates in ISO 8601 format
	 */
	private static final ObjectWriter s_ObjectWriter =
			new ObjectMapper()
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
				.writer().withDefaultPrettyPrinter();

	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType)
	{
		// no need to check the media type because of the @Produces annotation
		if (type == Pagination.class &&
				genericType instanceof ParameterizedType)
		{
			return true;
		}

		return false;
	}

	@Override
	public long getSize(Pagination<T> arg0, Class<?> arg1, Type arg2,
			Annotation[] arg3, MediaType arg4)
	{
		// deprecated by JAX-RS 2.0
		return 0;
	}

	/**
	 * Write the Pagination object bean
	 */
	@Override
	public void writeTo(Pagination<T> bean, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream)
	throws IOException, WebApplicationException
	{
		s_ObjectWriter.writeValue(entityStream, bean);
	}

}
