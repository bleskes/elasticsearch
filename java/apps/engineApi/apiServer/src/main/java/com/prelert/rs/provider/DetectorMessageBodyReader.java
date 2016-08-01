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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.prelert.job.Detector;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.messages.Messages;

/**
 * Detector entity provider.
 * Reads the http message body and converts it to a Detector
 * bean. Only conversion from JSON is supported.
 */
public class DetectorMessageBodyReader extends AbstractMessageBodyReader<Detector>
{
    /**
     * The Object to JSON mapper.
     */
    private static final ObjectReader OBJECT_READER = new ObjectMapper().readerFor(Detector.class);

    @Override
    protected Class<?> getType()
    {
        return Detector.class;
    }

    @Override
    protected ObjectReader getObjectReader()
    {
        return OBJECT_READER;
    }

    @Override
    protected JobConfigurationParseException handle(JsonParseException e)
    {
        return new JobConfigurationParseException(
                Messages.getMessage(Messages.JSON_DETECTOR_CONFIG_PARSE), e,
                ErrorCodes.DETECTOR_PARSE_ERROR);
    }

    @Override
    protected JobConfigurationParseException handle(JsonMappingException e)
    {
        if (e.getCause() != null)
        {
            Throwable cause = e.getCause();
            if (cause instanceof JobConfigurationException)
            {
                JobConfigurationException jce = (JobConfigurationException) cause;
                throw new JobConfigurationParseException(jce.getMessage(), e, jce.getErrorCode());
            }
        }

        throw new JobConfigurationParseException(
                Messages.getMessage(Messages.JSON_DETECTOR_CONFIG_MAPPING), e,
                ErrorCodes.DETECTOR_UNKNOWN_FIELD_ERROR);
    }
}
