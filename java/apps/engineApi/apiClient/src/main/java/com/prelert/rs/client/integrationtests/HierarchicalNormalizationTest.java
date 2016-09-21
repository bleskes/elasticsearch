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

package com.prelert.rs.client.integrationtests;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.Pagination;


/**
 * This test is verifying that hierarchical normalisation
 * is taking place. It checks against two jobs, one configured
 * to have two detectors with different partition fields, and
 * the other having three detectors, two with the same by field
 * and one with a different by field. The test asserts that
 * the normalized probability for the records that should
 * be normalized separately is higher than a certain threshold.
 *
 * <br>
 * The system property 'prelert.test.data.home' must be set and point
 * to a directory containing the file:
 * <ol>
 * <li>/engine_api_integration_test/hierarchical_normalisation_test.csv</li>
 * </ol>
 *
 */
public class HierarchicalNormalizationTest implements Closeable
{
    private static final Logger LOGGER = Logger.getLogger(HierarchicalNormalizationTest.class);

    private static final String DIFFERENT_PARTITIONS_JOB_ID = "partition-norm-test";
    private static final String DIFFERENT_PERSONS_JOB_ID = "person-norm-test";
    private static final String DIFFERENT_PERSONS_AND_FUNCTIONS_JOB_ID = "person-function-norm-test";
    private static final String DIFFERENT_FUNCTION_FIELDS_JOB_ID = "function-fields-norm-test";

    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v2";

    private final EngineApiClient m_WebServiceClient;

    /**
     * Creates a new http client call {@linkplain #close()} once finished
     */
    public HierarchicalNormalizationTest(String baseUrl)
    {
        m_WebServiceClient = new EngineApiClient(baseUrl);
    }

    @Override
    public void close() throws IOException
    {
        m_WebServiceClient.close();
    }

    private String createDifferentPartitionsJob() throws IOException
    {
        Detector d1 = new Detector();
        d1.setFunction("mean");
        d1.setFieldName("value");
        d1.setPartitionFieldName("region");

        Detector d2 = new Detector();
        d2.setFunction("mean");
        d2.setFieldName("value");
        d2.setPartitionFieldName("instance");

        return createJob(Arrays.asList(d1, d2), DIFFERENT_PARTITIONS_JOB_ID);
    }

    private String createDifferentPersonsJob() throws IOException
    {
        Detector d1 = new Detector();
        d1.setFunction("mean");
        d1.setFieldName("value");
        d1.setByFieldName("region");

        Detector d2 = new Detector();
        d2.setFunction("mean");
        d2.setFieldName("value");
        d2.setByFieldName("instance");

        return createJob(Arrays.asList(d1, d2), DIFFERENT_PERSONS_JOB_ID);
    }

    private String createDifferentPersonsAndFunctionsJob() throws IOException
    {
        Detector d1 = new Detector();
        d1.setFunction("mean");
        d1.setFieldName("value");
        d1.setByFieldName("region");

        Detector d2 = new Detector();
        d2.setFunction("min");
        d2.setFieldName("value");
        d2.setByFieldName("region");

        Detector d3 = new Detector();
        d3.setFunction("mean");
        d3.setFieldName("value");
        d3.setByFieldName("instance");

        return createJob(Arrays.asList(d1, d2, d3), DIFFERENT_PERSONS_AND_FUNCTIONS_JOB_ID);
    }

    private String createDifferentFunctionFieldsJob() throws IOException
    {
        Detector d1 = new Detector();
        d1.setFunction("mean");
        d1.setFieldName("value");
        d1.setByFieldName("instance");

        Detector d2 = new Detector();
        d2.setFunction("mean");
        d2.setFieldName("value2");
        d2.setByFieldName("instance");

        return createJob(Arrays.asList(d1, d2), DIFFERENT_FUNCTION_FIELDS_JOB_ID);
    }

    private String createJob(List<Detector> detectors, String jobId) throws IOException
    {
        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(600L);
        ac.setDetectors(detectors);

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("time");
        dd.setTimeFormat("yyyy-MM-dd HH:mm:ssX");

        JobConfiguration config = new JobConfiguration(ac);
        config.setDescription("Partition level normalisation test");
        config.setId(jobId);
        config.setDataDescription(dd);

        String returnedJobId = m_WebServiceClient.createJob(config);
        if (returnedJobId == null || returnedJobId.isEmpty())
        {
            LOGGER.error("No Job Id returned by create job");
            test(false);
        }
        test(returnedJobId.equals(jobId));

        return returnedJobId;
    }

