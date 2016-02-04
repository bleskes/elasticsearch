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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.prelert.job.Detector;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;

/**
 * Detector entity provider.
 * Reads the http message body and converts it to a Detector
 * bean. Only conversion from JSON is supported.
 */
@Consumes(MediaType.APPLICATION_JSON)
public class DetectorMessageBodyReader implements MessageBodyReader<Detector>
{
     /**
      * The Object to JSON mapper.
      */
     private static final ObjectReader OBJECT_READER = new ObjectMapper().readerFor(Detector.class);

     @Override
     public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
               MediaType mediaType)
     {
          // no need to check the media type because of the @Consumes annotation
          return type == Detector.class;
     }

     @Override
     public Detector readFrom(Class<Detector> bean, Type genericType,
               Annotation[] annotation, MediaType mediaType,
               MultivaluedMap<String, String> httpHeaders, InputStream input)
                         throws IOException, WebApplicationException
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
               return OBJECT_READER.readValue(input);
          }
          catch (JsonParseException e)
          {
              throw new JobConfigurationParseException(
                         Messages.getMessage(Messages.JSON_DETECTOR_CONFIG_PARSE), e,
                         ErrorCodes.DETECTOR_PARSE_ERROR);
          }
          catch (JsonMappingException e)
          {
              if (e.getCause() != null)
              {
                  if (e.getCause() instanceof JobConfigurationException)
                  {
                      JobConfigurationException jce = (JobConfigurationException)e.getCause();
                      throw new JobConfigurationParseException(jce.getMessage(), e,
                                           jce.getErrorCode());
                  }
              }

              throw new JobConfigurationParseException(
                         Messages.getMessage(Messages.JSON_DETECTOR_CONFIG_MAPPING), e,
                         ErrorCodes.DETECTOR_UNKNOWN_FIELD_ERROR);
          }
     }

}
