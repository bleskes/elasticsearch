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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.results.Bucket;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;


/**
 * Tests that interim results get updated correctly
 */
public class UpdateInterimResultsTest implements Closeable
{
    private static final Logger LOGGER = Logger.getLogger(UpdateInterimResultsTest.class);

    private static final long BUCKET_SPAN = 1000;

    private static final int LATENCY_BUCKETS = 0;

    static final String TEST_JOB_ID = "update-interim-test";

    /**
     * The default base URL used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v2";

    private final EngineApiClient m_WebServiceClient;
    private final String m_BaseUrl;

    public UpdateInterimResultsTest(String baseUrl)
    {
        m_WebServiceClient = new EngineApiClient(baseUrl);
        m_BaseUrl = baseUrl;
    }

    @Override
    public void close() throws IOException
    {
        m_WebServiceClient.close();
    }

    public String createTestJob() throws IOException
    {
        Detector d = new Detector();
        d.setFunction("max");
        d.setFieldName("value");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(BUCKET_SPAN);
        ac.setLatency(LATENCY_BUCKETS * BUCKET_SPAN);
        ac.setDetectors(Arrays.asList(d));
        ac.setOverlappingBuckets(true);

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("_time");

        JobConfiguration config = new JobConfiguration(ac);
        config.setDescription("Update interim results test");
        config.setId(TEST_JOB_ID);
        config.setDataDescription(dd);

        String jobId = m_WebServiceClient.createJob(config);
        if (jobId == null || jobId.isEmpty())
        {
            LOGGER.error("No Job Id returned by create job");
            test(jobId != null && jobId.isEmpty() == false);
        }
        test(jobId.equals(TEST_JOB_ID));

        return jobId;
    }


    /**
     * Delete all the jobs in the list of job ids
     *
     * @param jobIds The list of ids of the jobs to delete
     * @throws IOException
     * @throws InterruptedException
     */
    public void deleteJobs(List<String> jobIds)
    throws IOException, InterruptedException
    {
        for (String jobId : jobIds)
        {
            LOGGER.debug("Deleting job " + jobId);

            boolean success = m_WebServiceClient.deleteJob(jobId);
            if (success == false)
            {
                LOGGER.error("Error deleting job " + m_BaseUrl + "/" + jobId);
            }
        }
    }

    public void runTestBody() throws IllegalStateException, IOException
    {
        // Play some data, close job, verify no interim results
        playData(dataChunk1);
        closeJob();
        verifyNoInterimResults();

        // Play some data, close job, verify no interim results
        playData(dataChunk2);
        closeJob();
        verifyNoInterimResults();

        // Play some data up to a 1/4 bucket boundary, flush (with interim), check interim results
        playData(dataChunk3);
        flushJob();
        List<Bucket> firstInterimBuckets = interimBuckets();
        LOGGER.debug("Got " + firstInterimBuckets.size() + " interim buckets");
        test(firstInterimBuckets.get(0).getEpoch() == 1400043000l);
        test(firstInterimBuckets.get(1).getEpoch() == 1400044000l);
        test(firstInterimBuckets.get(1).getRecordCount() == 1);
        test(firstInterimBuckets.get(1).getRecords().get(0).getActual()[0] == 16.0);

        // Play in 1 more record, flush (with interim), check same interim result
        playData(dataChunk4);
        flushJob();

        List<Bucket> secondInterimBuckets = interimBuckets();
        LOGGER.debug("Got " + secondInterimBuckets.size() + " interim buckets");
        test(secondInterimBuckets.get(0).getEpoch() == 1400043000l);
        test(secondInterimBuckets.get(1).getEpoch() == 1400044000l);
        test(secondInterimBuckets.get(1).getRecordCount() == 1);
        test(secondInterimBuckets.get(1).getRecords().get(0).getActual()[0] == 16.0);

        // Play in rest of data, close, verify no interim results
        playData(dataChunk5);
        closeJob();
        verifyNoInterimResults();

        // Verify interim results have been replaced with finalized results
        SingleDocument<Bucket> bucket = m_WebServiceClient.prepareGetBucket(TEST_JOB_ID, "1400043500").
                            expand(true).includeInterim(false).get();
        test(bucket.isExists());
        LOGGER.debug("RecordCount: " + bucket.getDocument().getRecordCount());
        test(bucket.getDocument().getRecordCount() == 1);
        test(bucket.getDocument().getRecords().get(0).getActual()[0] == 14.0);
    }

