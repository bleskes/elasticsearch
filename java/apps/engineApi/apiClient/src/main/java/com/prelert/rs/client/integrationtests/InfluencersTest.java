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
package com.prelert.rs.client.integrationtests;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.alert.Alert;
import com.prelert.job.alert.AlertType;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketInfluencer;
import com.prelert.job.results.Influence;
import com.prelert.job.results.Influencer;
import com.prelert.rs.client.AlertRequestBuilder;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.client.integrationtests.alertpoll.PollAlertService;
import com.prelert.rs.data.Pagination;


/**
 * These tests used the synthetic data in PRELERT_NAS/test_data/influence/security_story1.
 *
 * The nature and times of the anomalies are known so the results are checked
 * against the expected results.
 *
 * <br>Returns a non-zero value if the tests fail.
 */
public class InfluencersTest implements Closeable
{
    private static final Logger LOGGER = Logger.getLogger(InfluencersTest.class);

    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v2";

    private static final String FIREWALL_JOB = "firewall";
    private static final String SSH_AUTH_JOB = "ssh-auth";
    private static final String WEB_LOGS_JOB = "web-logs";
    private static final String BLUECOAT_LOGS_JOB = "bluecoat-logs";
    private static final String STATUS_CODES_RATES_JOB = "5xx_status_code_rates";

    private final EngineApiClient m_WebServiceClient;
    private final PollAlertService m_PollAlertService;
    private final List<String> m_JobIds;

    /**
     * Creates a new http client
     */
    public InfluencersTest(String baseUrl)
    {
        m_WebServiceClient = new EngineApiClient(baseUrl);
        m_PollAlertService = new PollAlertService();
        m_JobIds = new ArrayList<>();
    }

