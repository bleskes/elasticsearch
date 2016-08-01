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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;

import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.rs.provider.JobConfigurationParseException;

public class JobUpdater
{
    private static final Logger LOGGER = Logger.getLogger(JobUpdater.class);

    private static final String ANALYSIS_LIMITS_KEY = "analysisLimits";
    private static final String BACKGROUND_PERSIST_INTERVAL_KEY = "backgroundPersistInterval";
    private static final String CATEGORIZATION_FILTERS_KEY = "categorizationFilters";
    private static final String CUSTOM_SETTINGS = "customSettings";
    private static final String DETECTOR_KEY = "detectors";
    private static final String JOB_DESCRIPTION_KEY = "description";
    private static final String IGNORE_DOWNTIME_KEY = "ignoreDowntime";
    private static final String MODEL_DEBUG_CONFIG_KEY = "modelDebugConfig";
    private static final String RENORMALIZATION_WINDOW_DAYS_KEY = "renormalizationWindowDays";
    private static final String MODEL_SNAPSHOT_RETENTION_DAYS_KEY = "modelSnapshotRetentionDays";
    private static final String RESULTS_RETENTION_DAYS_KEY = "resultsRetentionDays";
    private static final String SCHEDULER_CONFIG_KEY = "schedulerConfig";

    private final JobManager m_JobManager;
    private final String m_JobId;
    private final StringWriter m_ConfigWriter;

    public JobUpdater(JobManager jobManager, String jobId)
    {
        m_JobManager = Objects.requireNonNull(jobManager);
        m_JobId = Objects.requireNonNull(jobId);
        m_ConfigWriter = new StringWriter();
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
     * @return a {@code Response}
     * @throws JobException If the update fails (e.g. the job does not exist, some of the updates
     * are invalid, the job is unavailable for updating, etc.)
     */
    public Response update(String updateJson) throws JobException
    {
        JobDetails job = m_JobManager.getJobOrThrowIfUnknown(m_JobId);

        JsonNode node = parse(updateJson);
        if (!node.isObject() || node.size() == 0)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_REQUIRES_NON_EMPTY_OBJECT),
                    ErrorCodes.JOB_CONFIG_PARSE_ERROR);
        }

        Map<String, Supplier<AbstractUpdater>> updaterPerKey = createUpdaterPerKeyMap(job);
        List<AbstractUpdater> updaters = new ArrayList<>();
        List<String> keysToUpdate = new ArrayList<>();
        Iterator<Entry<String, JsonNode>> fieldsIterator = node.fields();
        while (fieldsIterator.hasNext())
        {
            Entry<String, JsonNode> keyValue = fieldsIterator.next();
            LOGGER.debug("Updating job config for key: " + keyValue.getKey());
            AbstractUpdater updater = createKeyValueUpdater(updaterPerKey, keyValue.getKey());
            keysToUpdate.add(keyValue.getKey());
            updaters.add(updater);
            updater.prepareUpdate(keyValue.getValue());
        }

        for (AbstractUpdater updater : updaters)
        {
            updater.commit();
        }

        writeUpdateConfigMessage();
        m_JobManager.audit(m_JobId).info(Messages.getMessage(Messages.JOB_AUDIT_UPDATED, keysToUpdate));
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

    private Map<String, Supplier<AbstractUpdater>> createUpdaterPerKeyMap(JobDetails job)
    {
        return ImmutableMap.<String, Supplier<AbstractUpdater>>builder()
                .put(ANALYSIS_LIMITS_KEY, () -> new AnalysisLimitsUpdater(m_JobManager, job, ANALYSIS_LIMITS_KEY))
                .put(BACKGROUND_PERSIST_INTERVAL_KEY, () -> new BackgroundPersistIntervalUpdater(m_JobManager, job, BACKGROUND_PERSIST_INTERVAL_KEY))
                .put(CATEGORIZATION_FILTERS_KEY, () -> new CategorizationFiltersUpdater(m_JobManager, job, CATEGORIZATION_FILTERS_KEY))
                .put(CUSTOM_SETTINGS, () -> new CustomSettingsUpdater(m_JobManager, job, CUSTOM_SETTINGS))
                .put(DETECTOR_KEY, () -> new DetectorDescriptionUpdater(m_JobManager, job, DETECTOR_KEY))
                .put(JOB_DESCRIPTION_KEY, () -> new JobDescriptionUpdater(m_JobManager, job, JOB_DESCRIPTION_KEY))
                .put(IGNORE_DOWNTIME_KEY, () -> new IgnoreDowntimeUpdater(m_JobManager, job, IGNORE_DOWNTIME_KEY))
                .put(MODEL_DEBUG_CONFIG_KEY, () -> new ModelDebugConfigUpdater(m_JobManager, job, MODEL_DEBUG_CONFIG_KEY, m_ConfigWriter))
                .put(RENORMALIZATION_WINDOW_DAYS_KEY, () -> new RenormalizationWindowDaysUpdater(m_JobManager, job, RENORMALIZATION_WINDOW_DAYS_KEY))
                .put(MODEL_SNAPSHOT_RETENTION_DAYS_KEY, () -> new ModelSnapshotRetentionDaysUpdater(m_JobManager, job, MODEL_SNAPSHOT_RETENTION_DAYS_KEY))
                .put(RESULTS_RETENTION_DAYS_KEY, () -> new ResultsRetentionDaysUpdater(m_JobManager, job, RESULTS_RETENTION_DAYS_KEY))
                .put(SCHEDULER_CONFIG_KEY, () -> new SchedulerConfigUpdater(m_JobManager, job, SCHEDULER_CONFIG_KEY))
                .build();
    }

    private static AbstractUpdater createKeyValueUpdater(
            Map<String, Supplier<AbstractUpdater>> updaterPerKey, String key)
                    throws JobConfigurationException
    {
        if (updaterPerKey.containsKey(key) == false)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_INVALID_KEY, key),
                    ErrorCodes.INVALID_UPDATE_KEY);
        }
        return updaterPerKey.get(key).get();
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
