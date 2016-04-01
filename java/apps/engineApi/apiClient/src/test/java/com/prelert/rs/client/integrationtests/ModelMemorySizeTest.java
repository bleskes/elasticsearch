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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.ModelSnapshot;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

/**
 * Tests the modelSizeStats in combination with restoring snapshots.
 * <br>
 * The system property 'prelert.test.data.home' must be set and point
 * to a directory containing the file:
 * <ol>
 * <li>/engine_api_integration_test/XXXXXXXX FIXME </li>
 * </ol>
 *
 */

public class ModelMemorySizeTest implements Closeable
{
    private static final Logger LOGGER = Logger.getLogger(ModelMemorySizeTest.class);

    private static final long BUCKET_SPAN = 3600;

    static final String TEST_JOB_ID = "modelmemorystats_test";
    static final String TEST_JOB_ID_PARTS = "modelmemorystats_parts_test";


    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v2";

    public static final String TEST_DATA_FILE = "/engine_api_integration_test/gallery/gallery2015_sorted.csv";

    private final EngineApiClient m_WebServiceClient;
    private final String m_BaseUrl;

    /**
     * Creates a new http client call {@linkplain #close()} once finished
     */
    public ModelMemorySizeTest(String baseUrl)
    {
        m_WebServiceClient = new EngineApiClient(baseUrl);
        m_BaseUrl = baseUrl;
    }

    @Override
    public void close() throws IOException
    {
        m_WebServiceClient.close();
    }

    public String createJob(String jobId) throws IOException
    {
        Detector d1 = new Detector();
        d1.setFunction("rare");
        d1.setByFieldName("status");
        d1.setOverFieldName("clientip");
        d1.setDetectorDescription("Rare status code");

        Detector d2 = new Detector();
        d2.setFunction("freq_rare");
        d2.setByFieldName("uri");
        d2.setOverFieldName("clientip");
        d2.setDetectorDescription("Freq rare URI");

        Detector d3 = new Detector();
        d3.setFunction("high_count");
        d3.setByFieldName("status");
        d3.setOverFieldName("clientip");
        d3.setDetectorDescription("High count status code");

        Detector d4 = new Detector();
        d4.setFunction("high_count");
        d4.setByFieldName("uri");
        d4.setOverFieldName("clientip");
        d4.setDetectorDescription("High count URI");

        Detector d5 = new Detector();
        d5.setFunction("sum");
        d5.setFieldName("bytes");
        d5.setByFieldName("method");
        d5.setDetectorDescription("Unusual bytes");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(BUCKET_SPAN);
        ac.setDetectors(Arrays.asList(d1, d5));
        ac.setInfluencers(Arrays.asList("clientip", "status", "uri"));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("time");
        dd.setTimeFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        JobConfiguration config = new JobConfiguration(ac);
        config.setDescription("ModelMemoryStats test");
        config.setId(jobId);
        config.setDataDescription(dd);

        String createdId = m_WebServiceClient.createJob(config);
        if (createdId == null || createdId.isEmpty())
        {
            LOGGER.error("No Job Id returned by create job");
            test(createdId != null && createdId.isEmpty() == false);
        }
        test(createdId.equals(jobId));

        return createdId;
    }

    public void uploadWholeFile(String jobId, String prefix) throws IOException
    {
        File f = new File(prefix + TEST_DATA_FILE);
        m_WebServiceClient.fileUpload(jobId, f, false);
    }

    public void uploadInParts(String jobId, String prefix) throws IOException
    {
        // Expect 512022 lines in the file
        long numLines = 0;
        String header = null;
        try (BufferedReader br = new BufferedReader(new FileReader(prefix + TEST_DATA_FILE)))
        {
            String line;
            while ((line = br.readLine()) != null) {
                if (numLines++ == 0)
                {
                    header = line;
                }
            }
        }
        catch (IOException ex)
        {
            LOGGER.error(ex.getMessage());
            test(false);
        }

        test(numLines == 512022L);
        test(!header.isEmpty());

        uploadLines(jobId, prefix, null, 0L, 10000L);
        test(m_WebServiceClient.closeJob(jobId));
        Long stage1Size = getModelBytes(jobId);
        // Sanity check that something has registered
        test(stage1Size > 1L);

        uploadLines(jobId, prefix, header, 10000L, 10001L);
        test(m_WebServiceClient.closeJob(jobId));
        Long stage2Size = getModelBytes(jobId);
        // Sanity check that something has registered
        test(stage2Size > 1L);

        LOGGER.debug("Stage 1: " + stage1Size + ", stage 2: " + stage2Size);
        // The two values should be close
        testSizes(stage1Size, stage2Size, 25);

        uploadLines(jobId, prefix, header, 10001L, 100000L);
        test(m_WebServiceClient.closeJob(jobId));
        Long stage3Size = getModelBytes(jobId);
        // Sanity check that something has registered
        test(stage3Size > 1L);

        uploadLines(jobId, prefix, header, 100000L, 400000L);
        test(m_WebServiceClient.closeJob(jobId));
        Long stage4Size = getModelBytes(jobId);
        // Sanity check that something has registered
        test(stage4Size > 1L);

        // Snapshots are at 'http://localhost:8080/engine/v2/modelsnapshots/modelmemorystats_parts_-test?sort=description'

        // Restore to 2015-01-06T14:51:29.000+0000 (record 10001L)
        // For some utterly bizarre reason you can't pass a time value that the API returns
        // as a parameter to ?start=<time> for results queries...

        // delete records first of course, if this were possible via the API...
        restoreTo(jobId, 1420555889000L);

        Long restoredStage2 = getModelBytes(jobId);
        LOGGER.debug("After restoring snapshot, size is " + restoredStage2);
        test(restoredStage2 > 1L);
        testSizes(restoredStage2, stage2Size, 15);

        uploadLines(jobId, prefix, header, 10001L, 100000L);
        test(m_WebServiceClient.closeJob(jobId));
        Long restoredStage3Size = getModelBytes(jobId);
        // Sanity check that something has registered
        test(restoredStage3Size > 1L);
        testSizes(restoredStage3Size, stage3Size, 15);

        restoreTo(jobId, 1420555889000L);
        uploadLines(jobId, prefix, header, 10001L, 512022L);
        test(m_WebServiceClient.closeJob(jobId));
        Long endOfJobSize = getModelBytes(jobId);

        restoreTo(jobId, 1429348655000L);
        Long restoredStage4Size = getModelBytes(jobId);
        test(restoredStage4Size > 1L);
        testSizes(restoredStage4Size, stage4Size, 15);

        uploadLines(jobId, prefix, header, 100000L, 512022L);
        test(m_WebServiceClient.closeJob(jobId));
        Long endOfJobSize2 = getModelBytes(jobId);
        testSizes(endOfJobSize2, endOfJobSize, 15);

        Long uninterruptedSize = getModelBytes(TEST_JOB_ID);
        testSizes(uninterruptedSize, endOfJobSize2, 15);
    }

