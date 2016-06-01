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
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.AnalysisLimits;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;


/**
 * Integration test for the categorization functionality.
 * <br>
 * The system property 'prelert.test.data.home' must be set and point
 * to a directory containing the file:
 * <ol>
 * <li>/engine_api_integration_test/categorization/part1.csv</li>
 * <li>/engine_api_integration_test/categorization/part2.csv</li>
 * </ol>
 *
 */
public class CategorizationTest implements Closeable
{
    private static final Logger LOGGER = Logger.getLogger(CategorizationTest.class);

    private static final String HIGHEST_ANOMALY_BUCKET_ID = "highestAnomalyBucketId";
    private static final String HIGHEST_ANOMALY_CATEGORY_ID = "highestAnomalyCategoryId";
    private static final String HIGHEST_ANOMALY_SCORE_THRESHOLD = "highestAnomalyScoreThreshold";
    private static final String HIGHEST_RECORD_PROBABILITY_THRESHOLD =
            "highestAnomalyRecordProbabilityThreshold";

    private static final long BUCKET_SPAN = 3600;
    private static final int EXPECTED_CATEGORIES = 48;
    private static final int DEFAULT_EXAMPLES_BY_CATEGORY_LIMIT = 4;

    private static final String COUNT_DEFAULT_EXAMPLES_LIMIT_ID =
            "categorization-test-count-default-examples-limit";
    private static final String COUNT_ZERO_EXAMPLES_LIMIT_ID =
            "categorization-test-count-zero-examples-limit";
    private static final String COUNT_FIVE_EXAMPLES_LIMIT_ID =
            "categorization-test-count-zero-examples-limit";
    private static final String RARE_DEFAULT_EXAMPLES_LIMIT_ID =
            "categorization-test-rare-default-examples-limit";

    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v2";

    private final EngineApiClient m_WebServiceClient;
    private final String m_TestDataHome;
    private final String m_BaseUrl;
    private final String m_JobId;
    private Long m_ExamplesByCategoryLimit;
    private final String m_Function;
    private final Map<String, Object> m_ExpectedResults;

    /**
     * Creates a new http client call {@linkplain #close()} once finished
     */
    public CategorizationTest(String testDataHome, String baseUrl, String jobId, String function,
            Map<String, Object> expectedResults)
    {
        m_WebServiceClient = new EngineApiClient(baseUrl);
        m_TestDataHome = testDataHome;
        m_BaseUrl = baseUrl;
        m_JobId = jobId;
        m_Function = function;
        m_ExpectedResults = expectedResults;
    }

    @Override
    public void close() throws IOException
    {
        m_WebServiceClient.close();
    }

    public void setExamplesByCategoryLimit(long limit)
    {
        m_ExamplesByCategoryLimit = limit;
    }

    private long getExamplesByCategoryLimit()
    {
        return m_ExamplesByCategoryLimit == null ? DEFAULT_EXAMPLES_BY_CATEGORY_LIMIT :
            m_ExamplesByCategoryLimit;
    }

    public void execute() throws IOException
    {
        // Always delete the test job first in case it is hanging around
        // from a previous run
        deleteJob();

        setUp();
        verifyCategoryDefinitions();
        verifyHighestAnomaly();
        verifyPaginationWorks();
        deleteJob();
    }

    private void setUp() throws IOException
    {
        createCategorizationJob();

        // We upload the data split in two parts. By closing the job after the first part,
        // we also test the persistence of the categorizer.

        File data = new File(m_TestDataHome +
                "/engine_api_integration_test/categorization/part1.csv");
        m_WebServiceClient.fileUpload(m_JobId, data, false);
        test(m_WebServiceClient.closeJob(m_JobId) == true);
        data = new File(m_TestDataHome +
                "/engine_api_integration_test/categorization/part2.csv");
        m_WebServiceClient.fileUpload(m_JobId, data, false);
        test(m_WebServiceClient.closeJob(m_JobId) == true);
    }

    private String createCategorizationJob() throws IOException
    {
        Detector d = new Detector();
        d.setFunction(m_Function);
        d.setByFieldName("prelertcategory");

        AnalysisLimits analysisLimits = new AnalysisLimits();
        analysisLimits.setCategorizationExamplesLimit(m_ExamplesByCategoryLimit);

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(BUCKET_SPAN);
        ac.setCategorizationFieldName("message");
        ac.setOverlappingBuckets(false);
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("time");
        dd.setTimeFormat("yyyy-MM-dd HH:mm:ssX");

        JobConfiguration config = new JobConfiguration(ac);
        config.setDescription("Categorization test");
        config.setId(m_JobId);
        config.setDataDescription(dd);
        config.setAnalysisLimits(analysisLimits);

        String jobId = m_WebServiceClient.createJob(config);
        if (jobId == null || jobId.isEmpty())
        {
            LOGGER.error("No Job Id returned by create job");
            test(jobId != null && jobId.isEmpty() == false);
        }
        test(m_JobId.equals(jobId));

        return jobId;
    }