    private boolean verifyDifferentPartitionsJob() throws IOException
    {
        verifyConsistentMaxNormalizedProbability(DIFFERENT_PARTITIONS_JOB_ID);

        Pagination<AnomalyRecord> paginatedRecords = m_WebServiceClient.prepareGetRecords(DIFFERENT_PARTITIONS_JOB_ID)
            .sortField(AnomalyRecord.NORMALIZED_PROBABILITY)
            .descending(true)
            .take(2)
            .get();

        List<AnomalyRecord> records = paginatedRecords.getDocuments();
        test(records.size() == 2);

        AnomalyRecord record1 = records.get(0);
        test(record1.getNormalizedProbability() > 97.0);
        test(record1.getAnomalyScore() > 80.0);
        test(record1.getPartitionFieldName().equals("instance"));
        test(record1.getPartitionFieldValue().equals("US-1"));

        AnomalyRecord record2 = records.get(1);
        test(record2.getNormalizedProbability() > 82.0);
        test(record2.getAnomalyScore() == record1.getAnomalyScore());
        test(record2.getPartitionFieldName().equals("region"));
        test(record2.getPartitionFieldValue().equals("Europe"));

        return true;
    }

    private boolean verifyDifferentPersonsJob() throws IOException
    {
        verifyConsistentMaxNormalizedProbability(DIFFERENT_PERSONS_JOB_ID);

        Pagination<AnomalyRecord> paginatedRecords = m_WebServiceClient.prepareGetRecords(DIFFERENT_PERSONS_JOB_ID)
            .sortField(AnomalyRecord.NORMALIZED_PROBABILITY)
            .descending(true)
            .take(2)
            .get();

        List<AnomalyRecord> records = paginatedRecords.getDocuments();
        test(records.size() == 2);

        AnomalyRecord record1 = records.get(0);
        test(record1.getNormalizedProbability() > 97.0);
        test(record1.getAnomalyScore() > 80.0);
        test(record1.getByFieldName().equals("instance"));
        test(record1.getByFieldValue().equals("US-1"));

        AnomalyRecord record2 = records.get(1);
        test(record2.getNormalizedProbability() > 82.0);
        test(record2.getAnomalyScore() == record1.getAnomalyScore());
        test(record2.getByFieldName().equals("region"));
        test(record2.getByFieldValue().equals("Europe"));

        return true;
    }

    private boolean verifyDifferentPersonsAndFunctionsJob() throws IOException
    {
        verifyConsistentMaxNormalizedProbability(DIFFERENT_PERSONS_AND_FUNCTIONS_JOB_ID);

        Pagination<AnomalyRecord> paginatedRecords =
            m_WebServiceClient.prepareGetRecords(DIFFERENT_PERSONS_AND_FUNCTIONS_JOB_ID)
            .sortField(AnomalyRecord.NORMALIZED_PROBABILITY)
            .descending(true)
            .take(3)
            .get();

        List<AnomalyRecord> records = paginatedRecords.getDocuments();
        test(records.size() == 3);

        AnomalyRecord record1 = records.get(0);
        AnomalyRecord record2 = records.get(1);
        AnomalyRecord record3 = records.get(2);

        test(record1.getNormalizedProbability() > 97.0);
        test(record1.getAnomalyScore() > 80.0);
        test(record1.getByFieldName().equals("instance"));
        test(record1.getByFieldValue().equals("US-1"));
        test(record1.getFunction().equals("mean"));

        // The next two records have such close probabilities that tiny changes
        // in the core code can make their ordering flip, so tolerate them
        // either way around
        if (record2.getByFieldValue().equals("Europe"))
        {
            LOGGER.info("Flipping records 2 and 3");
            record2 = records.get(2);
            record3 = records.get(1);
        }

        test(record2.getNormalizedProbability() > 82.0);
        LOGGER.info("record2.getAnomalyScore() = " + record2.getAnomalyScore());
        test(record2.getAnomalyScore() > 1.5 && record2.getAnomalyScore() < 5.5);
        test(record2.getByFieldName().equals("region"));
        test(record2.getByFieldValue().equals("US"));
        test(record2.getFunction().equals("min"));

        test(record3.getNormalizedProbability() > 82.0);
        test(record3.getAnomalyScore() == record1.getAnomalyScore());
        test(record3.getByFieldName().equals("region"));
        test(record3.getByFieldValue().equals("Europe"));
        test(record3.getFunction().equals("mean"));

        return true;
    }

    private boolean verifyDifferentFunctionFieldsJob() throws IOException
    {
        verifyConsistentMaxNormalizedProbability(DIFFERENT_FUNCTION_FIELDS_JOB_ID);

        Pagination<AnomalyRecord> paginatedRecords =
            m_WebServiceClient.prepareGetRecords(DIFFERENT_FUNCTION_FIELDS_JOB_ID)
            .sortField(AnomalyRecord.NORMALIZED_PROBABILITY)
            .descending(true)
            .take(2)
            .get();

        List<AnomalyRecord> records = paginatedRecords.getDocuments();
        test(records.size() == 2);

        AnomalyRecord record1 = records.get(0);
        test(record1.getNormalizedProbability() > 97.0);
        test(record1.getAnomalyScore() > 80.0);
        test(record1.getByFieldName().equals("instance"));
        test(record1.getByFieldValue().equals("Europe-5"));
        test(record1.getFunction().equals("mean"));
        test(record1.getFieldName().equals("value2"));

        AnomalyRecord record2 = records.get(1);
        test(record2.getNormalizedProbability() > 97.0);
        test(record2.getAnomalyScore() > 50.0);
        test(record2.getByFieldName().equals("instance"));
        test(record2.getByFieldValue().equals("US-1"));
        test(record2.getFunction().equals("mean"));
        test(record2.getFieldName().equals("value"));

        return true;
    }