    /**
     * firewall.log  contains TCP traffic logs: dst_ip, dst_port, src_ip, src_port
     *
     * At time 2015-01-23T06:00:00.000+0000 there is an anomaly where "10.2.3.4"
     * gets a large number of connections. Our attacker is scanning the network,
     * and one host in particular. The influencer is src_ip = 23.28.243.150
     *
     * @param filePath directory containing firewall.log
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void doFirewallJob(String filePath) throws IOException, InterruptedException, ExecutionException
    {
        m_WebServiceClient.deleteJob(FIREWALL_JOB);

        Detector d = new Detector();
        d.setFunction("dc");
        d.setFieldName("dst_port");
        d.setOverFieldName("dst_ip");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(3600L);
        ac.setInfluencers(Arrays.asList("src_ip"));
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("_time");
        dd.setTimeFormat("epoch");

        JobConfiguration config = new JobConfiguration(ac);
        config.setId(FIREWALL_JOB);
        config.setDataDescription(dd);

        createJob(config);

        File data = new File(filePath, "firewall.log");


        // write the header to start the job
        BufferedReader reader = new BufferedReader(new FileReader(data));
        String header = reader.readLine();

        ByteArrayInputStream is = new ByteArrayInputStream(header.getBytes(StandardCharsets.UTF_8));
        m_WebServiceClient.streamingUpload(FIREWALL_JOB, is, false);
        reader.close();

        final double score = 10.0;

        // Alert on any influencer result
        AlertRequestBuilder requestBuilder = new
                AlertRequestBuilder(m_WebServiceClient, FIREWALL_JOB)
                                    .alertOnBucketInfluencers()
                                    .timeout(10l)
                                    .score(score);



        Future<Alert> alertFuture = m_PollAlertService.longPoll(requestBuilder);

        m_WebServiceClient.fileUpload(FIREWALL_JOB, data, false);
        m_WebServiceClient.closeJob(FIREWALL_JOB);

        Alert alert = alertFuture.get();
        test(alert.isTimeout() == false);
        test(alert.isInterim() == false);
        test(alert.getAlertType() == AlertType.BUCKETINFLUENCER);
        test(alert.getBucket().getBucketInfluencers().size() > 0);
        for (BucketInfluencer bi : alert.getBucket().getBucketInfluencers())
        {
            test(bi.getAnomalyScore() >= score);
        }


        Pagination<AnomalyRecord> records = m_WebServiceClient.prepareGetRecords(FIREWALL_JOB).get();

        AnomalyRecord record = records.getDocuments().get(0);

        test(record.getInfluencers().size() == 1);
        test(record.getInfluencers().get(0).getInfluencerFieldName().equals("src_ip"));
        test(record.getInfluencers().get(0).getInfluencerFieldValues().size() == 1);
        test(record.getInfluencers().get(0).getInfluencerFieldValues().get(0).equals("23.28.243.150"));

        List<Influencer> influencers = m_WebServiceClient.prepareGetInfluencers(FIREWALL_JOB).get()
                .getDocuments();
        test(influencers.size() == 1);
        test(influencers.get(0).isInterim() == false);
        test(influencers.get(0).getInfluencerFieldName().equals("src_ip"));
        test(influencers.get(0).getInfluencerFieldValue().equals("23.28.243.150"));
        test(influencers.get(0).getTimestamp().equals(new Date(1421992800000L)));
        test(influencers.get(0).getAnomalyScore() > 85.0);
    }

    /**
     * pam_authd.log  contains SSH authentication messages for all users,
     * either a success or error message.
     *
     * At time 2015-01-23T08:00:00.000+0000 there is an anomaly where user "nigella"
     * makes an unsually large number of failed login attempts. Our attacker cracks
     * the password for user "nigella". The influencer src_ip = 23.28.243.150
     *
     * @param filePath
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void doAuthDJob(String filePath) throws IOException, InterruptedException, ExecutionException
    {
        m_WebServiceClient.deleteJob(SSH_AUTH_JOB);

        Detector d = new Detector();
        d.setFunction("high_count");
        d.setByFieldName("message");
        d.setOverFieldName("user");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(3600L);
        ac.setInfluencers(Arrays.asList("src_ip"));
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("_time");
        dd.setTimeFormat("epoch");

        JobConfiguration config = new JobConfiguration(ac);
        config.setId(SSH_AUTH_JOB);
        config.setDataDescription(dd);

        createJob(config);


        File data = new File(filePath, "pam_authd.log");

        // write the header to start the job
        BufferedReader reader = new BufferedReader(new FileReader(data));
        String header = reader.readLine();

        ByteArrayInputStream is = new ByteArrayInputStream(header.getBytes(StandardCharsets.UTF_8));
        m_WebServiceClient.streamingUpload(SSH_AUTH_JOB, is, false);
        reader.close();

        final double score = 10.0;

        // Alert on any influencer result
        AlertRequestBuilder requestBuilder = new
                AlertRequestBuilder(m_WebServiceClient, SSH_AUTH_JOB)
                                    .alertOnInfluencers()
                                    .timeout(10l)
                                    .score(score);



        Future<Alert> alertFuture = m_PollAlertService.longPoll(requestBuilder);

        m_WebServiceClient.fileUpload(SSH_AUTH_JOB, data, false);
        m_WebServiceClient.closeJob(SSH_AUTH_JOB);

        Alert alert = alertFuture.get();
        test(alert.isTimeout() == false);
        test(alert.getAlertType() == AlertType.INFLUENCER);
        test(alert.getBucket().getInfluencers().size() > 0);
        for (Influencer inf : alert.getBucket().getInfluencers())
        {
            test(inf.getAnomalyScore() >= score);
        }


        Pagination<AnomalyRecord> records = m_WebServiceClient.prepareGetRecords(SSH_AUTH_JOB).get();

        AnomalyRecord record = records.getDocuments().get(0);

        test(record.getOverFieldValue().equals("nigella"));
        test(record.getInfluencers().size() == 1);
        test(record.getInfluencers().get(0).getInfluencerFieldName().equals("src_ip"));
        test(record.getInfluencers().get(0).getInfluencerFieldValues().size() == 1);
        test(record.getInfluencers().get(0).getInfluencerFieldValues().get(0).equals("23.28.243.150"));

        List<Influencer> influencers = m_WebServiceClient.prepareGetInfluencers(SSH_AUTH_JOB).get()
                .getDocuments();
        test(influencers.size() == 1);
        test(influencers.get(0).isInterim() == false);
        test(influencers.get(0).getInfluencerFieldName().equals("src_ip"));
        test(influencers.get(0).getInfluencerFieldValue().equals("23.28.243.150"));
        test(influencers.get(0).getTimestamp().equals(new Date(1422172800000L)));
        test(influencers.get(0).getAnomalyScore() > 97.0);
    }


    /**
     * intranet_server.log  contains HTTP web server logs
     *
     * At time 2015-01-27T10:40:00.000+0000 there is a large number of rare URLs
     * being accessed. Our attacker, as "nigella", grabs as many documents from
     * the intranet server as he can find.
     *
     * The influencers are [(src_machine, 10.2.20.200), (user, nigella)]
     *
     * @param filePath
     * @throws IOException
     */
    private void doServerLogsJob(String filePath) throws IOException
    {
        m_WebServiceClient.deleteJob(WEB_LOGS_JOB);

        Detector d = new Detector();
        d.setFunction("rare");
        d.setByFieldName("request");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(600L);
        ac.setInfluencers(Arrays.asList("user", "src_machine"));
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("_time");
        dd.setTimeFormat("epoch");

        JobConfiguration config = new JobConfiguration(ac);
        config.setId(WEB_LOGS_JOB);
        config.setDataDescription(dd);

        createJob(config);

        File data = new File(filePath, "intranet_server.log");
        m_WebServiceClient.fileUpload(WEB_LOGS_JOB, data, false);
        m_WebServiceClient.closeJob(WEB_LOGS_JOB);

        Pagination<AnomalyRecord> records = m_WebServiceClient.prepareGetRecords(WEB_LOGS_JOB).get();

        AnomalyRecord record = records.getDocuments().get(0);


        test(record.getInfluencers().size() == 2);

        Influence user = new Influence("user");
        user.addInfluenceFieldValue("nigella");
        test(record.getInfluencers().indexOf(user) >= 0);

        Influence src = new Influence("src_machine");
        src.addInfluenceFieldValue("10.2.20.200");
        test(record.getInfluencers().indexOf(user) >= 0);

        Pagination<Influencer> pagination = m_WebServiceClient.prepareGetInfluencers(WEB_LOGS_JOB).get();
        test(pagination.getHitCount() > 100);
        List<Influencer> influencers = pagination.getDocuments();
        test(influencers.size() == 100);

        for (int i = 0; i < influencers.size(); i++)
        {
            Influencer influencer = influencers.get(i);
            test(influencer.isInterim() == false);

            if (influencer.getInfluencerFieldName().equals("user"))
            {
                test(influencer.getInfluencerFieldValue().equals("nigella"));
            }
            else if (influencer.getInfluencerFieldName().equals("src_machine"))
            {
                test(influencer.getInfluencerFieldValue().equals("10.2.20.200"));
            }
            else
            {
                test(false);
            }

            if (i + 1 < influencers.size())
            {
                test(influencer.getAnomalyScore() >= influencers.get(i + 1).getAnomalyScore());
            }
        }

        // Test filtering based on anomalyScore
        test(m_WebServiceClient.prepareGetInfluencers(WEB_LOGS_JOB)
                .anomalyScoreThreshold(80.0)
                .get()
                .getHitCount() == 2);
    }