    private void playData(String data) throws IOException
    {
        InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        m_WebServiceClient.streamingUpload(TEST_JOB_ID, stream, false);
    }

    private void closeJob() throws IllegalStateException, IOException
    {
        test(m_WebServiceClient.closeJob(TEST_JOB_ID) == true);
    }

    private void flushJob() throws IllegalStateException, IOException
    {
        test(m_WebServiceClient.flushJob(TEST_JOB_ID, true) == true);
    }

    private void verifyNoInterimResults() throws IOException
    {
        Pagination<Bucket> bucketsWithInterim = m_WebServiceClient.prepareGetBuckets(TEST_JOB_ID)
                .includeInterim(true).take(1500).get();

        Pagination<Bucket> bucketsWithoutInterim = m_WebServiceClient.prepareGetBuckets(TEST_JOB_ID)
                .includeInterim(false).take(1500).get();

        LOGGER.debug("Doc count with: " + bucketsWithInterim.getDocumentCount() + ", hits: " +
                bucketsWithInterim.getHitCount());
        LOGGER.debug("Doc count without: " + bucketsWithoutInterim.getDocumentCount() + ", hits: " +
                bucketsWithoutInterim.getHitCount());

        test(bucketsWithInterim.getDocumentCount() == bucketsWithoutInterim.getDocumentCount());
        test(bucketsWithInterim.getHitCount() == bucketsWithoutInterim.getHitCount());

    }

    private List<Bucket> interimBuckets() throws IOException
    {
        List<Bucket> pagedBuckets = new ArrayList<>();
        long skip = 00, take = 100;
        boolean cont = true;
        while (cont)
        {
            Pagination<Bucket> buckets = m_WebServiceClient.prepareGetBuckets(TEST_JOB_ID)
                    .expand(true).includeInterim(true).skip(skip).take(take).get();
            for (Bucket bucket : buckets.getDocuments())
            {
                if (bucket.isInterim())
                {
                    pagedBuckets.add(bucket);
                }
            }

            skip += take;
            if (buckets.getDocumentCount() == 0)
            {
                cont = false;
            }
        }
        return pagedBuckets;
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
     * @throws ExecutionException
     */
    public static void main(String[] args)
    throws IOException, InterruptedException, ExecutionException
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

        LOGGER.info("Testing the service at " + baseUrl);

        List<String> jobUrls = new ArrayList<>();
        try (UpdateInterimResultsTest test = new UpdateInterimResultsTest(baseUrl))
        {
            // Always delete the test job first in case it is hanging around
            // from a previous run
            test.m_WebServiceClient.deleteJob(TEST_JOB_ID);
            jobUrls.add(TEST_JOB_ID);

            test.createTestJob();
            test.runTestBody();

            // Clean up test jobs
            UpdateInterimResultsTest.test(test.m_WebServiceClient.closeJob(TEST_JOB_ID) == true);
            //test.deleteJobs(jobUrls);
        }

        LOGGER.info("All tests passed Ok");
    }

