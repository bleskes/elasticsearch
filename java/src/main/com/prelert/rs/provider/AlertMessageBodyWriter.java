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
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prelert.job.alert.Alert;


/**
 * Web service provider writes {@linkplain Alert} objects JSON.
 */
public class AlertMessageBodyWriter implements MessageBodyWriter<Alert>
{
	/**
	 * The Object to JSON mapper.
	 * Writes dates in ISO 8601 format
	 */
	static final private ObjectWriter s_ObjectWriter = 
			new ObjectMapper()
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
				.writer().withDefaultPrettyPrinter();
	

	@Override
	public long getSize(Alert arg0, Class<?> arg1, Type arg2,
			Annotation[] arg3, MediaType arg4) 
	{
		// deprecated by JAX-RS 2.0
		return 0;
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] arg2,
			MediaType mediaType) 
	{
		// no need to check the media type because of the @Produces annotation
		return type == Alert.class;
	}

	@Override
	public void writeTo(Alert bean, Class<?> type, 
			Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, 
			OutputStream entityStream)
    throws IOException, WebApplicationException 
	{
		s_ObjectWriter.writeValue(entityStream, bean);
	}
}