    /**
     * proxy_bluecoat.log  contains details of HTTP connections over the corporate proxy.
     *
     * At times 2015-01-31T14:00:00.000+0000, 2015-02-02T16:00:00.000+0000,
     * 2015-02-04T18:00:00.000+0000 and 2015-02-06T20:00:00.000+0000 our
     * attacker uploads an unusually large number of bytes to a remote server -
     * exfiltrating the documents he got from the intranet server.
     *
     * The influencer is [(src_ip, 10.2.20.200), (user, nigella)]
     *
     * @param filePath
     * @throws IOException
     */
    private void doBluecoatLogsJob(String filePath) throws IOException
    {
        m_WebServiceClient.deleteJob(BLUECOAT_LOGS_JOB);

        Detector d = new Detector();
        d.setFunction("high_sum");
        d.setFieldName("bytes");
        d.setByFieldName("target");
        d.setExcludeFrequent("true");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(3600L);
        ac.setInfluencers(Arrays.asList("user", "src_ip"));
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("_time");
        dd.setTimeFormat("epoch");

        JobConfiguration config = new JobConfiguration(ac);
        config.setId(BLUECOAT_LOGS_JOB);
        config.setDataDescription(dd);

        createJob(config);

        File data = new File(filePath, "proxy_bluecoat.log");
        m_WebServiceClient.fileUpload(BLUECOAT_LOGS_JOB, data, false);
        m_WebServiceClient.closeJob(BLUECOAT_LOGS_JOB);

        Pagination<AnomalyRecord> records = m_WebServiceClient.prepareGetRecords(BLUECOAT_LOGS_JOB).get();

        // Top 5 records should contain 'nigella'
        Influence user = new Influence("user");
        user.addInfluenceFieldValue("nigella");
        for (int i = 0; i < 5; i++)
        {
            AnomalyRecord record = records.getDocuments().get(i);
            test(record.getInfluencers().size() == 2);
            if (record.getInfluencers().indexOf(user) >= 0)
            {
                Influence src = new Influence("src_ip");
                src.addInfluenceFieldValue("10.2.20.200");
                test(record.getInfluencers().indexOf(src) >= 0);
                break;
            }
            if (i == 4)
            {
                LOGGER.error("Top 5 records do not contain a user 'nigella'");
                test(false);
            }
        }

        Pagination<Influencer> pagination = m_WebServiceClient.prepareGetInfluencers(BLUECOAT_LOGS_JOB).get();
        test(pagination.getHitCount() > 1000);
        List<Influencer> influencers = pagination.getDocuments();
        test(influencers.size() == 100);

        // We expect two top influencers with score > 85.0
        for (int i = 0; i < 2; i++)
        {
            test(influencers.get(i).getInfluencerFieldName().equals("user")
                    || influencers.get(i).getInfluencerFieldName().equals("src_ip"));
            test(influencers.get(i).getInfluencerFieldValue().equals("nigella")
                    || influencers.get(i).getInfluencerFieldValue().equals("10.2.20.200"));
            test(influencers.get(i).getTimestamp().equals(new Date(1422712800000L)));
            test(influencers.get(i).getAnomalyScore() > 85.0);
        }

        test(influencers.get(2).getAnomalyScore() < influencers.get(1).getAnomalyScore());
    }


