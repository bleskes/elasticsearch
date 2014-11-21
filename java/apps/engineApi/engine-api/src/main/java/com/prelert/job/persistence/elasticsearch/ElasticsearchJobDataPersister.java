/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
import java.util.Arrays;
import java.util.List;
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


public class ElasticsearchJobDataPersister implements JobDataPersister
{
    static public String PERSISTED_RECORD_TYPE = "saved-data";

    public static final String FIELDS = "fields";
    public static final String BY_FIELDS = "byFields";
    public static final String OVER_FIELDS = "overFields";
    public static final String PARTITION_FIELDS = "partitionFields";

    public static final String TYPE = "saved-data";

    private static final int DOC_BUFFER_SIZE = 1000;

    private Client m_Client;
    private String m_IndexName;

    private String [] m_FieldNames;
    private int [] m_FieldMappings;
    private int [] m_ByFieldMappings;
    private int [] m_OverFieldMappings;
    private int [] m_PartitionFieldMappings;

    private String [][] m_BufferedRecords;
    private long [] m_Epochs;
    private int m_BufferedRecordCount;

    private Logger m_Logger;

    public ElasticsearchJobDataPersister(String jobId, Client client, Logger logger)
    {
        m_IndexName = jobId + "_raw";
        m_Client = client;
        m_Logger = logger;

        m_BufferedRecords = new String [DOC_BUFFER_SIZE][];
        m_Epochs = new long [DOC_BUFFER_SIZE];
    }

    @Override
    public void setFieldMappings(List<String> fields,
            List<String> byFields, List<String> overFields,
            List<String> partitionFields, String [] header)
    {
        List<String> headerList = Arrays.asList(header);

        m_FieldNames = new String [fields.size()];
        m_FieldNames = fields.<String>toArray(m_FieldNames);
        m_FieldMappings = new int [fields.size()];
        m_ByFieldMappings = new int [byFields.size()];
        m_OverFieldMappings = new int [overFields.size()];
        m_PartitionFieldMappings = new int [partitionFields.size()];

        List<List<String>> allFieldTypes = Arrays.asList(fields, byFields,
                overFields, partitionFields);

        int [][] allFieldMappings = new int [][] {m_FieldMappings, m_ByFieldMappings,
                m_OverFieldMappings, m_PartitionFieldMappings};

        int i = 0;
        for (List<String> fieldType : allFieldTypes)
        {
            int j = 0;
            for (String f : fieldType)
            {
                int index = headerList.indexOf(f);
                if (index >= 0)
                {
                    allFieldMappings[i][j] = index;
                }
                else
                {
                    // not found in header
                    int [] tmp = new int [allFieldMappings[i].length -1];
                    System.arraycopy(allFieldMappings[i], 0, tmp, 0, j);
                    System.arraycopy(allFieldMappings[i], j+1, tmp, j, tmp.length - j);
                }

                j++;
            }
            i++;
        }
    }

    /**
     * The contents of <code>record</code> needs to be copied as it is
     * reused by other code
     */
    @Override
    public void persistRecord(long epoch, String [] record)
    {
        if (isIndexExisting() == false) {
            m_Logger.info("Data will be persisted in the index " + m_IndexName);
            createIndex();
        }

        String [] copy = new String[record.length];
        System.arraycopy(record, 0, copy, 0, record.length);
        m_BufferedRecords[m_BufferedRecordCount] = copy;
        m_Epochs[m_BufferedRecordCount] = epoch;

        m_BufferedRecordCount++;


        if (m_BufferedRecordCount == DOC_BUFFER_SIZE)
        {
            writeDocs();
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
        }
        catch (IOException e)
        {
            m_Logger.error("Error creating the raw data index " + m_IndexName, e);
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

        try
        {
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
                    m_Logger.error("Error creating json builder", e);
                }
            }
        }
        finally
        {
            m_BufferedRecordCount = 0;
        }


        BulkResponse bulkResponse = bulkRequest.execute().actionGet();
        if (bulkResponse.hasFailures())
        {
            m_Logger.error("Errors writing raw job data: " +
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
        m_Logger.debug("Deleting the raw data index " + m_IndexName);

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
            m_Logger.warn("Error deleting the raw data index", e);
            return false;
        }
    }
}
