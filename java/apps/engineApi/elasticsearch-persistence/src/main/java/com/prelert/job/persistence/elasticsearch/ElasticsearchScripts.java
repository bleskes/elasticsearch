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

package com.prelert.job.persistence.elasticsearch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;

import com.prelert.job.JobException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;

/**
 * Create methods for the custom scripts that are run on Elasticsearch
 */
public final class ElasticsearchScripts
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchScripts.class);

    // Script names
    private static final String UPDATE_AVERAGE_PROCESSING_TIME = "update-average-processing-time";
    private static final String UPDATE_BUCKET_COUNT = "update-bucket-count";
    private static final String UPDATE_USAGE = "update-usage";
    private static final String UPDATE_CATEGORIZATION_FILTERS = "update-categorization-filters";
    private static final String UPDATE_DETECTOR_DESCRIPTION = "update-detector-description";
    private static final String UPDATE_SCHEDULER_CONFIG = "update-scheduler-config";

    // Script parameters
    private static final String COUNT_PARAM = "count";
    private static final String BYTES_PARAM = "bytes";
    private static final String FIELD_COUNT_PARAM = "fieldCount";
    private static final String RECORD_COUNT_PARAM = "recordCount";
    private static final String NEW_CATEGORIZATION_FILTERS_PARAM = "newFilters";
    private static final String DETECTOR_INDEX_PARAM = "detectorIndex";
    private static final String NEW_DESCRIPTION_PARAM = "newDescription";
    private static final String NEW_SCHEDULER_CONFIG_PARAM = "newSchedulerConfig";
    private static final String PROCESSING_TIME_PARAM = "timeMs";

    public static final int UPDATE_JOB_RETRY_COUNT = 3;

    private ElasticsearchScripts()
    {
        // Do nothing
    }

    public static Script newUpdateBucketCount(long count)
    {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put(COUNT_PARAM, count);
        return new Script(UPDATE_BUCKET_COUNT, ScriptService.ScriptType.FILE,
                ScriptService.DEFAULT_LANG, scriptParams);
    }

    public static Script newUpdateUsage(long additionalBytes, long additionalFields,
            long additionalRecords)
    {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put(BYTES_PARAM, additionalBytes);
        scriptParams.put(FIELD_COUNT_PARAM, additionalFields);
        scriptParams.put(RECORD_COUNT_PARAM, additionalRecords);
        return new Script(UPDATE_USAGE, ScriptService.ScriptType.FILE,
                ScriptService.DEFAULT_LANG, scriptParams);
    }

    public static Script newUpdateCategorizationFilters(List<String> newFilters)
    {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put(NEW_CATEGORIZATION_FILTERS_PARAM, newFilters);
        return new Script(UPDATE_CATEGORIZATION_FILTERS, ScriptService.ScriptType.FILE,
                ScriptService.DEFAULT_LANG, scriptParams);
    }

    public static Script newUpdateDetectorDescription(int detectorIndex, String newDescription)
    {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put(DETECTOR_INDEX_PARAM, detectorIndex);
        scriptParams.put(NEW_DESCRIPTION_PARAM, newDescription);
        return new Script(UPDATE_DETECTOR_DESCRIPTION, ScriptService.ScriptType.FILE,
                ScriptService.DEFAULT_LANG, scriptParams);
    }

    public static Script newUpdateSchedulerConfig(Map<String, Object> newSchedulerConfig)
    {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put(NEW_SCHEDULER_CONFIG_PARAM, newSchedulerConfig);
        return new Script(UPDATE_SCHEDULER_CONFIG, ScriptService.ScriptType.FILE,
                ScriptService.DEFAULT_LANG, scriptParams);
    }

    public static Script updateProcessingTime(Long processingTimeMs)
    {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put(PROCESSING_TIME_PARAM, processingTimeMs);
        return new Script(UPDATE_AVERAGE_PROCESSING_TIME, ScriptService.ScriptType.FILE,
                ScriptService.DEFAULT_LANG, scriptParams);
    }


    public static boolean updateViaScript(Client client, String index, String type,
                                        String docId, Script script)
    throws UnknownJobException, JobException
    {
        try
        {
            client.prepareUpdate(index, type, docId)
                            .setScript(script)
                            .setRetryOnConflict(UPDATE_JOB_RETRY_COUNT).get();
        }
        catch (IndexNotFoundException e)
        {
            throw new UnknownJobException(index);
        }
        catch (IllegalArgumentException e)
        {
            handleIllegalArgumentException(e, script);
        }
        return true;
    }

    public static boolean upsertViaScript(Client client, String index, String type,
                                        String docId, Script script, Map<String, Object> upsertMap)
    throws UnknownJobException, JobException
    {
        try
        {
            client.prepareUpdate(index, type, docId)
                               .setScript(script)
                               .setUpsert(upsertMap)
                               .setRetryOnConflict(UPDATE_JOB_RETRY_COUNT).get();
        }
        catch (IndexNotFoundException e)
        {
            throw new UnknownJobException(index);
        }
        catch (IllegalArgumentException e)
        {
            handleIllegalArgumentException(e, script);
        }
        return true;
    }

    private static void handleIllegalArgumentException(IllegalArgumentException e, Script script)
    throws JobException
    {
        String msg = Messages.getMessage(Messages.DATASTORE_ERROR_EXECUTING_SCRIPT, script);
        LOGGER.warn(msg);
        Throwable th = (e.getCause() == null) ? e : e.getCause();
        throw new JobException(msg, ErrorCodes.DATA_STORE_ERROR, th);
    }

}
