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

package com.prelert.rs.job.update;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.rs.provider.JobConfigurationParseException;

public class JobUpdater
{
    private static final Logger LOGGER = Logger.getLogger(JobUpdater.class);

    private static final String CUSTOM_SETTINGS = "customSettings";
    private static final String DETECTOR_KEY = "detectors";
    private static final String JOB_DESCRIPTION_KEY = "description";
    private static final String MODEL_DEBUG_CONFIG_KEY = "modelDebugConfig";
    private static final String RENORMALIZATION_WINDOW_KEY = "renormalizationWindow";
    private static final String MODEL_SNAPSHOT_RETENTION_DAYS_KEY = "modelSnapshotRetentionDays";
    private static final String RESULTS_RETENTION_DAYS_KEY = "resultsRetentionDays";
    private static final Set<String> HIDDEN_PROPERTIES = new HashSet<>(
            Arrays.asList(CUSTOM_SETTINGS, MODEL_DEBUG_CONFIG_KEY));

    private final JobManager m_JobManager;
    private final String m_JobId;
    private final Map<String, Supplier<AbstractUpdater>> m_UpdaterPerKey;
    private final StringWriter m_ConfigWriter;

    public JobUpdater(JobManager jobManager, String jobId)
    {
        m_JobManager = Objects.requireNonNull(jobManager);
        m_JobId = Objects.requireNonNull(jobId);
        m_UpdaterPerKey = createUpdaterPerKeyMap();
        m_ConfigWriter = new StringWriter();
    }

    private Map<String, Supplier<AbstractUpdater>> createUpdaterPerKeyMap()
    {
        return ImmutableMap.<String, Supplier<AbstractUpdater>>builder()
                .put(CUSTOM_SETTINGS, () -> new CustomSettingsUpdater(m_JobManager, m_JobId))
                .put(DETECTOR_KEY, () -> new DetectorDescriptionUpdater(m_JobManager, m_JobId))
                .put(JOB_DESCRIPTION_KEY, () -> new JobDescriptionUpdater(m_JobManager, m_JobId))
                .put(MODEL_DEBUG_CONFIG_KEY, () -> new ModelDebugConfigUpdater(m_JobManager, m_JobId, m_ConfigWriter))
                .put(RENORMALIZATION_WINDOW_KEY, () -> new RenormalizationWindowUpdater(m_JobManager, m_JobId))
                .put(MODEL_SNAPSHOT_RETENTION_DAYS_KEY, () -> new ModelSnapshotRetentionDaysUpdater(m_JobManager, m_JobId))
                .put(RESULTS_RETENTION_DAYS_KEY, () -> new ResultsRetentionDaysUpdater(m_JobManager, m_JobId))
                .build();
    }

    /**
     * Performs a job update according to the given JSON input. The update is done in 2 steps:
     * <ol>
     *   <li> Prepare update (performs validations)
     *   <li> Commit update
     * </ol>
     * If there are invalid updates, none of the updates is applied.
     *
     * @param updateJson the JSON input that contains the requested updates
     * @return a {@code RESPONSE}
     * @throws JobConfigurationException If some of the updates are invalid
     * @throws UnknownJobException If the job does not exist
     * @throws JobInUseException If the job is unavailable for updating
     * @throws NativeProcessRunException If an error occurs in job process
     */
    public Response update(String updateJson) throws JobConfigurationException, UnknownJobException,
            JobInUseException, NativeProcessRunException
    {
        JsonNode node = parse(updateJson);
        if (node.isObject() == false)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_NO_OBJECT),
                    ErrorCodes.JOB_CONFIG_PARSE_ERROR);
        }

        List<AbstractUpdater> updaters = new ArrayList<>();
        Iterator<Entry<String, JsonNode>> fieldsIterator = node.fields();
        while (fieldsIterator.hasNext())
        {
            Entry<String, JsonNode> keyValue = fieldsIterator.next();
            LOGGER.debug("Updating job config for key: " + keyValue.getKey());
            AbstractUpdater updater = createKeyValueUpdater(keyValue.getKey());
            updaters.add(updater);
            updater.prepareUpdate(keyValue.getValue());
        }

        for (AbstractUpdater updater : updaters)
        {
            updater.commit();
        }

        writeUpdateConfigMessage();
        m_JobManager.audit(m_JobId).info(Messages.getMessage(Messages.JOB_AUDIT_UPDATED));
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
        List<String> keys = m_UpdaterPerKey.keySet().stream()
                .filter(k -> !HIDDEN_PROPERTIES.contains(k)).collect(Collectors.toList());
        String validKeys = Joiner.on(", ").join(keys).toString();
        return Messages.getMessage(Messages.JOB_CONFIG_UPDATE_INVALID_KEY, key, validKeys);
    }

    private void writeUpdateConfigMessage() throws JobInUseException, NativeProcessRunException
    {
        String config = m_ConfigWriter.toString();
        if (!config.isEmpty())
        {
            m_JobManager.writeUpdateConfigMessage(m_JobId, config);
        }
    }
}