    private static final String dataChunk1 =
            "_time,value\n"
                    + "1400000000,2\n"
                    + "1400000500,1\n"
                    + "1400001000,2\n"
                    + "1400001500,3\n"
                    + "1400002000,2\n"
                    + "1400002500,2\n"
                    + "1400003000,3\n"
                    + "1400003500,2\n"
                    + "1400004000,1\n"
                    + "1400004500,2\n"
                    + "1400005000,2\n"
                    + "1400005500,1\n"
                    + "1400006000,3\n"
                    + "1400006500,2\n"
                    + "1400007000,3\n"
                    + "1400007500,1\n"
                    + "1400008000,1\n"
                    + "1400008500,3\n"
                    + "1400009000,1\n"
                    + "1400009500,3\n"
                    + "1400010000,2\n"
                    + "1400010500,3\n"
                    + "1400011000,2\n"
                    + "1400011500,1\n"
                    + "1400012000,2\n"
                    + "1400012500,1\n"
                    + "1400013000,2\n"
                    + "1400013500,1\n"
                    + "1400014000,3\n"
                    + "1400014500,2\n"
                    + "1400015000,3\n"
                    + "1400015500,2\n"
                    + "1400016000,3\n"
                    + "1400016500,1\n"
                    + "1400017000,2\n"
                    + "1400017500,1\n"
                    + "1400018000,2\n"
                    + "1400018500,3\n"
                    + "1400019000,2\n"
                    + "1400019500,2\n"
                    + "1400020000,3\n"
                    + "1400020500,2\n"
                    + "1400021000,1\n"
                    + "1400021500,14\n"
                    + "1400022000,3\n"
                    + "1400022500,2\n"
                    + "1400023000,1\n"
                    + "1400023500,3\n"
                    + "1400024000,3\n"
                    + "1400024500,2\n";

    private static final String dataChunk2 =
            "_time,value\n"
                    + "1400025000,3\n"
                    + "1400025500,2\n"
                    + "1400026000,1\n"
                    + "1400026500,1\n"
                    + "1400027000,2\n"
                    + "1400027500,3\n"
                    + "1400028000,2\n"
                    + "1400028500,1\n"
                    + "1400029000,2\n"
                    + "1400029500,3\n"
                    + "1400030000,2\n"
                    + "1400030500,1\n"
                    + "1400031000,2\n"
                    + "1400031500,3\n"
                    + "1400032000,2\n"
                    + "1400032500,2\n"
                    + "1400033000,3\n"
                    + "1400033500,2\n"
                    + "1400034000,1\n"
                    + "1400034500,2\n"
                    + "1400035000,2\n"
                    + "1400035500,1\n"
                    + "1400036000,3\n"
                    + "1400036500,2\n"
                    + "1400037000,3\n"
                    + "1400037500,1\n"
                    + "1400038000,1\n"
                    + "1400038500,3\n"
                    + "1400039000,1\n"
                    + "1400039500,3\n";

    private static final String dataChunk3 =
            "_time,value\n"
                    + "1400040000,2\n"
                    + "1400040500,3\n"
                    + "1400041000,2\n"
                    + "1400041500,1\n"
                    + "1400042000,2\n"
                    + "1400042500,1\n"
                    + "1400043000,2\n"
                    + "1400043500,1\n"
                    + "1400044000,14\n"
                    + "1400044500,12\n"
                    + "1400044510,16\n";

    private static final String dataChunk4 =
            "_time,value\n"
                    + "1400044520,15\n";

    private static final String dataChunk5 =
            "_time,value\n"
                    + "1400045000,3\n"
                    + "1400045500,2\n"
                    + "1400046000,3\n"
                    + "1400046500,1\n"
                    + "1400047000,2\n"
                    + "1400047500,1\n"
                    + "1400048000,2\n"
                    + "1400048500,3\n"
                    + "1400049000,2\n"
                    + "1400049500,2\n"
                    + "1400050000,3\n"
                    + "1400050500,2\n"
                    + "1400051000,1\n"
                    + "1400051500,1\n"
                    + "1400052000,3\n"
                    + "1400052500,2\n"
                    + "1400053000,1\n"
                    + "1400053500,3\n"
                    + "1400054000,3\n"
                    + "1400054500,2\n"
                    + "1400055000,3\n"
                    + "1400055500,2\n"
                    + "1400056000,3\n"
                    + "1400056500,1\n"
                    + "1400057000,2\n"
                    + "1400057500,3\n"
                    + "1400058000,2\n"
                    + "1400058500,1\n"
                    + "1400059000,2\n";
}