    private void testSizes(Long s1, Long s2, int percentageDifference)
    {
        Long difference = Math.abs(s1 - s2);
        LOGGER.debug("Testing sizes " + s1 + " & " + s2);
        test(100.0 * difference / Math.min(s1, s2) < percentageDifference);
    }

    private void deleteResultsAfter(String jobId, Long lastRecordTime) throws JsonParseException, JsonMappingException, IOException
    {
        LOGGER.warn("Not deleting results after " + lastRecordTime + " - FIXME!");
    }

    private void restoreTo(String jobId, Long lastRecordTime) throws JsonParseException, JsonMappingException, IOException
    {
        String url = m_BaseUrl + "/modelsnapshots/" + jobId;
        Pagination<ModelSnapshot> pagedSnapshots = m_WebServiceClient.get(url, new TypeReference<Pagination<ModelSnapshot>>() {}, true);
        List<ModelSnapshot> snapshots = pagedSnapshots.getDocuments();
        LOGGER.debug("Asking for snapshot at time " + lastRecordTime);

        for (ModelSnapshot ss : snapshots)
        {
            if (ss.getLatestRecordTimeStamp().getTime() == lastRecordTime)
            {
                deleteResultsAfter(jobId, lastRecordTime);
                String fullUrl = url + "/revert?snapshotId=" + ss.getSnapshotId();
                SingleDocument<ModelSnapshot> restored = m_WebServiceClient.post(fullUrl, new TypeReference<SingleDocument<ModelSnapshot>>() {});
                test(restored.isExists());
                //curl -X POST 'http://localhost:8080/engine/v2/modelsnapshots/kpi_measure/revert?snapshotId=1234567890'
                return;
            }
        }
        test(false);
    }

    private Long getModelBytes(String jobId) throws IOException
    {
        SingleDocument<JobDetails> job = m_WebServiceClient.getJob(jobId);
        test(job.isExists());
        JobDetails jb = job.getDocument();
        return jb.getModelSizeStats().getModelBytes();
    }

    private void uploadLines(String jobId, String prefix, String header, Long startLine, Long endLine) throws IOException
    {
        StringBuffer sb = new StringBuffer();
        Long numLines = 0L;
        boolean firstRecord = true;
        if (header != null)
        {
            sb.append(header);
            sb.append("\n");
        }
        try (BufferedReader br = new BufferedReader(new FileReader(prefix + TEST_DATA_FILE)))
        {
            String line;
            while ((line = br.readLine()) != null && numLines < endLine)
            {
                if (numLines >= startLine)
                {
                    if (firstRecord)
                    {
                        LOGGER.debug("UploadLines, starting with " + line);
                        firstRecord = false;
                    }
                    sb.append(line);
                    sb.append("\n");
                }
                numLines++;
            }
        }
        catch (IOException ex)
        {
            LOGGER.error(ex.getMessage());
            test(false);
        }
        LOGGER.debug("About to upload bytes: " + sb.length());
        BufferedInputStream is = new BufferedInputStream(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        m_WebServiceClient.streamingUpload(jobId, is, false);
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

        LOGGER.warn("Enable this test when EL16-335 has been implemented!");

        /*
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

        List<String> jobUrls = new ArrayList<>();
        try (ModelMemorySizeTest test = new ModelMemorySizeTest(baseUrl))
        {
            // Always delete the test job first in case it is hanging around
            // from a previous run
            test.m_WebServiceClient.deleteJob(TEST_JOB_ID);
            test.m_WebServiceClient.deleteJob(TEST_JOB_ID_PARTS);
            jobUrls.add(TEST_JOB_ID);
            jobUrls.add(TEST_JOB_ID_PARTS);

            test.createJob(TEST_JOB_ID);
            test.uploadWholeFile(TEST_JOB_ID, prelertTestDataHome);
            ModelMemorySizeTest.test(test.m_WebServiceClient.closeJob(TEST_JOB_ID) == true);

            test.createJob(TEST_JOB_ID_PARTS);
            test.uploadInParts(TEST_JOB_ID_PARTS, prelertTestDataHome);

            ModelMemorySizeTest.test(test.m_WebServiceClient.closeJob(TEST_JOB_ID_PARTS) == true);

            //==========================
            // Clean up test jobs
            test.deleteJobs(jobUrls);
        }
        */

        LOGGER.info("All tests passed Ok");
    }
}
