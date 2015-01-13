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

package com.prelert.job.status.elasticsearch;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;

import com.prelert.job.JobDetails;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.usage.UsageReporter;


/**
 * The {@link #reportStatus(int)} function logs a status message
 * and updates the jobs ProcessedRecordCount, InvalidDateCount,
 * MissingFieldCount and OutOfOrderTimeStampCount values in the
 * Elasticsearch document.
 */
public class ElasticsearchStatusReporter extends StatusReporter
{
    private static final int RETRY_ON_CONFLICT = 3;

    private Client m_Client;

    public ElasticsearchStatusReporter(Client client, UsageReporter usageReporter,
            String jobId, JobDetails.Counts counts, Logger logger)
    {
        super(jobId, counts, usageReporter, logger);
        m_Client = client;
    }


    /**
     * Log a message then write to elastic search.
     */
    @Override
    protected void reportStatus(long totalRecords)
    {
        String status = String.format("%d records written to autodetect with %d "
                + "missing fields, %d were discarded because the date could not be "
                + "read and %d were ignored as because they weren't in ascending "
                + "chronological order.", getProcessedRecordCount(),
                getMissingFieldErrorCount(), getDateParseErrorsCount(),
                getOutOfOrderRecordCount());

        m_Logger.info(status);

        persistStats();
    }

    /**
     * Write the status counts to the datastore
     */
    private void persistStats()
    {
        try
        {
            UpdateRequestBuilder updateBuilder = m_Client.prepareUpdate(m_JobId,
                    JobDetails.TYPE, m_JobId);
            updateBuilder.setRetryOnConflict(RETRY_ON_CONFLICT);

            Map<String, Object> updates = new HashMap<>();
            updates.put(JobDetails.PROCESSED_RECORD_COUNT, getProcessedRecordCount());
            updates.put(JobDetails.PROCESSED_FIELD_COUNT, getProcessedFieldCount());
            updates.put(JobDetails.INPUT_RECORD_COUNT, getInputRecordCount());
            updates.put(JobDetails.INPUT_BYTES, getBytesRead());
            updates.put(JobDetails.INPUT_FIELD_COUNT, getInputFieldCount());
            updates.put(JobDetails.INVALID_DATE_COUNT, getDateParseErrorsCount());
            updates.put(JobDetails.MISSING_FIELD_COUNT, getMissingFieldErrorCount());
            updates.put(JobDetails.OUT_OF_ORDER_TIME_COUNT, getOutOfOrderRecordCount());

            Map<String, Object> counts = new HashMap<>();
            counts.put(JobDetails.COUNTS, updates);

            updateBuilder.setDoc(counts).setRefresh(true);

            m_Client.update(updateBuilder.request()).get();
        }
        catch (IndexMissingException | InterruptedException | ExecutionException e)
        {
            String msg = String.format("Error writing the job '%s' status stats.",
                    m_JobId);

            m_Logger.warn(msg, e);
        }
    }
}
