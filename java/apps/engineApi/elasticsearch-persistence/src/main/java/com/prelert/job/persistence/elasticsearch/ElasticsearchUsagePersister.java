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

import static com.prelert.job.persistence.elasticsearch.ElasticsearchJobProvider.PRELERT_USAGE_INDEX;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.engine.VersionConflictEngineException;

import com.prelert.job.persistence.UsagePersister;
import com.prelert.job.usage.Usage;

public class ElasticsearchUsagePersister implements UsagePersister
{
    private static final String USAGE_DOC_ID_PREFIX = "usage-";
    private static final int RETRY_COUNT = 5;

    private final Client m_Client;
    private final Logger m_Logger;
    private final DateTimeFormatter m_DateTimeFormatter;
    private final Map<String, Object> m_UpsertMap;
    private String m_DocId;

    public ElasticsearchUsagePersister(Client client, Logger logger)
    {
        m_Client = client;
        m_Logger = logger;
        m_DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX");
        m_UpsertMap = new HashMap<>();

        m_UpsertMap.put(ElasticsearchMappings.ES_TIMESTAMP, "");
        m_UpsertMap.put(Usage.INPUT_BYTES, null);
    }

    @Override
    public void persistUsage(String jobId, long bytesRead, long fieldsRead, long recordsRead)
    {
        ZonedDateTime nowTruncatedToHour = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
        String formattedNowTruncatedToHour = nowTruncatedToHour.format(m_DateTimeFormatter);
        m_DocId = USAGE_DOC_ID_PREFIX + formattedNowTruncatedToHour;
        m_UpsertMap.put(ElasticsearchMappings.ES_TIMESTAMP, formattedNowTruncatedToHour);

        // update global count
        updateDocument(PRELERT_USAGE_INDEX,  m_DocId, bytesRead, fieldsRead, recordsRead);
        updateDocument(new ElasticsearchJobId(jobId).getIndex(), m_DocId, bytesRead,
                fieldsRead, recordsRead);
    }


    /**
     * Update the metering document in the given index/id.
     * Uses a script to update the volume field and 'upsert'
     * to create the doc if it doesn't exist.
     *
     * @param index
     * @param id Doc id is also its timestamp
     * @param additionalBytes Add this value to the running total
     * @param additionalFields Add this value to the running total
     * @param additionalRecords Add this value to the running total
     */
    private void updateDocument(String index, String id,
            long additionalBytes, long additionalFields, long additionalRecords)
    {
        m_UpsertMap.put(Usage.INPUT_BYTES, new Long(additionalBytes));
        m_UpsertMap.put(Usage.INPUT_FIELD_COUNT, new Long(additionalFields));
        m_UpsertMap.put(Usage.INPUT_RECORD_COUNT, new Long(additionalRecords));

        m_Logger.trace("ES API CALL: upsert ID " + id +
                " type " + Usage.TYPE + " in index " + index +
                " by running Groovy script update-usage with arguments bytes=" + additionalBytes +
                " fieldCount=" + additionalFields + " recordCount=" + additionalRecords);

        try
        {
            m_Client.prepareUpdate(index, Usage.TYPE, id)
                    .setScript(ElasticsearchScripts.newUpdateUsage(
                            additionalBytes, additionalFields, additionalRecords))
                    .setUpsert(m_UpsertMap)
                    .setRetryOnConflict(RETRY_COUNT).get();
        }
        catch (VersionConflictEngineException e)
        {
            m_Logger.error("Failed to update the Usage document " + id +
                            " in index " + index, e);
        }
    }
}