    /**
     * Analyse the 5xx status code data using the high count function
     * so there are record and bucket level influencers.
     *
     * @param filePath
     * @throws IOException
     */
    private void doBucketOnlyInfluencers(String filePath) throws IOException
    {
        m_WebServiceClient.deleteJob(STATUS_CODES_RATES_JOB);

        Detector d = new Detector();
        d.setFunction("high_count");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(3600L);
        ac.setInfluencers(Arrays.asList("clientip"));
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("time");
        dd.setTimeFormat("epoch");

        JobConfiguration config = new JobConfiguration(ac);
        config.setId(STATUS_CODES_RATES_JOB);
        config.setDataDescription(dd);

        createJob(config);

        File data = new File(filePath, "5xx_status_codes.csv");
        m_WebServiceClient.fileUpload(STATUS_CODES_RATES_JOB, data, false);
        m_WebServiceClient.closeJob(STATUS_CODES_RATES_JOB);

        Pagination<Bucket> topBuckets = m_WebServiceClient.prepareGetBuckets(STATUS_CODES_RATES_JOB)
                .anomalyScoreThreshold(50.0)
                .expand(true)
                .get();

        boolean atLeastOneBucketWithTwoBucketInfluencers = false;
        for (Bucket bucket : topBuckets.getDocuments())
        {
            double anomalyScore = bucket.getAnomalyScore();
            double maxBucketInfluencerAnomalyScore = 0.0;
            test(bucket.getBucketInfluencers().size() >= 1);
            test(bucket.getBucketInfluencers().size() <= 2);
            atLeastOneBucketWithTwoBucketInfluencers |= bucket.getBucketInfluencers().size() == 2;
            Set<String> bucketInfluencers = new HashSet<>();
            for (BucketInfluencer bucketInfluencer : bucket.getBucketInfluencers())
            {
                maxBucketInfluencerAnomalyScore = Math.max(maxBucketInfluencerAnomalyScore,
                        bucketInfluencer.getAnomalyScore());
                bucketInfluencers.add(bucketInfluencer.getInfluencerFieldName());
            }
            test(bucketInfluencers.contains("bucketTime"));
            if (bucketInfluencers.size() > 1)
            {
                test(bucketInfluencers.contains("clientip"));
            }
            test(anomalyScore == maxBucketInfluencerAnomalyScore);
            test(bucket.getRecords().size() > 0);
            for (AnomalyRecord record : bucket.getRecords())
            {
                List<Influence> influencers = record.getInfluencers();
                if (influencers != null)
                {
                    test(influencers.size() == 1);
                    test(influencers.get(0).getInfluencerFieldName().equals("clientip"));
                }
            }
        }
        test(atLeastOneBucketWithTwoBucketInfluencers);
    }

