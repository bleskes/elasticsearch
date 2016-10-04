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
 ***********************************************************/
package com.prelert.rs.client.integrationtests;

import com.prelert.job.JobConfiguration;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.rs.data.Pagination;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Run a job with per partition normalization enabled then query
 * results by partition field.
 */
public class PerPartitionNormalizationTest extends BaseIntegrationTest
{
    public PerPartitionNormalizationTest(String baseUrl)
    {
        super(baseUrl, true);
    }

    @Override
    protected void runTest() throws IOException
    {
        File fareQuoteData = StandardJobs.farequoteDataFile(m_TestDataHome);
        JobConfiguration config = StandardJobs.farequoteJobConfiguration();

        config.getAnalysisConfig().getDetectors().get(0).setByFieldName(null);
        config.getAnalysisConfig().getDetectors().get(0).setPartitionFieldName("airline");
        config.getAnalysisConfig().setUsePerPartitionNormalization(true);
        String jobId = m_EngineApiClient.createJob(config);
        test(jobId.isEmpty() == false);

        m_EngineApiClient.fileUpload(jobId, fareQuoteData, false);
        m_EngineApiClient.closeJob(jobId);

        verifyGetResultsByPartition(jobId);
        deleteJob(jobId);
    }

    private void verifyGetResultsByPartition(String jobId) throws IOException
    {
        checkQueryRecordsWithPartitionFieldValueResults(jobId, "AAL");
        checkQueryRecordsWithPartitionFieldValueResults(jobId, "SWA");
        checkQueryBucketsWithPartitionFieldValueResults(jobId, "JAL");
        checkQueryBucketsWithPartitionFieldValueResults(jobId, "AAL");
        checkQueryBucketsWithUnknownPartitionValue(jobId);
    }

    private void checkQueryRecordsWithPartitionFieldValueResults(String jobId,
                                            String partitionFieldValue)
            throws IOException
    {
        Pagination<AnomalyRecord> anomalyRecords = m_EngineApiClient
                .prepareGetRecords(jobId).partitionFieldValue(partitionFieldValue).take(1000l).get();

        test(anomalyRecords.getDocumentCount() > 0);
        for (AnomalyRecord record: anomalyRecords.getDocuments())
        {
            test("airline", record.getPartitionFieldName());
            test(partitionFieldValue, record.getPartitionFieldValue());
            test(record.getNormalizedProbability() < 80);
        }
    }

    private void checkQueryBucketsWithPartitionFieldValueResults(String jobId,
            String partitionFieldValue)
    throws UnsupportedEncodingException, IOException
    {
        Pagination<Bucket> buckets = m_EngineApiClient.prepareGetBuckets(jobId)
                            .partitionFieldValue(partitionFieldValue)
                            .expand(true)
                            .take(1000l).get();

        boolean atLeastOneRecordFound = false;
        for (Bucket b : buckets.getDocuments())
        {
            if (b.getRecords().isEmpty())
            {
//                test(0.0, b.getAnomalyScore());
                test(0.0, b.getMaxNormalizedProbability());
            }
            for (AnomalyRecord r : b.getRecords())
            {
                atLeastOneRecordFound = true;
                test(r.getPartitionFieldValue().equals(partitionFieldValue));
            }
        }

        test(atLeastOneRecordFound);
    }

    private void checkQueryBucketsWithUnknownPartitionValue(String jobId)
    throws UnsupportedEncodingException, IOException
    {
        Pagination<Bucket> buckets = m_EngineApiClient.prepareGetBuckets(jobId)
                            .partitionFieldValue("NO SUCH FIELD")
                            .expand(true)
                            .take(1000l).get();

        for (Bucket b : buckets.getDocuments())
        {
            test(0, b.getRecords().size());
//            test(0.0, b.getAnomalyScore());
            test(0.0, b.getMaxNormalizedProbability());
        }
    }

    public static void main(String[] args) throws IOException
    {
        String baseUrl = API_BASE_URL;
        if (args.length > 0)
        {
            baseUrl = args[0];
        }

        try (PerPartitionNormalizationTest test = new PerPartitionNormalizationTest(baseUrl))
        {
            test.runTest();

            test.m_Logger.info("Per Partition Normalization tests passed Ok");
        }
    }
}
