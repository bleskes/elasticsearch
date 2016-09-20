
package org.elasticsearch.xpack.prelert.job.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;

import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

/**
 * Create methods for the custom scripts that are run on Elasticsearch
 */
public final class ElasticsearchScripts
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchScripts.class);

    private static final String PAINLESS = "painless";

    // Script names
    private static final String UPDATE_AVERAGE_PROCESSING_TIME = "ctx._source.averageProcessingTimeMs = ctx._source.averageProcessingTimeMs * 0.9 + params.timeMs * 0.1";
    private static final String UPDATE_BUCKET_COUNT = "ctx._source.counts.bucketCount += params.count";
    private static final String UPDATE_USAGE = "ctx._source.inputBytes += params.bytes;ctx._source.inputFieldCount += params.fieldCount;ctx._source.inputRecordCount += params.recordCount;";
    private static final String UPDATE_CATEGORIZATION_FILTERS = "ctx._source.analysisConfig.categorizationFilters = params.newFilters;";
    private static final String UPDATE_DETECTOR_DESCRIPTION = "ctx._source.analysisConfig.detectors[params.detectorIndex].detectorDescription = params.newDescription;";
    private static final String UPDATE_DETECTOR_RULES = "ctx._source.analysisConfig.detectors[params.detectorIndex].detectorRules = params.newDetectorRules;";
    private static final String UPDATE_SCHEDULER_CONFIG = "ctx._source.schedulerConfig = params.newSchedulerConfig;";

    // Script parameters
    private static final String COUNT_PARAM = "count";
    private static final String BYTES_PARAM = "bytes";
    private static final String FIELD_COUNT_PARAM = "fieldCount";
    private static final String RECORD_COUNT_PARAM = "recordCount";
    private static final String NEW_CATEGORIZATION_FILTERS_PARAM = "newFilters";
    private static final String DETECTOR_INDEX_PARAM = "detectorIndex";
    private static final String NEW_DESCRIPTION_PARAM = "newDescription";
    private static final String NEW_DETECTOR_RULES_PARAM = "newDetectorRules";
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
        return new Script(UPDATE_BUCKET_COUNT, ScriptService.ScriptType.INLINE, PAINLESS, scriptParams);
    }

    public static Script newUpdateUsage(long additionalBytes, long additionalFields,
            long additionalRecords)
    {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put(BYTES_PARAM, additionalBytes);
        scriptParams.put(FIELD_COUNT_PARAM, additionalFields);
        scriptParams.put(RECORD_COUNT_PARAM, additionalRecords);
        return new Script(UPDATE_USAGE, ScriptService.ScriptType.INLINE, PAINLESS, scriptParams);
    }

    public static Script newUpdateCategorizationFilters(List<String> newFilters)
    {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put(NEW_CATEGORIZATION_FILTERS_PARAM, newFilters);
        return new Script(UPDATE_CATEGORIZATION_FILTERS, ScriptService.ScriptType.INLINE,
                PAINLESS, scriptParams);
    }

    public static Script newUpdateDetectorDescription(int detectorIndex, String newDescription)
    {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put(DETECTOR_INDEX_PARAM, detectorIndex);
        scriptParams.put(NEW_DESCRIPTION_PARAM, newDescription);
        return new Script(UPDATE_DETECTOR_DESCRIPTION, ScriptService.ScriptType.INLINE,
                PAINLESS, scriptParams);
    }

    public static Script newUpdateDetectorRules(int detectorIndex, List<Map<String, Object>> newRules)
    {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put(DETECTOR_INDEX_PARAM, detectorIndex);
        scriptParams.put(NEW_DETECTOR_RULES_PARAM, newRules);
        return new Script(UPDATE_DETECTOR_RULES, ScriptService.ScriptType.INLINE,
                PAINLESS, scriptParams);
    }

    public static Script newUpdateSchedulerConfig(Map<String, Object> newSchedulerConfig)
    {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put(NEW_SCHEDULER_CONFIG_PARAM, newSchedulerConfig);
        return new Script(UPDATE_SCHEDULER_CONFIG, ScriptService.ScriptType.INLINE,
                PAINLESS, scriptParams);
    }

    public static Script updateProcessingTime(Long processingTimeMs)
    {
        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put(PROCESSING_TIME_PARAM, processingTimeMs);
        return new Script(UPDATE_AVERAGE_PROCESSING_TIME, ScriptService.ScriptType.INLINE,
                PAINLESS, scriptParams);
    }

    /**
     * Updates the specified document via executing a script
     *
     * @param client the Elasticsearch client
     * @param index the index
     * @param type the document type
     * @param docId the document id
     * @param script the script the performs the update
     * @return {@code} true if successful, {@false) otherwise
     * @throws UnknownJobException if no job does not exist
     * @throws JobException if the update fails (e.g. the script does not exist)
     */
    public static boolean updateViaScript(Client client, String index, String type, String docId,
            Script script) throws JobException, UnknownJobException {
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

    /**
     * Upserts the specified document via executing a script
     *
     * @param client the Elasticsearch client
     * @param index the index
     * @param type the document type
     * @param docId the document id
     * @param script the script the performs the update
     * @param upsertMap the doc source of the update request to be used when the document does not exists
     * @return {@code} true if successful, {@false) otherwise
     * @throws UnknownJobException if no job does not exist
     * @throws JobException if the update fails (e.g. the script does not exist)
     */
    public static boolean upsertViaScript(Client client, String index, String type, String docId,
            Script script, Map<String, Object> upsertMap) throws JobException, UnknownJobException {
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