    private void createJob(JobConfiguration jobConfig) throws IOException
    {
        String jobId = m_WebServiceClient.createJob(jobConfig);
        if (jobId == null || jobId.isEmpty())
        {
            LOGGER.error("No Job Id returned by create job");
            test(jobId != null && jobId.isEmpty() == false);
        }
        LOGGER.info("Created job: " + jobId);
        m_JobIds.add(jobId);
    }

    /**
     * Throws an IllegalStateException if <code>condition</code> is false.
     *
     * @param condition
     * @throws IllegalStateException
     */
    public static void test(boolean condition) throws IllegalStateException
    {
        if (condition == false)
        {
            throw new IllegalStateException();
        }
    }

    /**
     * Delete all the jobs that have been created so far
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void deleteJobs() throws IOException, InterruptedException
    {
        for (String jobId : m_JobIds)
        {
            LOGGER.debug("Deleting job " + jobId);

            boolean success = m_WebServiceClient.deleteJob(jobId);
            if (success == false)
            {
                LOGGER.error("Error deleting job: " + jobId);
            }
        }
    }

    @Override
    public void close() throws IOException
    {
         m_WebServiceClient.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException
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
            LOGGER.error("Error property prelert.test.data.home is not set");
            return;
        }

        try (InfluencersTest test = new InfluencersTest(baseUrl))
        {
            String basePath = prelertTestDataHome + "/influence/security_story1";
            test.doFirewallJob(basePath);
            test.doAuthDJob(basePath);
            test.doServerLogsJob(basePath);
            test.doBluecoatLogsJob(basePath);

            String statusCodePath = prelertTestDataHome + "/engine_api_integration_test/influence";
            test.doBucketOnlyInfluencers(statusCodePath);

            test.deleteJobs();
            test.m_PollAlertService.shutdown();
        }

        LOGGER.info("All tests passed Ok");
    }

}
