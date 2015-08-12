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
package com.prelert.job.persistence.elasticsearch;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;

import com.prelert.job.DataCounts;
import com.prelert.job.JobDetails;
import com.prelert.job.persistence.JobDataCountsPersister;

public class ElasticsearchJobDataCountsPersister implements JobDataCountsPersister
{
    private static final int RETRY_ON_CONFLICT = 3;

    private Client m_Client;
    private Logger m_Logger;

    public ElasticsearchJobDataCountsPersister(Client client, Logger logger)
    {
        m_Client = client;
        m_Logger = logger;
    }

    @Override
    public void persistDataCounts(String jobId, DataCounts counts)
    {
        try
        {
            UpdateRequestBuilder updateBuilder = m_Client.prepareUpdate(jobId,
                    JobDetails.TYPE, jobId);
            updateBuilder.setRetryOnConflict(RETRY_ON_CONFLICT);

            Map<String, Object> updates = new HashMap<>();
            updates.put(DataCounts.PROCESSED_RECORD_COUNT, counts.getProcessedRecordCount());
            updates.put(DataCounts.PROCESSED_FIELD_COUNT, counts.getProcessedFieldCount());
            updates.put(DataCounts.INPUT_RECORD_COUNT, counts.getInputRecordCount());
            updates.put(DataCounts.INPUT_BYTES, counts.getInputBytes());
            updates.put(DataCounts.INPUT_FIELD_COUNT, counts.getInputFieldCount());
            updates.put(DataCounts.INVALID_DATE_COUNT, counts.getInvalidDateCount());
            updates.put(DataCounts.MISSING_FIELD_COUNT, counts.getMissingFieldCount());
            updates.put(DataCounts.OUT_OF_ORDER_TIME_COUNT, counts.getOutOfOrderTimeStampCount());
            updates.put(DataCounts.FAILED_TRANSFORM_COUNT, counts.getFailedTransformCount());
            updates.put(DataCounts.EXCLUDED_RECORD_COUNT, counts.getExcludedRecordCount());
            updates.put(DataCounts.LATEST_RECORD_TIME, counts.getLatestRecordTimeStamp());

            Map<String, Object> countUpdates = new HashMap<>();
            countUpdates.put(JobDetails.COUNTS, updates);

            updateBuilder.setDoc(countUpdates).setRefresh(true);

            m_Logger.trace("ES API CALL: update ID " + jobId + " type " + JobDetails.TYPE +
                    " in index " + jobId + " using map of new values");
            m_Client.update(updateBuilder.request()).get();
        }
        catch (IndexMissingException | InterruptedException | ExecutionException e)
        {
            String msg = String.format("Error writing the job '%s' status stats.",
                    jobId);

            m_Logger.warn(msg, e);
        }
    }
}
