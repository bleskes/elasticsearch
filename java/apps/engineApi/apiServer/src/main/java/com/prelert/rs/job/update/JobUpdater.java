/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

package com.prelert.rs.job.update;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;

import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.rs.provider.JobConfigurationParseException;

public class JobUpdater
{
    private static final Logger LOGGER = Logger.getLogger(JobUpdater.class);
    static final String JOB_DESCRIPTION_KEY = "description";
    static final String MODEL_DEBUG_CONFIG_KEY = "modelDebugConfig";

    private final JobManager m_JobManager;
    private final String m_JobId;
    private final Map<String, Supplier<AbstractUpdater>> m_UpdaterPerKey;

    public JobUpdater(JobManager jobManager, String jobId)
    {
        m_JobManager = Objects.requireNonNull(jobManager);
        m_JobId = Objects.requireNonNull(jobId);
        m_UpdaterPerKey = createUpdaterPerKeyMap();
    }

    private Map<String, Supplier<AbstractUpdater>> createUpdaterPerKeyMap()
    {
        return ImmutableMap.<String, Supplier<AbstractUpdater>>builder()
                .put(JOB_DESCRIPTION_KEY, () -> new JobDescriptionUpdater(m_JobManager, m_JobId))
                .put(MODEL_DEBUG_CONFIG_KEY, () -> new ModelDebugConfigUpdater(m_JobManager, m_JobId))
                .build();
    }

    public Response update(String updateJson) throws UnknownJobException, JobConfigurationException
    {
        JsonNode node = parse(updateJson);
        if (node.isObject() == false)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_NO_OBJECT),
                    ErrorCodes.JOB_CONFIG_PARSE_ERROR);
        }

        Iterator<Entry<String, JsonNode>> fieldsIterator = node.fields();
        while (fieldsIterator.hasNext())
        {
            Entry<String, JsonNode> keyValue = fieldsIterator.next();
            LOGGER.debug("Updating job config for key: " + keyValue.getKey());
            createKeyValueUpdater(keyValue.getKey()).update(keyValue.getValue());
            LOGGER.debug("Updated successfully job config for key: " + keyValue.getKey());
        }

        return Response.ok(new Acknowledgement()).build();
    }

    private JsonNode parse(String json) throws JobConfigurationException
    {
        ObjectMapper mapper = new ObjectMapper();
        try
        {
            return mapper.readTree(json);
        }
        catch (IOException e)
        {
            throw new JobConfigurationParseException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_PARSE_ERROR),
                    e, ErrorCodes.JOB_CONFIG_PARSE_ERROR);
        }
    }

    private AbstractUpdater createKeyValueUpdater(String key) throws JobConfigurationException
    {
        if (m_UpdaterPerKey.containsKey(key) == false)
        {
            throw new JobConfigurationException(createInvalidKeyMsg(key),
                    ErrorCodes.INVALID_UPDATE_KEY);
        }
        return m_UpdaterPerKey.get(key).get();
    }

    private String createInvalidKeyMsg(String key)
    {
        String validKeys = Joiner.on(", ").join(m_UpdaterPerKey.keySet().iterator()).toString();
        return Messages.getMessage(Messages.JOB_CONFIG_UPDATE_INVALID_KEY, key, validKeys);
    }
}
