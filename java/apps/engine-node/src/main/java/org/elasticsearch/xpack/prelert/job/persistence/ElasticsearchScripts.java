/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.persistence;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Create methods for the custom scripts that are run on Elasticsearch
 */
public final class ElasticsearchScripts
{
    private static final Logger LOGGER = Loggers.getLogger(ElasticsearchScripts.class);

    private static final String PAINLESS = "painless";

    // Script names
    private static final String UPDATE_AVERAGE_PROCESSING_TIME = "ctx._source.averageProcessingTimeMs = ctx._source.averageProcessingTimeMs"
            + " * 0.9 + params.timeMs * 0.1";
    private static final String UPDATE_BUCKET_COUNT = "ctx._source.counts.bucketCount += params.count";
    private static final String UPDATE_USAGE = "ctx._source.inputBytes += params.bytes;ctx._source.inputFieldCount += params.fieldCount;"
            + "ctx._source.inputRecordCount += params.recordCount;";

    // Script parameters
    private static final String COUNT_PARAM = "count";
    private static final String BYTES_PARAM = "bytes";
    private static final String FIELD_COUNT_PARAM = "fieldCount";
    private static final String RECORD_COUNT_PARAM = "recordCount";
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
        return new Script(UPDATE_BUCKET_COUNT, ScriptType.INLINE, PAINLESS, scriptParams);
    }

    public static Script newUpdateUsage(long additionalBytes, long additionalFields,
            long additionalRecords)
    {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put(BYTES_PARAM, additionalBytes);
        scriptParams.put(FIELD_COUNT_PARAM, additionalFields);
        scriptParams.put(RECORD_COUNT_PARAM, additionalRecords);
        return new Script(UPDATE_USAGE, ScriptType.INLINE, PAINLESS, scriptParams);
    }

    public static Script updateProcessingTime(Long processingTimeMs)
    {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put(PROCESSING_TIME_PARAM, processingTimeMs);
        return new Script(UPDATE_AVERAGE_PROCESSING_TIME, ScriptType.INLINE,
                PAINLESS, scriptParams);
    }

    /**
     * Updates the specified document via executing a script
     *
     * @param client
     *            the Elasticsearch client
     * @param index
     *            the index
     * @param type
     *            the document type
     * @param docId
     *            the document id
     * @param script
     *            the script the performs the update
     * @return {@code} true if successful, {@code} false otherwise
     */
    public static boolean updateViaScript(Client client, String index, String type, String docId,
            Script script) {
        try
        {
            client.prepareUpdate(index, type, docId)
            .setScript(script)
            .setRetryOnConflict(UPDATE_JOB_RETRY_COUNT).get();
        }
        catch (IndexNotFoundException e)
        {
            throw ExceptionsHelper.missingJobException(index);
        }
        catch (IllegalArgumentException e)
        {
            handleIllegalArgumentException(e, script);
        }
        return true;
    }

    /**
     * Upserts the specified document via executing a script
     *
     * @param client
     *            the Elasticsearch client
     * @param index
     *            the index
     * @param type
     *            the document type
     * @param docId
     *            the document id
     * @param script
     *            the script the performs the update
     * @param upsertMap
     *            the doc source of the update request to be used when the
     *            document does not exists
     * @return {@code} true if successful, {@code} false otherwise
     */
    public static boolean upsertViaScript(Client client, String index, String type, String docId,
            Script script, Map<String, Object> upsertMap) {
        try
        {
            client.prepareUpdate(index, type, docId)
            .setScript(script)
            .setUpsert(upsertMap)
            .setRetryOnConflict(UPDATE_JOB_RETRY_COUNT).get();
        }
        catch (IndexNotFoundException e)
        {
            throw ExceptionsHelper.missingJobException(index);
        }
        catch (IllegalArgumentException e)
        {
            handleIllegalArgumentException(e, script);
        }
        return true;
    }

    private static void handleIllegalArgumentException(IllegalArgumentException e, Script script)
    {
        String msg = Messages.getMessage(Messages.DATASTORE_ERROR_EXECUTING_SCRIPT, script);
        LOGGER.warn(msg);
        Throwable th = (e.getCause() == null) ? e : e.getCause();
        ElasticsearchException exception = new ElasticsearchException(msg, th);
        exception.addHeader("errorCode", ErrorCodes.DATA_STORE_ERROR.getValueString());
        throw exception;
    }

}
