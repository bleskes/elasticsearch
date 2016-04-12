/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectReader;

@Consumes(MediaType.APPLICATION_JSON)
abstract class AbstractMessageBodyReader<T> implements MessageBodyReader<T>
{
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
              MediaType mediaType)
    {
         // no need to check the media type because of the @Consumes annotation
         return type == getType();
    }

    @Override
    public T readFrom(Class<T> bean, Type genericType, Annotation[] annotation, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream input) throws IOException
    {
         // Sanity check. The consumes annotation means only Json should be read
         if (!mediaType.equals(MediaType.APPLICATION_JSON_TYPE)
              && !mediaType.equals(MediaType.APPLICATION_JSON_TYPE.withCharset("UTF-8")))
         {
              throw new WebApplicationException(
                        Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
         }

         try
         {
              try (JsonParser parser = new JsonFactory().createParser(input))
              {
                  T ret = getObjectReader().readValue(parser);
                  if (parser.nextToken() != null)
                  {
                      throw new JsonMappingException("Unexpected token after end of expected JSON: " + parser.getText());
                  }
                  return ret;
              }
         }
         catch (JsonParseException e)
         {
             throw handle(e);
         }
         catch (JsonMappingException e)
         {
             throw handle(e);
         }
    }

    protected abstract Class<?> getType();
    protected abstract ObjectReader getObjectReader();
    protected abstract JobConfigurationParseException handle(JsonParseException e);
    protected abstract JobConfigurationParseException handle(JsonMappingException e);
}
