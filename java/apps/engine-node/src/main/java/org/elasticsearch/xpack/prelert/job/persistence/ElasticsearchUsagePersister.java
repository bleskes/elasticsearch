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

import static org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchJobProvider.PRELERT_USAGE_INDEX;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.engine.VersionConflictEngineException;

import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.usage.Usage;

public class ElasticsearchUsagePersister implements UsagePersister {
    private static final String USAGE_DOC_ID_PREFIX = "usage-";

    private final Client client;
    private final Logger logger;
    private final DateTimeFormatter dateTimeFormatter;
    private final Map<String, Object> upsertMap;
    private String docId;

    public ElasticsearchUsagePersister(Client client, Logger logger) {
        this.client = client;
        this.logger = logger;
        dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX", Locale.ROOT);
        upsertMap = new HashMap<>();

        upsertMap.put(ElasticsearchMappings.ES_TIMESTAMP, "");
        upsertMap.put(Usage.INPUT_BYTES, null);
    }

    @Override
    public void persistUsage(String jobId, long bytesRead, long fieldsRead, long recordsRead) throws JobException {
        ZonedDateTime nowTruncatedToHour = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
        String formattedNowTruncatedToHour = nowTruncatedToHour.format(dateTimeFormatter);
        docId = USAGE_DOC_ID_PREFIX + formattedNowTruncatedToHour;
        upsertMap.put(ElasticsearchMappings.ES_TIMESTAMP, formattedNowTruncatedToHour);

        // update global count
        updateDocument(PRELERT_USAGE_INDEX, docId, bytesRead, fieldsRead, recordsRead);
        updateDocument(new ElasticsearchJobId(jobId).getIndex(), docId, bytesRead,
                fieldsRead, recordsRead);
    }


    /**
     * Update the metering document in the given index/id.
     * Uses a script to update the volume field and 'upsert'
     * to create the doc if it doesn't exist.
     *
     * @param index the index to persist to
     * @param id                Doc id is also its timestamp
     * @param additionalBytes   Add this value to the running total
     * @param additionalFields  Add this value to the running total
     * @param additionalRecords Add this value to the running total
     */
    private void updateDocument(String index, String id, long additionalBytes, long additionalFields, long additionalRecords)
            throws JobException {
        upsertMap.put(Usage.INPUT_BYTES, new Long(additionalBytes));
        upsertMap.put(Usage.INPUT_FIELD_COUNT, new Long(additionalFields));
        upsertMap.put(Usage.INPUT_RECORD_COUNT, new Long(additionalRecords));

        logger.trace("ES API CALL: upsert ID " + id +
                " type " + Usage.TYPE + " in index " + index +
                " by running Groovy script update-usage with arguments bytes=" + additionalBytes +
                " fieldCount=" + additionalFields + " recordCount=" + additionalRecords);

        try {
            ElasticsearchScripts.upsertViaScript(client, index, Usage.TYPE, id,
                    ElasticsearchScripts.newUpdateUsage(additionalBytes, additionalFields,
                            additionalRecords),
                    upsertMap);
        } catch (VersionConflictEngineException e) {
            logger.error("Failed to update the Usage document " + id +
                    " in index " + index, e);
        } catch (JobException e) {
            // log and rethrow
            logger.error(e);
            throw e;
        }
    }
}
