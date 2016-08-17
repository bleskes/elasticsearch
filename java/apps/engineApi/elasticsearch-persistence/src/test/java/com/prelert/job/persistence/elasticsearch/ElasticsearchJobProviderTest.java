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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.AnalysisLimits;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.JobStatus;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.SchedulerState;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.persistence.DataStoreException;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketProcessingTime;

public class ElasticsearchJobProviderTest
{
    private static final String CLUSTER_NAME = "myCluster";
    private static final String JOB_ID = "foo";
    private static final String INDEX_NAME = "prelertresults-foo";

    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private Node m_Node;

    @Captor private ArgumentCaptor<Map<String, Object>> m_MapCaptor;

    @Before
    public void setUp() throws InterruptedException, ExecutionException
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testShutdown() throws InterruptedException, ExecutionException, IOException
    {
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true);
        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        provider.shutdown();

        verify(m_Node).close();
    }

    @Test
    public void testGetQuantiles_GivenNoIndexForJob() throws InterruptedException,
            ExecutionException, UnknownJobException
    {
        m_ExpectedException.expect(UnknownJobException.class);
        m_ExpectedException.expectMessage("No known job with id '"+ JOB_ID + "'");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.MISSING_JOB_ERROR));

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .throwMissingIndexOnPrepareGet(INDEX_NAME, Quantiles.TYPE, Quantiles.QUANTILES_ID);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        provider.getQuantiles(JOB_ID);
    }

    @Test
    public void testGetQuantiles_GivenNoQuantilesForJob() throws InterruptedException,
            ExecutionException, UnknownJobException
    {
        GetResponse getResponse = createGetResponse(false, null);

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet(INDEX_NAME, Quantiles.TYPE, Quantiles.QUANTILES_ID, getResponse);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        Quantiles quantiles = provider.getQuantiles(JOB_ID);

        assertEquals("", quantiles.getQuantileState());
    }

    @Test
    public void testGetQuantiles_GivenQuantilesHaveNonEmptyState()
            throws InterruptedException, ExecutionException, UnknownJobException
    {
        Map<String, Object> source = new HashMap<>();
        source.put(Quantiles.QUANTILE_STATE, "state");
        GetResponse getResponse = createGetResponse(true, source);

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet(INDEX_NAME, Quantiles.TYPE, Quantiles.QUANTILES_ID, getResponse);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        Quantiles quantiles = provider.getQuantiles(JOB_ID);

        assertEquals("state", quantiles.getQuantileState());
    }

    @Test
    public void testGetQuantiles_GivenQuantilesHaveEmptyState()
            throws InterruptedException, ExecutionException, UnknownJobException
    {
        Map<String, Object> source = new HashMap<>();
        GetResponse getResponse = createGetResponse(true, source);

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet(INDEX_NAME, Quantiles.TYPE, Quantiles.QUANTILES_ID, getResponse);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        Quantiles quantiles = provider.getQuantiles(JOB_ID);

        assertEquals("", quantiles.getQuantileState());
    }

    @Test
    public void testGetSchedulerState_GivenNoIndexForJob() throws InterruptedException,
            ExecutionException, UnknownJobException
    {
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .throwMissingIndexOnPrepareGet(INDEX_NAME, SchedulerState.TYPE, SchedulerState.TYPE);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        assertFalse(provider.getSchedulerState(JOB_ID).isPresent());
    }

    @Test
    public void testGetSchedulerState_GivenNoSchedulerStateForJob() throws InterruptedException,
            ExecutionException, UnknownJobException
    {
        GetResponse getResponse = createGetResponse(false, null);

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet(INDEX_NAME, SchedulerState.TYPE, SchedulerState.TYPE, getResponse);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        assertFalse(provider.getSchedulerState(JOB_ID).isPresent());
    }

    @Test
    public void testGetSchedulerState_GivenSchedulerStateForJob() throws InterruptedException,
            ExecutionException, UnknownJobException
    {
        Map<String, Object> source = new HashMap<>();
        source.put(SchedulerState.START_TIME_MILLIS, 18L);
        source.put(SchedulerState.END_TIME_MILLIS, 42L);
        GetResponse getResponse = createGetResponse(true, source);

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet(INDEX_NAME, SchedulerState.TYPE, SchedulerState.TYPE, getResponse);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        Optional<SchedulerState> schedulerState = provider.getSchedulerState(JOB_ID);

        assertTrue(schedulerState.isPresent());
        assertEquals(18L, schedulerState.get().getStartTimeMillis().longValue());
        assertEquals(42L, schedulerState.get().getEndTimeMillis().longValue());
    }

    @Test
    public void testConstructor_GivenRedClusterState() throws InterruptedException,
            ExecutionException
    {
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusRedResponse();

        try
        {
            ElasticsearchJobProvider provider = createProvider(clientBuilder.build());
            assertTrue(false);
            provider.shutdown();
        }
        catch (IllegalStateException e)
        {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testIsConnected() throws InterruptedException, ExecutionException
    {
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .addClusterStatusYellowResponse("prelertresults-id", TimeValue.timeValueSeconds(2L));

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        assertTrue(provider.isConnected("id"));
    }

    @Test
    public void testCreateUsageMetering() throws InterruptedException, ExecutionException
    {
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, false)
                .prepareCreate(ElasticsearchJobProvider.PRELERT_USAGE_INDEX)
                .addClusterStatusYellowResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX);
        Client client = clientBuilder.build();
        createProvider(client);
        clientBuilder.verifyIndexCreated(ElasticsearchJobProvider.PRELERT_USAGE_INDEX);
    }

    @Test
    public void testSavePrelertInfo() throws InterruptedException, ExecutionException
    {
        String index = "prelert-int";
        String info = "info";

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareCreate(index)
                .addClusterStatusYellowResponse(index);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        clientBuilder.resetIndices();
        clientBuilder.prepareCreate(index);
        clientBuilder.addIndicesExistsResponse(index, false);
        clientBuilder.prepareIndex(index,  info);
        client = clientBuilder.build();

        provider.savePrelertInfo(info);
        clientBuilder.verifyIndexCreated(index);
    }

    @Test
    public void testCheckJobExists() throws InterruptedException, ExecutionException
    {
        GetResponse getResponse = createGetResponse(true, null);
        String jobId = "jobThing";

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("prelertresults-" + jobId, JobDetails.TYPE, jobId, getResponse);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        try
        {
            provider.checkJobExists(jobId);
        }
        catch (UnknownJobException e)
        {
            assertTrue(false);
        }
    }


    @Test
    public void testCheckJobExists_GivenInvalidId() throws InterruptedException, ExecutionException
    {
        GetResponse getResponse = createGetResponse(false, null);
        String jobId = "jobThing";

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("prelertresults-" + jobId, JobDetails.TYPE, jobId, getResponse);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        try
        {
            provider.checkJobExists(jobId);
            assertTrue(false);
        }
        catch (UnknownJobException e)
        {
        }
    }

    @Test
    public void testJobIdIsUnique_GivenSingleJob() throws InterruptedException, ExecutionException
    {
        String jobId = "jobThing";

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        clientBuilder.resetIndices();
        clientBuilder.addIndicesExistsResponse("prelertresults-" + jobId, false);
        client = clientBuilder.build();
        assertTrue(provider.jobIdIsUnique(jobId));
    }

    @Test
    public void testJobIdIsUnique_GivenJobExists() throws InterruptedException, ExecutionException
    {
        String jobId = "jobThing";

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        clientBuilder.resetIndices();
        clientBuilder.addIndicesExistsResponse("prelertresults-" + jobId, true);
        client = clientBuilder.build();
        assertFalse(provider.jobIdIsUnique(jobId));
    }

    @Test
    public void testGetJobDetails() throws InterruptedException, ExecutionException
    {
        String jobId = "myJob";
        Map<String, Object> source = new HashMap<>();
        source.put("jobId",  jobId);
        source.put("description", "This is a job description");
        GetResponse response = createGetResponse(true, source);

        GetResponse response2 = createGetResponse(false, null);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("prelertresults-" + jobId, JobDetails.TYPE, jobId, response)
                .prepareGet("prelertresults-" + jobId, ModelSizeStats.TYPE, "modelSizeStats", response2)
                .prepareGet("prelertresults-" + jobId, BucketProcessingTime.TYPE, BucketProcessingTime.AVERAGE_PROCESSING_TIME_MS, response2);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        Optional<JobDetails> details = provider.getJobDetails(jobId);
        assertTrue(details.isPresent());
        JobConfiguration config = new JobConfiguration();
        config.setDescription("This is a job description");
        JobDetails expected = new JobDetails(jobId, config);
        assertEquals(details.get().allFields(), expected.allFields());
    }


    @Test
    public void testGetJobDetails_NonExistent() throws InterruptedException, ExecutionException
    {
        String jobId = "myJob";
        Map<String, Object> source = new HashMap<>();
        source.put("jobId",  jobId);
        source.put("description", "This is a job description");
        GetResponse response = createGetResponse(false, null);

        GetResponse response2 = createGetResponse(false, null);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("prelertresults-" + jobId, JobDetails.TYPE, jobId, response)
                .prepareGet("prelertresults-" + jobId, ModelSizeStats.TYPE, "modelSizeStats", response2)
                .prepareGet("prelertresults-" + jobId, BucketProcessingTime.TYPE, BucketProcessingTime.AVERAGE_PROCESSING_TIME_MS, response2);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        Optional<JobDetails> details = provider.getJobDetails(jobId);
        assertFalse(details.isPresent());
    }

    @Test
    public void testGetJobs() throws InterruptedException, ExecutionException
    {
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("jobId",  "job1");
        map.put("description", "This is a job description");
        source.add(map);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("jobId",  "job2");
        map2.put("description", "This is a job description 2");
        source.add(map2);

        Map<String, Object> map3 = new HashMap<>();
        map3.put("jobId",  "job3");
        map3.put("description", "This is a job description 3");
        source.add(map3);

        Map<String, Object> map4 = new HashMap<>();
        map4.put("jobId",  "job4");
        map4.put("description", "This is a job description 4");
        source.add(map4);

        SearchResponse response = createSearchResponse(true, source);
        GetResponse response2 = createGetResponse(false, null);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-*",  JobDetails.TYPE,  0,  10, response)
                .prepareGet("prelertresults-job1", ModelSizeStats.TYPE, "modelSizeStats", response2)
                .prepareGet("prelertresults-job1", BucketProcessingTime.TYPE, BucketProcessingTime.AVERAGE_PROCESSING_TIME_MS, response2)
                .prepareGet("prelertresults-job2", ModelSizeStats.TYPE, "modelSizeStats", response2)
                .prepareGet("prelertresults-job2", BucketProcessingTime.TYPE, BucketProcessingTime.AVERAGE_PROCESSING_TIME_MS, response2)
                .prepareGet("prelertresults-job3", ModelSizeStats.TYPE, "modelSizeStats", response2)
                .prepareGet("prelertresults-job3", BucketProcessingTime.TYPE, BucketProcessingTime.AVERAGE_PROCESSING_TIME_MS, response2)
                .prepareGet("prelertresults-job4", ModelSizeStats.TYPE, "modelSizeStats", response2)
                .prepareGet("prelertresults-job4", BucketProcessingTime.TYPE, BucketProcessingTime.AVERAGE_PROCESSING_TIME_MS, response2);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        int skip = 0;
        int take = 10;
        QueryPage<JobDetails> page = provider.getJobs(skip, take);
        assertEquals(source.size(), page.hitCount());
        List<JobDetails> jobs = page.queryResults();
        for (int i = 0; i < source.size(); i++)
        {
            assertEquals(source.get(i).get("jobId"), jobs.get(i).getId());
            assertEquals(source.get(i).get("description"), jobs.get(i).getDescription());
        }
    }

    @Test
    public void testCreateJob() throws InterruptedException, ExecutionException
    {
        JobDetails job = new JobDetails();
        job.setJobId("marscapone");
        job.setDescription("This is a very cheesy job");
        job.setStatus(JobStatus.FAILED);
        AnalysisLimits limits = new AnalysisLimits();
        limits.setModelMemoryLimit(9878695309134L);
        job.setAnalysisLimits(limits);

        ArgumentCaptor<String> getSource = ArgumentCaptor.forClass(String.class);

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .addClusterStatusYellowResponse("prelertresults-"+ job.getJobId())
                .prepareCreate("prelertresults-" + job.getJobId())
                .prepareIndex("prelertresults-" + job.getJobId(), getSource);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        assertTrue(provider.createJob(job));
        String result = getSource.getValue();
        assertTrue(result.matches(".*This is a very cheesy job.*"));
        assertTrue(result.matches(".*marscapone.*"));
        assertTrue(result.matches(".*9878695309134.*"));
        assertTrue(result.matches(".*FAILED.*"));
    }

    @Test
    public void testUpdateJob() throws InterruptedException, ExecutionException, UnknownJobException
    {
        String job = "testjob";
        Map<String, Object> map = new HashMap<>();
        map.put("testKey",  "testValue");

        GetResponse getResponse = createGetResponse(true, null);

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("prelertresults-" + job, JobDetails.TYPE, job, getResponse)
                .prepareUpdate("prelertresults-" + job, JobDetails.TYPE, job, m_MapCaptor);
        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        assertTrue(provider.updateJob(job, map));
        Map<String, Object> response = m_MapCaptor.getValue();
        assertTrue(response.equals(map));
    }

    @Test
    public void testSetJobStatus() throws InterruptedException, ExecutionException, UnknownJobException
    {
        String job = "testjob";

        GetResponse getResponse = createGetResponse(true, null);

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("prelertresults-" + job, JobDetails.TYPE, job, getResponse)
                .prepareUpdate("prelertresults-" + job, JobDetails.TYPE, job, m_MapCaptor);
        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        assertTrue(provider.setJobStatus(job, JobStatus.PAUSING));
        Map<String, Object> response = m_MapCaptor.getValue();
        assertTrue(response.get(JobDetails.STATUS).equals(JobStatus.PAUSING));
    }

    @Test
    public void testSetJobFinishedTimeAndStatus() throws InterruptedException, ExecutionException, UnknownJobException
    {
        String job = "testjob";

        GetResponse getResponse = createGetResponse(true, null);

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("prelertresults-" + job, JobDetails.TYPE, job, getResponse)
                .prepareUpdate("prelertresults-" + job, JobDetails.TYPE, job, m_MapCaptor);
        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        Date now = new Date();
        assertTrue(provider.setJobFinishedTimeAndStatus(job, now, JobStatus.CLOSING));
        Map<String, Object> response = m_MapCaptor.getValue();
        assertTrue(response.get(JobDetails.STATUS).equals(JobStatus.CLOSING));
        assertTrue(response.get(JobDetails.FINISHED_TIME).equals(now));
    }

    @Test
    public void testDeleteJob() throws InterruptedException, ExecutionException, UnknownJobException, DataStoreException, IOException
    {
        String jobId = "ThisIsMyJob";
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true);
        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        clientBuilder.resetIndices();
        clientBuilder.addIndicesExistsResponse("prelertresults-" + jobId, true)
                     .addIndicesDeleteResponse("prelertresults-" + jobId,  true,  false);
        client = clientBuilder.build();


        assertTrue(provider.deleteJob(jobId));
    }

    @Test
    public void testBuckets_OneBucketNoInterim() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException
    {
        String jobId = "TestJobIdentification";
        Date now = new Date();
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("timestamp",  now.getTime());
        source.add(map);

        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        int skip = 0;
        int take = 10;
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId,  Bucket.TYPE,  skip,  take, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        QueryPage<Bucket> buckets = provider.buckets(jobId, false, false, skip, take, 0.0, 1.0);
        assertEquals(1l, buckets.hitCount());
        QueryBuilder query = queryBuilder.getValue();
        String queryString = query.toString();
        System.out.println(queryString);
        assertTrue(queryString.matches("(?s).*maxNormalizedProbability[^}]*from. : 1\\.0.*must_not[^}]*term[^}]*isInterim. : .true.*"));
    }

    @Test
    public void testBuckets_OneBucketInterim() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException
    {
        String jobId = "TestJobIdentification";
        Date now = new Date();
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("timestamp",  now.getTime());
        source.add(map);

        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        int skip = 99;
        int take = 17;
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId,  Bucket.TYPE,  skip,  take, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        QueryPage<Bucket> buckets = provider.buckets(jobId, false, true, skip, take, 5.1, 10.9);
        assertEquals(1l, buckets.hitCount());
        QueryBuilder query = queryBuilder.getValue();
        String queryString = query.toString();
        System.out.println(queryString);
        assertTrue(queryString.matches("(?s).*maxNormalizedProbability[^}]*from. : 10\\.9.*"));
        assertTrue(queryString.matches("(?s).*anomalyScore[^}]*from. : 5\\.1.*"));
        assertFalse(queryString.matches("(?s).*isInterim.*"));
    }

    @Test
    public void testBucket_NoBucketNoExpandNoInterim() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException
    {
        String jobId = "TestJobIdentification";
        Long timestamp = 98765432123456789L;
        Date now = new Date();
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("timestamp",  now.getTime());
        //source.add(map);

        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(false, source);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId,  Bucket.TYPE,  0,  0, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        Optional<Bucket>  bucket = provider.bucket(jobId, timestamp, false, false);
        assertFalse(bucket.isPresent());
    }

    @Test
    public void testBucket_OneBucketNoExpandNoInterim() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException
    {
        String jobId = "TestJobIdentification";
        Date now = new Date();
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("@timestamp", now.getTime());
        source.add(map);

        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId,  Bucket.TYPE,  0,  0, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        Optional<Bucket>  bucketHolder = provider.bucket(jobId, now.getTime(), false, false);
        assertTrue(bucketHolder.isPresent());
        Bucket b = bucketHolder.get();
        assertEquals(now, b.getTimestamp());
    }

    @Test
    public void testBucket_OneBucketNoExpandInterim() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException
    {
        String jobId = "TestJobIdentification";
        Date now = new Date();
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("@timestamp", now.getTime());
        map.put("isInterim",  true);
        source.add(map);

        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId,  Bucket.TYPE,  0,  0, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        Optional<Bucket>  bucketHolder = provider.bucket(jobId, now.getTime(), false, false);
        assertFalse(bucketHolder.isPresent());
    }

    @Test
    public void testRecords() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException
    {
        String jobId = "TestJobIdentification";
        Date now = new Date();
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> recordMap1 = new HashMap<>();
        recordMap1.put("typical", 22.4);
        recordMap1.put("actual", 33.3);
        recordMap1.put("timestamp", now.getTime());
        recordMap1.put("function", "irritable");
        recordMap1.put("bucketSpan", 22);
        recordMap1.put("_parent", "father");
        Map<String, Object> recordMap2 = new HashMap<>();
        recordMap2.put("typical", 1122.4);
        recordMap2.put("actual", 933.3);
        recordMap2.put("timestamp", now.getTime());
        recordMap2.put("function", "irrascible");
        recordMap2.put("bucketSpan", 22);
        recordMap2.put("_parent", "father");
        source.add(recordMap1);
        source.add(recordMap2);

        int skip = 14;
        int take = 2;
        String sortfield = "minefield";
        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId,  AnomalyRecord.TYPE,  skip,  take, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        QueryPage<AnomalyRecord> recordPage = provider.records(jobId, skip, take, now.getTime(), now.getTime(),
                true, sortfield, true, 11.1, 2.2);
        assertEquals(2L, recordPage.hitCount());
        List<AnomalyRecord> records = recordPage.queryResults();
        assertEquals(22.4, records.get(0).getTypical()[0], 0.000001);
        assertEquals(33.3, records.get(0).getActual()[0], 0.000001);
        assertEquals("irritable", records.get(0).getFunction());
        assertEquals(1122.4, records.get(1).getTypical()[0], 0.000001);
        assertEquals(933.3, records.get(1).getActual()[0], 0.000001);
        assertEquals("irrascible", records.get(1).getFunction());
    }

    private ElasticsearchJobProvider createProvider(Client client)
    {
        return new ElasticsearchJobProvider(m_Node, client, 0);
    }

    private static GetResponse createGetResponse(boolean exists, Map<String, Object> source)
    {
        GetResponse getResponse = mock(GetResponse.class);
        when(getResponse.isExists()).thenReturn(exists);
        when(getResponse.getSource()).thenReturn(source);
        return getResponse;
    }

    private static SearchResponse createSearchResponse(boolean exists, List<Map<String, Object>> source)
    {
        SearchResponse response = mock(SearchResponse.class);
        SearchHits hits = mock(SearchHits.class);
        List<SearchHit> list = new ArrayList<>();

        for (Map<String, Object> map : source)
        {
            SearchHit hit = mock(SearchHit.class);
            when(hit.getSource()).thenReturn(map);
            when(hit.getId()).thenReturn(String.valueOf(map.hashCode()));
            doAnswer(invocation ->
            {
                String field = (String) invocation.getArguments()[0];
                SearchHitField shf = mock(SearchHitField.class);
                when(shf.getValue()).thenReturn(map.get(field));
                return shf;
            }).when(hit).field(any(String.class));
            list.add(hit);
        }
        when(response.getHits()).thenReturn(hits);
        when(hits.getHits()).thenReturn(list.toArray(new SearchHit[0]));
        when(hits.getTotalHits()).thenReturn((long) source.size());

        doAnswer(invocation ->
        {
            Integer idx = (Integer) invocation.getArguments()[0];
            return list.get(idx);
        }).when(hits).getAt(any(Integer.class));

        return response;
    }
}