    private void verifyCategoryDefinitions() throws IOException
    {
        Pagination<CategoryDefinition> categoryDefinitions = m_WebServiceClient
                .prepareGetCategoryDefinitions(m_JobId).get();
        test(categoryDefinitions.getHitCount() == EXPECTED_CATEGORIES);

        long categoryId = 1;
        int maxExamplesSize = 0;
        for (CategoryDefinition def : categoryDefinitions.getDocuments())
        {
            SingleDocument<CategoryDefinition> doc = m_WebServiceClient.getCategoryDefinition(
                    m_JobId, String.valueOf(categoryId));
            CategoryDefinition categoryDefinition = doc.getDocument();
            test(categoryDefinition.getCategoryId() == categoryId);
            test(categoryDefinition.getTerms().isEmpty() == false);
            test(categoryDefinition.getRegex().isEmpty() == false);
            if (m_ExamplesByCategoryLimit == null || m_ExamplesByCategoryLimit > 0)
            {
                int examplesSize = def.getExamples().size();
                maxExamplesSize = Math.max(maxExamplesSize, examplesSize);
                test(examplesSize > 0);
            }
            ++categoryId;
        }
        test(maxExamplesSize == getExamplesByCategoryLimit());
    }

    private void verifyHighestAnomaly() throws IOException
    {
        SingleDocument<Bucket> doc = m_WebServiceClient.prepareGetBucket(
                m_JobId, (String) m_ExpectedResults.get(HIGHEST_ANOMALY_BUCKET_ID))
                .expand(true).get();
        Bucket bucket = doc.getDocument();
        test(bucket.getAnomalyScore() >= (double) m_ExpectedResults.get(HIGHEST_ANOMALY_SCORE_THRESHOLD));
        AnomalyRecord highestRecord = null;
        for (AnomalyRecord record : bucket.getRecords())
        {
            test(record.getFunction().equals(m_Function));
            if (record.getNormalizedProbability() >=
                    (double) m_ExpectedResults.get(HIGHEST_RECORD_PROBABILITY_THRESHOLD))
            {
                highestRecord = record;
            }
        }
        test(highestRecord != null);
        if (m_ExpectedResults.containsKey(HIGHEST_ANOMALY_CATEGORY_ID))
        {
            test(highestRecord.getByFieldValue().equals(
                    m_ExpectedResults.get(HIGHEST_ANOMALY_CATEGORY_ID)));
        }
    }

    private void verifyPaginationWorks() throws IOException
    {
        Pagination<CategoryDefinition> categoryDefinitions = m_WebServiceClient
                .prepareGetCategoryDefinitions(m_JobId).take(25).get();
        test(categoryDefinitions.isAllResults() == false);
        test(categoryDefinitions.getPreviousPage() == null);
        String expectedNextPageUri =
                m_BaseUrl + "/results/" + m_JobId + "/categorydefinitions?skip=25&take=25";
        test(expectedNextPageUri.equals(categoryDefinitions.getNextPage().toString()));
    }

    private void deleteJob() throws IOException
    {
        LOGGER.debug("Deleting job " + m_JobId);

        boolean success = m_WebServiceClient.deleteJob(m_JobId);
        if (success == false)
        {
            LOGGER.error("Error deleting job " + m_BaseUrl + "/" + m_JobId);
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

        // Count tests with different examples limits
        Map<String, Object> expectedResultsForCount = new HashMap<>();
        expectedResultsForCount.put(HIGHEST_ANOMALY_BUCKET_ID, "1428678000");
        expectedResultsForCount.put(HIGHEST_ANOMALY_SCORE_THRESHOLD, 91.0);
        expectedResultsForCount.put(HIGHEST_RECORD_PROBABILITY_THRESHOLD, 75.0);
        expectedResultsForCount.put(HIGHEST_ANOMALY_CATEGORY_ID, "13");

        try (CategorizationTest test = new CategorizationTest(prelertTestDataHome, baseUrl,
                COUNT_DEFAULT_EXAMPLES_LIMIT_ID, "count", expectedResultsForCount))
        {
            test.execute();
        }

        try (CategorizationTest test = new CategorizationTest(prelertTestDataHome, baseUrl,
                COUNT_ZERO_EXAMPLES_LIMIT_ID, "count", expectedResultsForCount))
        {
            test.setExamplesByCategoryLimit(0);
            test.execute();
        }

        try (CategorizationTest test = new CategorizationTest(prelertTestDataHome, baseUrl,
                COUNT_FIVE_EXAMPLES_LIMIT_ID, "count", expectedResultsForCount))
        {
            test.setExamplesByCategoryLimit(5);
            test.execute();
        }

        // Rare tests with default examples limits
        Map<String, Object> expectedResultsForRare = new HashMap<>();
        expectedResultsForRare.put(HIGHEST_ANOMALY_BUCKET_ID, "1428678000");
        expectedResultsForRare.put(HIGHEST_ANOMALY_SCORE_THRESHOLD, 71.0);
        expectedResultsForRare.put(HIGHEST_RECORD_PROBABILITY_THRESHOLD, 10.0);

        try (CategorizationTest test = new CategorizationTest(prelertTestDataHome, baseUrl,
                RARE_DEFAULT_EXAMPLES_LIMIT_ID, "rare", expectedResultsForRare))
        {
            test.execute();
        }

        LOGGER.info("All tests passed Ok");
    }
}
