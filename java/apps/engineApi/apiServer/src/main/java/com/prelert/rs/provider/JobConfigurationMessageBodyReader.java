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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.prelert.job.JobConfiguration;
import com.prelert.job.condition.UnknownOperatorException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.messages.Messages;

/**
 * JobConfiguration entity provider.
 * Reads the http message body and converts it to a JobConfiguration
 * bean. Only conversion from JSON is supported.
 */
public class JobConfigurationMessageBodyReader extends AbstractMessageBodyReader<JobConfiguration>
{
    /**
     * The Object to JSON mapper.
     */
    private static final ObjectReader OBJECT_READER = new ObjectMapper()
            .readerFor(JobConfiguration.class)
            .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    @Override
    protected Class<?> getType()
    {
        return JobConfiguration.class;
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
                Messages.getMessage(Messages.JSON_JOB_CONFIG_PARSE), e,
                ErrorCodes.JOB_CONFIG_PARSE_ERROR);
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
                return new JobConfigurationParseException(jce.getMessage(), e, jce.getErrorCode());
            }
            else if (cause instanceof UnknownOperatorException)
            {
                UnknownOperatorException uoe = (UnknownOperatorException) cause;

                return new JobConfigurationParseException(Messages.getMessage(
                        Messages.JOB_CONFIG_CONDITION_UNKNOWN_OPERATOR, uoe.getName()),
                        uoe, ErrorCodes.UNKNOWN_OPERATOR);
            }
        }

        return new JobConfigurationParseException(
                Messages.getMessage(Messages.JSON_JOB_CONFIG_MAPPING), e,
                ErrorCodes.JOB_CONFIG_UNKNOWN_FIELD_ERROR);
    }
}
