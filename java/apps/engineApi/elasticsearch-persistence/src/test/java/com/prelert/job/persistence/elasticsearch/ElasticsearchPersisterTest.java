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


package com.prelert.job.persistence.elasticsearch;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.IndexNotFoundException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prelert.job.CategorizerState;
import com.prelert.job.JobDetails;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.ModelState;
import com.prelert.job.UnknownJobException;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.job.results.Influencer;
import com.prelert.job.results.ModelDebugOutput;
import com.prelert.job.usage.Usage;

public class ElasticsearchPersisterTest
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchPersisterTest.class);

    private Random m_Random;

    private void initLogging()
    {
        ConsoleAppender console = new ConsoleAppender();
        console.setLayout(new PatternLayout("%m%n"));
        console.setThreshold(Level.DEBUG);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
    }

    private Client initClient(String jobId)
    {
        TransportClient tsClient = TransportClient.builder()
                .settings(Settings.builder().put("cluster.name", "prelert").build())
                .build();
        try
        {
            tsClient.addTransportAddress(
                    new InetSocketTransportAddress(InetAddress.getByName("localhost"),
                            9300));
        }
        catch (UnknownHostException e)
        {
            LOGGER.error("Failed to lookup localhost.... " + e);
            return null;
        }
        LOGGER.info("Creating client");
        Client client = Objects.requireNonNull(tsClient);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        LOGGER.info("Connecting to Elasticsearch cluster '" + client.settings().get("cluster.name")
                + "'");

        LOGGER.trace("ES API CALL: wait for yellow status on whole cluster");
        client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();

        LOGGER.info("Elasticsearch cluster '" + client.settings().get("cluster.name")
                + "' now ready to use");
        String index = "prelertresults-" + jobId;

        LOGGER.info("Deleting any existing index");
        deleteIndex(client, index);

        LOGGER.info("Creating index");
        createIndex(client, index);
        LOGGER.info("Created");
        return client;
    }

    private void createIndex(Client client, String indexName)
    {
        try
        {
            XContentBuilder inputDataMapping = ElasticsearchMappings.inputDataMapping();
            Collection<String> termFields = new ArrayList<>();
            Collection<String> influencers = new ArrayList<>();
            XContentBuilder jobMapping = ElasticsearchMappings.jobMapping();
            XContentBuilder bucketMapping = ElasticsearchMappings.bucketMapping();
            XContentBuilder categorizerStateMapping = ElasticsearchMappings.categorizerStateMapping();
            XContentBuilder categoryDefinitionMapping = ElasticsearchMappings.categoryDefinitionMapping();
            XContentBuilder recordMapping = ElasticsearchMappings.recordMapping(termFields);
            XContentBuilder quantilesMapping = ElasticsearchMappings.quantilesMapping();
            XContentBuilder modelStateMapping = ElasticsearchMappings.modelStateMapping();
            XContentBuilder modelSnapshotMapping = ElasticsearchMappings.modelSnapshotMapping();
            XContentBuilder usageMapping = ElasticsearchMappings.usageMapping();
            XContentBuilder modelSizeStatsMapping = ElasticsearchMappings.modelSizeStatsMapping();
            XContentBuilder influencerMapping = ElasticsearchMappings.influencerMapping(influencers);
            XContentBuilder modelDebugMapping = ElasticsearchMappings.modelDebugOutputMapping(termFields);

            LOGGER.trace("ES API CALL: create index " + indexName);
            client.admin().indices()
                    .prepareCreate(indexName)
                    .addMapping(ElasticsearchJobDataPersister.TYPE, inputDataMapping)
                    .addMapping(JobDetails.TYPE, jobMapping)
                    .addMapping(Bucket.TYPE, bucketMapping)
                    .addMapping(CategorizerState.TYPE, categorizerStateMapping)
                    .addMapping(CategoryDefinition.TYPE, categoryDefinitionMapping)
                    .addMapping(AnomalyRecord.TYPE, recordMapping)
                    .addMapping(Quantiles.TYPE, quantilesMapping)
                    .addMapping(ModelState.TYPE, modelStateMapping)
                    .addMapping(ModelSnapshot.TYPE, modelSnapshotMapping)
                    .addMapping(Usage.TYPE, usageMapping)
                    .addMapping(ModelSizeStats.TYPE, modelSizeStatsMapping)
                    .addMapping(Influencer.TYPE, influencerMapping)
                    .addMapping(ModelDebugOutput.TYPE, modelDebugMapping)
                    .get();

            LOGGER.trace("ES API CALL: wait for yellow status " + indexName);
            client.admin().cluster().prepareHealth(indexName).setWaitForYellowStatus().execute().actionGet();
            LOGGER.info("Created index " + indexName);
        }
        catch (IOException e)
        {
            LOGGER.error("Error creating the raw data index " + indexName, e);
        }
    }

    private void deleteIndex(Client client, String indexName)
    {
        try
        {
            LOGGER.debug("ES API CALL: delete index " + indexName);
            client.admin().indices().prepareDelete(indexName).get();
        }
        catch (IndexNotFoundException e)
        {
            LOGGER.debug("Index didn't exist " + e);
        }
    }

    private void createBucket(String id, boolean interim, ElasticsearchPersister persister)
    {
        Bucket bucket = new Bucket();
        try
        {
            long epoch = Long.parseLong(id);
            bucket.setTimestamp(new Date(epoch * 1000));
        }
        catch (NumberFormatException nfe)
        {
            LOGGER.error("Could not parse ID " + id + " as a long");
        }
        bucket.setAnomalyScore(m_Random.nextDouble() * 100);
        bucket.setBucketSpan(500l);
        bucket.setInterim(interim);
        bucket.setInitialAnomalyScore(m_Random.nextDouble());
        bucket.setMaxNormalizedProbability(m_Random.nextDouble() / (m_Random.nextInt(10000)));

        AnomalyRecord record = new AnomalyRecord();
        record.setActual(1.0 * m_Random.nextInt(100));
        record.setAnomalyScore(m_Random.nextDouble() * 100);
        record.setFunction("count");
        record.setTypical(1.0 * m_Random.nextInt(100));
        record.setDetectorIndex(0);
        record.setInterim(interim);
        List<AnomalyRecord> records = new ArrayList<>();

        records.add(record);

        bucket.setRecordCount(records.size());
        bucket.setEventCount(m_Random.nextInt(200));
        bucket.setRecords(records);
        persister.persistBucket(bucket);
    }

    public void testDeleteInterimBuckets() throws InterruptedException,
            ExecutionException, UnknownJobException, UnknownHostException
    {
        initLogging();
        m_Random = new Random();

        LOGGER.info("Starting test for ES Persist");

        String jobId = "es_persister_test_pest";
        Client client = initClient(jobId);

        ElasticsearchPersister persister = new ElasticsearchPersister(jobId, client);
        LOGGER.info("Created persister");
        createBucket("1234567800", true, persister);
        LOGGER.info("Created bucket 1");
        createBucket("1234567810", true, persister);

        createBucket("1234567800", false, persister);

        createBucket("1234567820", true, persister);
        createBucket("1234567830", true, persister);
        createBucket("1234567840", true, persister);
        createBucket("1234567850", true, persister);
        try
        {
            Thread.sleep(2000);

        } catch (InterruptedException e)
        {
        }

        createBucket("1234567830", false, persister);

        createBucket("1234567860", true, persister);
        createBucket("1234567870", true, persister);

        try
        {
            Thread.sleep(2000);

        } catch (InterruptedException e)
        {
        }

        createBucket("1234567850", false, persister);

        createBucket("1234567880", true, persister);

        // We should be left with 6 buckets and 6 results:
        // non-interim: 1234567800, 1234567830, 1234567850
        // interim: 1234567860, 1234567870, 1234567880

        LOGGER.info("All Done");
    }

    public static void main(String[] args) throws UnknownJobException, UnknownHostException,
            InterruptedException, ExecutionException
    {
        ElasticsearchPersisterTest t = new ElasticsearchPersisterTest();
        t.testDeleteInterimBuckets();
    }

}