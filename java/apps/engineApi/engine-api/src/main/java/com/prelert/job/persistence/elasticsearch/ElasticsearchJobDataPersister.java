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


import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.prelert.job.persistence.JobDataPersister;

/**
 * Write the data sent to the API (analysis fields only) to Elasticsearch
 */
public class ElasticsearchJobDataPersister extends JobDataPersister
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchJobDataPersister.class);

    private static final String PERSISTED_RECORD_TYPE = "saved-data";

    public static final String TYPE = "saved-data";

    private static final int DOC_BUFFER_SIZE = 1000;

    private final Client m_Client;
    private final String m_IndexName;

    private final String [][] m_BufferedRecords;
    private final long [] m_Epochs;
    private int m_BufferedRecordCount;

    public ElasticsearchJobDataPersister(String jobId, Client client)
    {
        m_IndexName = jobId + "_raw";
        m_Client = client;

        m_BufferedRecords = new String [DOC_BUFFER_SIZE][];
        m_Epochs = new long [DOC_BUFFER_SIZE];
    }

    /**
     * The contents of <code>record</code> needs to be copied as it is
     * reused by other code
     */
    @Override
    public void persistRecord(long epoch, String [] record)
    {
        String [] copy = new String[record.length];
        System.arraycopy(record, 0, copy, 0, record.length);
        m_BufferedRecords[m_BufferedRecordCount] = copy;
        m_Epochs[m_BufferedRecordCount] = epoch;

        m_BufferedRecordCount++;


        if (m_BufferedRecordCount == DOC_BUFFER_SIZE)
        {
            // Checking whether the index exists can be quite expensive.
            if (isIndexExisting() == false) {
                LOGGER.info("Data will be persisted in the index " + m_IndexName);
                createIndex();
            }

            try
            {
                writeDocs();
            }
            finally
            {
                m_BufferedRecordCount = 0;
            }
        }
    }

    private void createIndex()
    {
        try
        {
            XContentBuilder inputDataMapping = ElasticsearchMappings.inputDataMapping();

            m_Client.admin().indices()
                    .prepareCreate(m_IndexName)
                    .addMapping(ElasticsearchJobDataPersister.TYPE, inputDataMapping)
                    .get();
            m_Client.admin().cluster().prepareHealth(m_IndexName).setWaitForYellowStatus().execute().actionGet();
        }
        catch (IOException e)
        {
            LOGGER.error("Error creating the raw data index " + m_IndexName, e);
        }
    }

    private boolean isIndexExisting()
    {
        return m_Client.admin().indices().prepareExists(m_IndexName).get().isExists();
    }

    @Override
    public void flushRecords()
    {
        if (m_BufferedRecordCount > 0)
        {
            writeDocs();
        }
    }

    private void writeDocs()
    {
        // write docs

        BulkRequestBuilder bulkRequest = m_Client.prepareBulk();

        for (int count=0; count<m_BufferedRecordCount; count++)
        {
            try
            {
                XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();

                String [] bufferedRecord = m_BufferedRecords[count];
                // epoch in ms
                jsonBuilder.startObject().field("epoch", m_Epochs[count] * 1000);


                for (int i=0; i<m_FieldNames.length; i++)
                {
                    try
                    {
                        jsonBuilder.field(m_FieldNames[i], Double.parseDouble(bufferedRecord[m_FieldMappings[i]]));
                    }
                    catch (NumberFormatException e)
                    {
                        jsonBuilder.field(m_FieldNames[i], bufferedRecord[m_FieldMappings[i]]);
                    }
                }

                jsonBuilder.startArray(BY_FIELDS);
                for (int i=0; i<m_ByFieldMappings.length; i++)
                {
                    jsonBuilder.value(bufferedRecord[m_ByFieldMappings[i]]);
                }
                jsonBuilder.endArray();

                jsonBuilder.startArray(OVER_FIELDS);
                for (int i=0; i<m_OverFieldMappings.length; i++)
                {
                    jsonBuilder.value(bufferedRecord[m_OverFieldMappings[i]]);
                }
                jsonBuilder.endArray();

                jsonBuilder.startArray(PARTITION_FIELDS);
                for (int i=0; i<m_PartitionFieldMappings.length; i++)
                {
                    jsonBuilder.value(bufferedRecord[m_PartitionFieldMappings[i]]);
                }
                jsonBuilder.endArray();

                jsonBuilder.endObject();


                bulkRequest.add(m_Client.prepareIndex(m_IndexName, PERSISTED_RECORD_TYPE)
                        .setSource(jsonBuilder));

                m_BufferedRecords[count] = null; // free mem

            }
            catch (IOException e)
            {
                LOGGER.error("Error creating json builder", e);
            }
        }

        BulkResponse bulkResponse = bulkRequest.execute().actionGet();
        if (bulkResponse.hasFailures())
        {
            LOGGER.error("Errors writing raw job data: " +
                    bulkResponse.buildFailureMessage());
        }
    }

    @Override
    public boolean deleteData()
    {
        if (isIndexExisting() == false)
        {
            return false;
        }
        LOGGER.debug("Deleting the raw data index " + m_IndexName);

        // we don't care about errors here as the
        // the index may not exist anyway
        try
        {
            DeleteIndexResponse response = m_Client.admin()
                    .indices().delete(new DeleteIndexRequest(m_IndexName)).get();

            return response.isAcknowledged();
        }
        catch (InterruptedException|ExecutionException e)
        {
            LOGGER.warn("Error deleting the raw data index", e);
            return false;
        }
    }
}