    private void verifyConsistentMaxNormalizedProbability(String jobId) throws IOException
    {
        Pagination<Bucket> allBucketsExpanded = m_WebServiceClient.prepareGetBuckets(jobId)
                .expand(true).take(300).get();

        for (Bucket bucket: allBucketsExpanded.getDocuments())
        {
            double bucketMax = 0.0;
            for (AnomalyRecord r : bucket.getRecords())
            {
                test(r.getAnomalyScore() == bucket.getAnomalyScore());
                bucketMax = Math.max(r.getNormalizedProbability(), bucketMax);
            }

            test(bucketMax == bucket.getMaxNormalizedProbability());
        }
    }

    /**
     * Throws an IllegalStateException if <code>condition</code> is false.
     *
     * @param condition
     * @throws IllegalStateException
     */
    public static void test(boolean condition)
    throws IllegalStateException
    {
        if (condition == false)
        {
            throw new IllegalStateException();
        }
    }

    /**
     * The program takes one argument which is the base Url of the RESTful API.
     * If no arguments are given then {@value #API_BASE_URL} is used.
     *
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args)
    throws IOException, InterruptedException
    {
        // configure log4j
        ConsoleAppender console = new ConsoleAppender();
        console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n"));
        console.setThreshold(Level.INFO);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);

        String baseUrl = API_BASE_URL;
        if (args.length > 0)
        {
            baseUrl = args[0];
        }

        LOGGER.info("Testing Service at " + baseUrl);

        final String prelertTestDataHome = System.getProperty("prelert.test.data.home");
        if (prelertTestDataHome == null)
        {
            throw new IllegalStateException("Error property prelert.test.data.home is not set");
        }


        try (HierarchicalNormalizationTest test = new HierarchicalNormalizationTest(baseUrl))
        {
            // Always delete the test job first in case it is hanging around
            // from a previous run
            test.m_WebServiceClient.deleteJob(DIFFERENT_PARTITIONS_JOB_ID);
            test.m_WebServiceClient.deleteJob(DIFFERENT_PERSONS_JOB_ID);
            test.m_WebServiceClient.deleteJob(DIFFERENT_PERSONS_AND_FUNCTIONS_JOB_ID);
            test.m_WebServiceClient.deleteJob(DIFFERENT_FUNCTION_FIELDS_JOB_ID);

            File data = new File(prelertTestDataHome
                    + "/engine_api_integration_test/hierarchical_normalisation_test.csv");

            test.createDifferentPartitionsJob();
            test.m_WebServiceClient.fileUpload(DIFFERENT_PARTITIONS_JOB_ID, data, false);
            test.m_WebServiceClient.closeJob(DIFFERENT_PARTITIONS_JOB_ID);
            test.verifyDifferentPartitionsJob();

            test.createDifferentPersonsJob();
            test.m_WebServiceClient.fileUpload(DIFFERENT_PERSONS_JOB_ID, data, false);
            test.m_WebServiceClient.closeJob(DIFFERENT_PERSONS_JOB_ID);
            test.verifyDifferentPersonsJob();

            test.createDifferentPersonsAndFunctionsJob();
            test.m_WebServiceClient.fileUpload(DIFFERENT_PERSONS_AND_FUNCTIONS_JOB_ID, data, false);
            test.m_WebServiceClient.closeJob(DIFFERENT_PERSONS_AND_FUNCTIONS_JOB_ID);
            test.verifyDifferentPersonsAndFunctionsJob();

            test.createDifferentFunctionFieldsJob();
            test.m_WebServiceClient.fileUpload(DIFFERENT_FUNCTION_FIELDS_JOB_ID, data, false);
            test.m_WebServiceClient.closeJob(DIFFERENT_FUNCTION_FIELDS_JOB_ID);
            test.verifyDifferentFunctionFieldsJob();

            //==========================
            // Clean up test jobs
            test(test.m_WebServiceClient.deleteJob(DIFFERENT_PARTITIONS_JOB_ID));
            test(test.m_WebServiceClient.deleteJob(DIFFERENT_PERSONS_JOB_ID));
            test(test.m_WebServiceClient.deleteJob(DIFFERENT_PERSONS_AND_FUNCTIONS_JOB_ID));
            test(test.m_WebServiceClient.deleteJob(DIFFERENT_FUNCTION_FIELDS_JOB_ID));
        }

        LOGGER.info("All tests passed Ok");
    }
}
