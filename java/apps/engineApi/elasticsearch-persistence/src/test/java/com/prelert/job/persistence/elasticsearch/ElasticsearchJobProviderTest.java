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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.Node;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.SchedulerState;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.results.BucketProcessingTime;

public class ElasticsearchJobProviderTest
{
    private static final String CLUSTER_NAME = "myCluster";
    private static final String JOB_ID = "foo";
    private static final String INDEX_NAME = "prelertresults-foo";

    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private Node m_Node;

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
}
