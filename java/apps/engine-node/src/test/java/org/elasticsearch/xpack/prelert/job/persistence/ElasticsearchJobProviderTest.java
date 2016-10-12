
package org.elasticsearch.xpack.prelert.job.persistence;

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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonParseException;
import org.elasticsearch.xpack.prelert.job.AnalysisLimits;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.JobStatus;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.SchedulerState;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.BucketProcessingTime;
import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;
import org.elasticsearch.xpack.prelert.job.results.Influencer;

public class ElasticsearchJobProviderTest extends ESTestCase {
    private static final String CLUSTER_NAME = "myCluster";
    private static final String JOB_ID = "foo";
    private static final String INDEX_NAME = "prelertresults-foo";

    @Mock
    private Node node;

    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    @Before
    public void setUpMocks() throws InterruptedException, ExecutionException {
        MockitoAnnotations.initMocks(this);
    }

    public void testShutdown() throws InterruptedException, ExecutionException, IOException {
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true);
        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        provider.shutdown();

        verify(node).close();
    }

    public void testGetQuantiles_GivenNoIndexForJob() throws InterruptedException,
            ExecutionException, UnknownJobException {

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .throwMissingIndexOnPrepareGet(INDEX_NAME, Quantiles.TYPE, Quantiles.QUANTILES_ID);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        UnknownJobException e = ESTestCase.expectThrows(UnknownJobException.class, () -> provider.getQuantiles(JOB_ID));
        assertEquals(ErrorCodes.MISSING_JOB_ERROR, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_UNKNOWN_ID, JOB_ID), e.getMessage());
    }

    public void testGetQuantiles_GivenNoQuantilesForJob() throws InterruptedException,
            ExecutionException, UnknownJobException {
        GetResponse getResponse = createGetResponse(false, null);

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet(INDEX_NAME, Quantiles.TYPE, Quantiles.QUANTILES_ID, getResponse);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        Quantiles quantiles = provider.getQuantiles(JOB_ID);

        assertEquals("", quantiles.getQuantileState());
    }

    public void testGetQuantiles_GivenQuantilesHaveNonEmptyState()
            throws InterruptedException, ExecutionException, UnknownJobException {
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

    public void testGetQuantiles_GivenQuantilesHaveEmptyState()
            throws InterruptedException, ExecutionException, UnknownJobException {
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

    public void testGetSchedulerState_GivenNoIndexForJob() throws InterruptedException,
            ExecutionException, UnknownJobException {
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .throwMissingIndexOnPrepareGet(INDEX_NAME, SchedulerState.TYPE, SchedulerState.TYPE);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        assertFalse(provider.getSchedulerState(JOB_ID).isPresent());
    }

    public void testGetSchedulerState_GivenNoSchedulerStateForJob() throws InterruptedException,
            ExecutionException, UnknownJobException {
        GetResponse getResponse = createGetResponse(false, null);

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet(INDEX_NAME, SchedulerState.TYPE, SchedulerState.TYPE, getResponse);

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        assertFalse(provider.getSchedulerState(JOB_ID).isPresent());
    }

    public void testGetSchedulerState_GivenSchedulerStateForJob() throws InterruptedException,
            ExecutionException, UnknownJobException {
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

    public void testIsConnected() throws InterruptedException, ExecutionException {
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .addClusterStatusYellowResponse("prelertresults-id", TimeValue.timeValueSeconds(2L));

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        assertTrue(provider.isConnected("id"));
    }

    public void testIsConnected_TimedOutException() throws InterruptedException, ExecutionException {
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .addClusterStatusYellowResponse("prelertresults-id", TimeValue.timeValueSeconds(2L), new NoNodeAvailableException("blah"));
        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        assertFalse(provider.isConnected("id"));
    }

    public void testCreateUsageMetering() throws InterruptedException, ExecutionException {
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, false)
                .prepareCreate(ElasticsearchJobProvider.PRELERT_USAGE_INDEX)
                .addClusterStatusYellowResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX);
        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        provider.doStart();
        try
        {
            clientBuilder.verifyIndexCreated(ElasticsearchJobProvider.PRELERT_USAGE_INDEX);
        }
        finally {
            provider.shutdown();
        }
    }

    public void testSavePrelertInfo() throws InterruptedException, ExecutionException {
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
        clientBuilder.prepareIndex(index, info);
        client = clientBuilder.build();

        provider.savePrelertInfo(info);
        clientBuilder.verifyIndexCreated(index);
    }

    public void testCheckJobExists() throws InterruptedException, ExecutionException {
        GetResponse getResponse = createGetResponse(true, null);
        String jobId = "jobThing";

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("prelertresults-" + jobId, JobDetails.TYPE, jobId, getResponse);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        try {
            provider.checkJobExists(jobId);
        } catch (UnknownJobException e) {
            assertTrue(false);
        }
    }

    public void testCheckJobExists_GivenInvalidId() throws InterruptedException, ExecutionException {
        GetResponse getResponse = createGetResponse(false, null);
        String jobId = "jobThing";

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("prelertresults-" + jobId, JobDetails.TYPE, jobId, getResponse);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        try {
            provider.checkJobExists(jobId);
            assertTrue(false);
        } catch (UnknownJobException e) {
        }
    }

    public void testCheckJobExists_GivenInvalidIndex() throws InterruptedException, ExecutionException {
        String jobId = "jobThing";
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("prelertresults-" + jobId, JobDetails.TYPE, jobId, new IndexNotFoundException("blah"));
        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        try {
            provider.checkJobExists(jobId);
            assertTrue(false);
        } catch (UnknownJobException e) {
        }
    }

    public void testJobIdIsUnique_GivenSingleJob() throws InterruptedException, ExecutionException {
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

    public void testJobIdIsUnique_GivenJobExists() throws InterruptedException, ExecutionException {
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

    public void testGetJobDetails() throws InterruptedException, ExecutionException {
        String jobId = "myJob";
        Map<String, Object> source = new HashMap<>();
        source.put("jobId", jobId);
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


    public void testGetJobDetails_NonExistent() throws InterruptedException, ExecutionException {
        String jobId = "myJob";
        Map<String, Object> source = new HashMap<>();
        source.put("jobId", jobId);
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

    public void testGetJobDetails_IndexNotFound() throws InterruptedException, ExecutionException {
        String jobId = "myJob";
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("prelertresults-" + jobId, JobDetails.TYPE, jobId, new IndexNotFoundException("not today"));

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        Optional<JobDetails> details = provider.getJobDetails(jobId);
        assertFalse(details.isPresent());
    }

    public void testGetJobs() throws InterruptedException, ExecutionException {
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("jobId", "job1");
        map.put("description", "This is a job description");
        source.add(map);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("jobId", "job2");
        map2.put("description", "This is a job description 2");
        source.add(map2);

        Map<String, Object> map3 = new HashMap<>();
        map3.put("jobId", "job3");
        map3.put("description", "This is a job description 3");
        source.add(map3);

        Map<String, Object> map4 = new HashMap<>();
        map4.put("jobId", "job4");
        map4.put("description", "This is a job description 4");
        source.add(map4);

        SearchResponse response = createSearchResponse(true, source);
        GetResponse response2 = createGetResponse(false, null);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-*", JobDetails.TYPE, 0, 10, response)
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
        List<JobDetails> jobs = page.hits();
        for (int i = 0; i < source.size(); i++) {
            assertEquals(source.get(i).get("jobId"), jobs.get(i).getId());
            assertEquals(source.get(i).get("description"), jobs.get(i).getDescription());
        }
    }

    public void testCreateJob() throws InterruptedException, ExecutionException {
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
                .addClusterStatusYellowResponse("prelertresults-" + job.getJobId())
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

    public void testCreateJob_ElasticsearchException() throws InterruptedException, ExecutionException {
        JobDetails job = new JobDetails();
        job.setJobId("gorgonzola");
        ElasticsearchException ex = new ElasticsearchException("blah");

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .addClusterStatusYellowResponse("prelertresults-" + job.getJobId())
                .prepareCreate("prelertresults-" + job.getJobId(), ex);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        try {
            provider.createJob(job);
            assertTrue(false);
        } catch (ElasticsearchException e) {
            assertEquals(e, ex);
        }
    }

    public void testCreateJob_IOException() throws InterruptedException, ExecutionException {
        JobDetails job = new JobDetails();
        job.setJobId("gorgonzola");

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .addClusterStatusYellowResponse("prelertresults-" + job.getJobId())
                .prepareCreate("prelertresults-" + job.getJobId(), new IOException());

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        assertFalse(provider.createJob(job));
    }

    public void testUpdateJob() throws InterruptedException, ExecutionException, UnknownJobException {
        String job = "testjob";
        Map<String, Object> map = new HashMap<>();
        map.put("testKey", "testValue");

        GetResponse getResponse = createGetResponse(true, null);

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("prelertresults-" + job, JobDetails.TYPE, job, getResponse)
                .prepareUpdate("prelertresults-" + job, JobDetails.TYPE, job, mapCaptor);
        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        assertTrue(provider.updateJob(job, map));
        Map<String, Object> response = mapCaptor.getValue();
        assertTrue(response.equals(map));
    }

    public void testUpdateJob_ConflictedVersion() throws InterruptedException, ExecutionException, UnknownJobException {
        String job = "testjob";
        Map<String, Object> map = new HashMap<>();
        map.put("testKey", "testValue");

        GetResponse getResponse = createGetResponse(true, null);
        StreamInput si = mock(StreamInput.class);

        MockClientBuilder clientBuilder;
        try {
            clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                    .addClusterStatusYellowResponse()
                    .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                    .prepareGet("prelertresults-" + job, JobDetails.TYPE, job, getResponse)
                    .prepareUpdate("prelertresults-" + job, JobDetails.TYPE, job, mapCaptor, new VersionConflictEngineException(si));

            Client client = clientBuilder.build();
            ElasticsearchJobProvider provider = createProvider(client);
            assertFalse(provider.updateJob(job, map));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testSetJobStatus() throws InterruptedException, ExecutionException, UnknownJobException {
        String job = "testjob";

        GetResponse getResponse = createGetResponse(true, null);

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("prelertresults-" + job, JobDetails.TYPE, job, getResponse)
                .prepareUpdate("prelertresults-" + job, JobDetails.TYPE, job, mapCaptor);
        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        assertTrue(provider.setJobStatus(job, JobStatus.PAUSING));
        Map<String, Object> response = mapCaptor.getValue();
        assertTrue(response.get(JobDetails.STATUS).equals(JobStatus.PAUSING));
    }

    public void testSetJobFinishedTimeAndStatus() throws InterruptedException, ExecutionException, UnknownJobException {
        String job = "testjob";

        GetResponse getResponse = createGetResponse(true, null);

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("prelertresults-" + job, JobDetails.TYPE, job, getResponse)
                .prepareUpdate("prelertresults-" + job, JobDetails.TYPE, job, mapCaptor);
        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        Date now = new Date();
        assertTrue(provider.setJobFinishedTimeAndStatus(job, now, JobStatus.CLOSING));
        Map<String, Object> response = mapCaptor.getValue();
        assertTrue(response.get(JobDetails.STATUS).equals(JobStatus.CLOSING));
        assertTrue(response.get(JobDetails.FINISHED_TIME).equals(now));
    }

    public void testDeleteJob() throws InterruptedException, ExecutionException, UnknownJobException, DataStoreException, IOException {
        String jobId = "ThisIsMyJob";
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true);
        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        clientBuilder.resetIndices();
        clientBuilder.addIndicesExistsResponse("prelertresults-" + jobId, true)
                .addIndicesDeleteResponse("prelertresults-" + jobId, true, false);
        client = clientBuilder.build();


        assertTrue(provider.deleteJob(jobId));
    }

    public void testDeleteJob_InvalidIndex() throws InterruptedException, ExecutionException, UnknownJobException, DataStoreException, IOException {
        String jobId = "ThisIsMyJob";
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true);
        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        clientBuilder.resetIndices();
        clientBuilder.addIndicesExistsResponse("prelertresults-" + jobId, true)
                .addIndicesDeleteResponse("prelertresults-" + jobId, true, true);
        client = clientBuilder.build();

        try {
            provider.deleteJob(jobId);
            assertTrue(false);
        } catch (UnknownJobException ex) {
            assertEquals(jobId, ex.getJobId());
        }
    }

    public void testBuckets_OneBucketNoInterim() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
        String jobId = "TestJobIdentification";
        Date now = new Date();
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", now.getTime());
        source.add(map);

        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        int skip = 0;
        int take = 10;
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId, Bucket.TYPE, skip, take, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        BucketsQueryBuilder bq = new BucketsQueryBuilder()
                .skip(skip)
                .take(take)
                .anomalyScoreThreshold(0.0)
                .normalizedProbabilityThreshold(1.0);

        QueryPage<Bucket> buckets = provider.buckets(jobId, bq.build());
        assertEquals(1l, buckets.hitCount());
        QueryBuilder query = queryBuilder.getValue();
        String queryString = query.toString();
        assertTrue(queryString.matches("(?s).*maxNormalizedProbability[^}]*from. : 1\\.0.*must_not[^}]*term[^}]*isInterim.*value. : .true.*"));
    }

    public void testBuckets_OneBucketInterim() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
        String jobId = "TestJobIdentification";
        Date now = new Date();
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", now.getTime());
        source.add(map);

        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        int skip = 99;
        int take = 17;
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId, Bucket.TYPE, skip, take, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        BucketsQueryBuilder bq = new BucketsQueryBuilder()
                .skip(skip)
                .take(take)
                .anomalyScoreThreshold(5.1)
                .normalizedProbabilityThreshold(10.9)
                .includeInterim(true);


        QueryPage<Bucket> buckets = provider.buckets(jobId, bq.build());
        assertEquals(1l, buckets.hitCount());
        QueryBuilder query = queryBuilder.getValue();
        String queryString = query.toString();
        assertTrue(queryString.matches("(?s).*maxNormalizedProbability[^}]*from. : 10\\.9.*"));
        assertTrue(queryString.matches("(?s).*anomalyScore[^}]*from. : 5\\.1.*"));
        assertFalse(queryString.matches("(?s).*isInterim.*"));
    }

    public void testBuckets_UsingBuilder() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
        String jobId = "TestJobIdentification";
        Date now = new Date();
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", now.getTime());
        source.add(map);

        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        int skip = 99;
        int take = 17;
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId, Bucket.TYPE, skip, take, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        BucketsQueryBuilder bq = new BucketsQueryBuilder();
        bq.skip(skip);
        bq.take(take);
        bq.anomalyScoreThreshold(5.1);
        bq.normalizedProbabilityThreshold(10.9);
        bq.includeInterim(true);

        QueryPage<Bucket> buckets = provider.buckets(jobId, bq.build());
        assertEquals(1l, buckets.hitCount());
        QueryBuilder query = queryBuilder.getValue();
        String queryString = query.toString();
        assertTrue(queryString.matches("(?s).*maxNormalizedProbability[^}]*from. : 10\\.9.*"));
        assertTrue(queryString.matches("(?s).*anomalyScore[^}]*from. : 5\\.1.*"));
        assertFalse(queryString.matches("(?s).*isInterim.*"));
    }

    public void testBucket_NoBucketNoExpandNoInterim() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
        String jobId = "TestJobIdentification";
        Long timestamp = 98765432123456789L;
        Date now = new Date();
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", now.getTime());
        //source.add(map);

        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(false, source);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId, Bucket.TYPE, 0, 0, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        BucketQueryBuilder bq = new BucketQueryBuilder(Long.toString(timestamp));

        Optional<Bucket> bucket = provider.bucket(jobId, bq.build());
        assertFalse(bucket.isPresent());
    }

    public void testBucket_OneBucketNoExpandNoInterim() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
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
                .prepareSearch("prelertresults-" + jobId, Bucket.TYPE, 0, 0, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);


        BucketQueryBuilder bq = new BucketQueryBuilder(Long.toString(now.getTime()));

        Optional<Bucket> bucketHolder = provider.bucket(jobId, bq.build());
        assertTrue(bucketHolder.isPresent());
        Bucket b = bucketHolder.get();
        assertEquals(now, b.getTimestamp());
    }

    public void testBucket_OneBucketNoExpandInterim() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
        String jobId = "TestJobIdentification";
        Date now = new Date();
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("@timestamp", now.getTime());
        map.put("isInterim", true);
        source.add(map);

        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId, Bucket.TYPE, 0, 0, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        BucketQueryBuilder bq = new BucketQueryBuilder(Long.toString(now.getTime()));

        Optional<Bucket> bucketHolder = provider.bucket(jobId, bq.build());
        assertFalse(bucketHolder.isPresent());
    }

    public void testRecords() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
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
                .prepareSearch("prelertresults-" + jobId, AnomalyRecord.TYPE, skip, take, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        RecordsQueryBuilder rqb = new RecordsQueryBuilder()
                .skip(skip)
                .take(take)
                .epochStart(now.getTime())
                .epochEnd(now.getTime())
                .includeInterim(true)
                .sortField(sortfield)
                .anomalyScoreFilter(11.1)
                .normalizedProbability(2.2);

        QueryPage<AnomalyRecord> recordPage = provider.records(jobId, rqb.build());
        assertEquals(2L, recordPage.hitCount());
        List<AnomalyRecord> records = recordPage.hits();
        assertEquals(22.4, records.get(0).getTypical()[0], 0.000001);
        assertEquals(33.3, records.get(0).getActual()[0], 0.000001);
        assertEquals("irritable", records.get(0).getFunction());
        assertEquals(1122.4, records.get(1).getTypical()[0], 0.000001);
        assertEquals(933.3, records.get(1).getActual()[0], 0.000001);
        assertEquals("irrascible", records.get(1).getFunction());
    }

    public void testRecords_UsingBuilder() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
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
                .prepareSearch("prelertresults-" + jobId, AnomalyRecord.TYPE, skip, take, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        RecordsQueryBuilder rqb = new RecordsQueryBuilder();
        rqb.skip(skip);
        rqb.take(take);
        rqb.epochStart(now.getTime());
        rqb.epochEnd(now.getTime());
        rqb.includeInterim(true);
        rqb.sortField(sortfield);
        rqb.anomalyScoreFilter(11.1);
        rqb.normalizedProbability(2.2);

        QueryPage<AnomalyRecord> recordPage = provider.records(jobId, rqb.build());
        assertEquals(2L, recordPage.hitCount());
        List<AnomalyRecord> records = recordPage.hits();
        assertEquals(22.4, records.get(0).getTypical()[0], 0.000001);
        assertEquals(33.3, records.get(0).getActual()[0], 0.000001);
        assertEquals("irritable", records.get(0).getFunction());
        assertEquals(1122.4, records.get(1).getTypical()[0], 0.000001);
        assertEquals(933.3, records.get(1).getActual()[0], 0.000001);
        assertEquals("irrascible", records.get(1).getFunction());
    }


    public void testBucketRecords() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
        String jobId = "TestJobIdentification";
        Date now = new Date();
        Bucket bucket = mock(Bucket.class);
        when(bucket.getTimestamp()).thenReturn(now);

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
                .prepareSearch("prelertresults-" + jobId, AnomalyRecord.TYPE, skip, take, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        QueryPage<AnomalyRecord> recordPage = provider.bucketRecords(jobId, bucket, skip, take, true, sortfield, true, "");

        assertEquals(2L, recordPage.hitCount());
        List<AnomalyRecord> records = recordPage.hits();
        assertEquals(22.4, records.get(0).getTypical()[0], 0.000001);
        assertEquals(33.3, records.get(0).getActual()[0], 0.000001);
        assertEquals("irritable", records.get(0).getFunction());
        assertEquals(1122.4, records.get(1).getTypical()[0], 0.000001);
        assertEquals(933.3, records.get(1).getActual()[0], 0.000001);
        assertEquals("irrascible", records.get(1).getFunction());
    }

    public void testexpandBucket() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
        String jobId = "TestJobIdentification";
        Date now = new Date();
        Bucket bucket = new Bucket();
        bucket.setTimestamp(now);

        List<Map<String, Object>> source = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            Map<String, Object> recordMap = new HashMap<>();
            recordMap.put("typical", 22.4 + i);
            recordMap.put("actual", 33.3 + i);
            recordMap.put("timestamp", now.getTime());
            recordMap.put("function", "irritable");
            recordMap.put("bucketSpan", 22);
            recordMap.put("_parent", "father");
            source.add(recordMap);
        }

        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearchAnySize("prelertresults-" + jobId, AnomalyRecord.TYPE, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        int records = provider.expandBucket(jobId, false, bucket);
        assertEquals(400L, records);
    }

    public void testexpandBucket_WithManyRecords() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
        String jobId = "TestJobIdentification";
        Date now = new Date();
        Bucket bucket = new Bucket();
        bucket.setTimestamp(now);

        List<Map<String, Object>> source = new ArrayList<>();
        for (int i = 0; i < 600; i++) {
            Map<String, Object> recordMap = new HashMap<>();
            recordMap.put("typical", 22.4 + i);
            recordMap.put("actual", 33.3 + i);
            recordMap.put("timestamp", now.getTime());
            recordMap.put("function", "irritable");
            recordMap.put("bucketSpan", 22);
            recordMap.put("_parent", "father");
            source.add(recordMap);
        }

        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearchAnySize("prelertresults-" + jobId, AnomalyRecord.TYPE, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        int records = provider.expandBucket(jobId, false, bucket);
        // This is not realistic, but is an artifact of the fact that the mock query
        // returns all the records, not a subset
        assertEquals(1200L, records);
    }

    public void testCategoryDefinitions() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
        String jobId = "TestJobIdentification";
        String terms = "the terms and conditions are not valid here";
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("categoryId", String.valueOf(map.hashCode()));
        map.put("terms", terms);

        source.add(map);

        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        int skip = 0;
        int take = 10;
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId, CategoryDefinition.TYPE, skip, take, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        QueryPage<CategoryDefinition> categoryDefinitions = provider.categoryDefinitions(jobId, skip, take);
        assertEquals(1l, categoryDefinitions.hitCount());
        assertEquals(terms, categoryDefinitions.hits().get(0).getTerms());
    }

    public void testCategoryDefinition() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
        String jobId = "TestJobIdentification";
        String terms = "the terms and conditions are not valid here";

        Map<String, Object> source = new HashMap<>();
        String categoryId = String.valueOf(source.hashCode());
        source.put("categoryId", categoryId);
        source.put("terms", terms);

        GetResponse getResponse = createGetResponse(true, source);

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareGet("prelertresults-" + jobId, CategoryDefinition.TYPE, categoryId, getResponse);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);
        Optional<CategoryDefinition> categoryDefinition = provider.categoryDefinition(jobId, categoryId);
        assertTrue(categoryDefinition.isPresent());
        CategoryDefinition category = categoryDefinition.get();
        assertEquals(terms, category.getTerms());
    }

    public void testInfluencers_NoInterim() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
        String jobId = "TestJobIdentificationForInfluencers";
        Date now = new Date();
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> recordMap1 = new HashMap<>();
        recordMap1.put("probability", 0.555);
        recordMap1.put("influencerFieldName", "Builder");
        recordMap1.put("@timestamp", now.getTime());
        recordMap1.put("influencerFieldValue", "Bob");
        recordMap1.put("initialAnomalyScore", 22.2);
        recordMap1.put("anomalyScore", 22.6);
        Map<String, Object> recordMap2 = new HashMap<>();
        recordMap2.put("probability", 0.99);
        recordMap2.put("influencerFieldName", "Builder");
        recordMap2.put("@timestamp", now.getTime());
        recordMap2.put("influencerFieldValue", "James");
        recordMap2.put("initialAnomalyScore", 5.0);
        recordMap2.put("anomalyScore", 5.0);
        source.add(recordMap1);
        source.add(recordMap2);

        int skip = 4;
        int take = 3;
        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId, Influencer.TYPE, skip, take, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        QueryPage<Influencer> page = provider.influencers(jobId, skip, take, false);
        assertEquals(2L, page.hitCount());

        String queryString = queryBuilder.getValue().toString();
        System.out.println(queryString);
        assertTrue(queryString.matches("(?s).*must_not[^}]*term[^}]*isInterim.*value. : .true.*"));

        List<Influencer> records = page.hits();
        assertEquals("Bob", records.get(0).getInfluencerFieldValue());
        assertEquals("Builder", records.get(0).getInfluencerFieldName());
        assertEquals(now, records.get(0).getTimestamp());
        assertEquals(0.555, records.get(0).getProbability(), 0.00001);
        assertEquals(22.6, records.get(0).getAnomalyScore(), 0.00001);
        assertEquals(22.2, records.get(0).getInitialAnomalyScore(), 0.00001);

        assertEquals("James", records.get(1).getInfluencerFieldValue());
        assertEquals("Builder", records.get(1).getInfluencerFieldName());
        assertEquals(now, records.get(1).getTimestamp());
        assertEquals(0.99, records.get(1).getProbability(), 0.00001);
        assertEquals(5.0, records.get(1).getAnomalyScore(), 0.00001);
        assertEquals(5.0, records.get(1).getInitialAnomalyScore(), 0.00001);
    }


    public void testInfluencers_WithInterim() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
        String jobId = "TestJobIdentificationForInfluencers";
        Date now = new Date();
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> recordMap1 = new HashMap<>();
        recordMap1.put("probability", 0.555);
        recordMap1.put("influencerFieldName", "Builder");
        recordMap1.put("@timestamp", now.getTime());
        recordMap1.put("influencerFieldValue", "Bob");
        recordMap1.put("initialAnomalyScore", 22.2);
        recordMap1.put("anomalyScore", 22.6);
        Map<String, Object> recordMap2 = new HashMap<>();
        recordMap2.put("probability", 0.99);
        recordMap2.put("influencerFieldName", "Builder");
        recordMap2.put("@timestamp", now.getTime());
        recordMap2.put("influencerFieldValue", "James");
        recordMap2.put("initialAnomalyScore", 5.0);
        recordMap2.put("anomalyScore", 5.0);
        source.add(recordMap1);
        source.add(recordMap2);

        int skip = 4;
        int take = 3;
        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId, Influencer.TYPE, skip, take, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        QueryPage<Influencer> page = provider.influencers(jobId, skip, take, 0l, 0l, "sort", true, 0.0, true);
        assertEquals(2L, page.hitCount());

        String queryString = queryBuilder.getValue().toString();
        assertFalse(queryString.matches("(?s).*isInterim.*"));

        List<Influencer> records = page.hits();
        assertEquals("Bob", records.get(0).getInfluencerFieldValue());
        assertEquals("Builder", records.get(0).getInfluencerFieldName());
        assertEquals(now, records.get(0).getTimestamp());
        assertEquals(0.555, records.get(0).getProbability(), 0.00001);
        assertEquals(22.6, records.get(0).getAnomalyScore(), 0.00001);
        assertEquals(22.2, records.get(0).getInitialAnomalyScore(), 0.00001);

        assertEquals("James", records.get(1).getInfluencerFieldValue());
        assertEquals("Builder", records.get(1).getInfluencerFieldName());
        assertEquals(now, records.get(1).getTimestamp());
        assertEquals(0.99, records.get(1).getProbability(), 0.00001);
        assertEquals(5.0, records.get(1).getAnomalyScore(), 0.00001);
        assertEquals(5.0, records.get(1).getInitialAnomalyScore(), 0.00001);
    }

    public void testInfluencer() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
        String jobId = "TestJobIdentificationForInfluencers";
        String influencerId = "ThisIsAnInfluencerId";

        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        try {
            provider.influencer(jobId, influencerId);
            assertTrue(false);
        } catch (IllegalStateException e) {
        }
    }

    public void testModelSnapshots() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
        String jobId = "TestJobIdentificationForInfluencers";
        Date now = new Date();
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> recordMap1 = new HashMap<>();
        recordMap1.put("description", "snapshot1");
        recordMap1.put("restorePriority", 1);
        recordMap1.put("@timestamp", now.getTime());
        recordMap1.put("snapshotDocCount", 5);
        recordMap1.put("latestRecordTimeStamp", now.getTime());
        recordMap1.put("latestResultTimeStamp", now.getTime());
        Map<String, Object> recordMap2 = new HashMap<>();
        recordMap2.put("description", "snapshot2");
        recordMap2.put("restorePriority", 999);
        recordMap2.put("@timestamp", now.getTime());
        recordMap2.put("snapshotDocCount", 6);
        recordMap2.put("latestRecordTimeStamp", now.getTime());
        recordMap2.put("latestResultTimeStamp", now.getTime());
        source.add(recordMap1);
        source.add(recordMap2);

        int skip = 4;
        int take = 3;
        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId, ModelSnapshot.TYPE, skip, take, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        QueryPage<ModelSnapshot> page = provider.modelSnapshots(jobId, skip, take);
        assertEquals(2L, page.hitCount());
        List<ModelSnapshot> snapshots = page.hits();

        assertEquals(now, snapshots.get(0).getTimestamp());
        assertEquals(now, snapshots.get(0).getLatestRecordTimeStamp());
        assertEquals(now, snapshots.get(0).getLatestResultTimeStamp());
        assertEquals("snapshot1", snapshots.get(0).getDescription());
        assertEquals(1L, snapshots.get(0).getRestorePriority());
        assertEquals(5, snapshots.get(0).getSnapshotDocCount());

        assertEquals(now, snapshots.get(1).getTimestamp());
        assertEquals(now, snapshots.get(1).getLatestRecordTimeStamp());
        assertEquals(now, snapshots.get(1).getLatestResultTimeStamp());
        assertEquals("snapshot2", snapshots.get(1).getDescription());
        assertEquals(999L, snapshots.get(1).getRestorePriority());
        assertEquals(6, snapshots.get(1).getSnapshotDocCount());
    }


    public void testModelSnapshots_WithDescription() throws InterruptedException, ExecutionException, UnknownJobException, JsonParseException, IOException {
        String jobId = "TestJobIdentificationForInfluencers";
        Date now = new Date();
        List<Map<String, Object>> source = new ArrayList<>();

        Map<String, Object> recordMap1 = new HashMap<>();
        recordMap1.put("description", "snapshot1");
        recordMap1.put("restorePriority", 1);
        recordMap1.put("@timestamp", now.getTime());
        recordMap1.put("snapshotDocCount", 5);
        recordMap1.put("latestRecordTimeStamp", now.getTime());
        recordMap1.put("latestResultTimeStamp", now.getTime());
        Map<String, Object> recordMap2 = new HashMap<>();
        recordMap2.put("description", "snapshot2");
        recordMap2.put("restorePriority", 999);
        recordMap2.put("@timestamp", now.getTime());
        recordMap2.put("snapshotDocCount", 6);
        recordMap2.put("latestRecordTimeStamp", now.getTime());
        recordMap2.put("latestResultTimeStamp", now.getTime());
        source.add(recordMap1);
        source.add(recordMap2);

        int skip = 4;
        int take = 3;
        ArgumentCaptor<QueryBuilder> queryBuilder = ArgumentCaptor.forClass(QueryBuilder.class);
        SearchResponse response = createSearchResponse(true, source);
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addClusterStatusYellowResponse()
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .prepareSearch("prelertresults-" + jobId, ModelSnapshot.TYPE, skip, take, response, queryBuilder);

        Client client = clientBuilder.build();
        ElasticsearchJobProvider provider = createProvider(client);

        QueryPage<ModelSnapshot> page = provider.modelSnapshots(jobId, skip, take, 0, 0,
                "sortfield", true, "snappyId", "description1");
        assertEquals(2L, page.hitCount());
        List<ModelSnapshot> snapshots = page.hits();

        assertEquals(now, snapshots.get(0).getTimestamp());
        assertEquals(now, snapshots.get(0).getLatestRecordTimeStamp());
        assertEquals(now, snapshots.get(0).getLatestResultTimeStamp());
        assertEquals("snapshot1", snapshots.get(0).getDescription());
        assertEquals(1L, snapshots.get(0).getRestorePriority());
        assertEquals(5, snapshots.get(0).getSnapshotDocCount());

        assertEquals(now, snapshots.get(1).getTimestamp());
        assertEquals(now, snapshots.get(1).getLatestRecordTimeStamp());
        assertEquals(now, snapshots.get(1).getLatestResultTimeStamp());
        assertEquals("snapshot2", snapshots.get(1).getDescription());
        assertEquals(999L, snapshots.get(1).getRestorePriority());
        assertEquals(6, snapshots.get(1).getSnapshotDocCount());

        String queryString = queryBuilder.getValue().toString();
        System.out.println(queryString);
        assertTrue(queryString.matches("(?s).*snapshotId.*value. : .snappyId.*description.*value. : .description1.*"));
    }

    public void testMergePartitionScoresIntoBucket()
            throws InterruptedException, ExecutionException {
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .addClusterStatusYellowResponse();

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        List<ElasticsearchJobProvider.ScoreTimestamp> scores = new ArrayList<>();
        scores.add(provider.new ScoreTimestamp(new Date(2), 1.0));
        scores.add(provider.new ScoreTimestamp(new Date(3), 2.0));
        scores.add(provider.new ScoreTimestamp(new Date(5), 3.0));

        List<Bucket> buckets = new ArrayList<>();
        buckets.add(createBucketAtEpochTime(1));
        buckets.add(createBucketAtEpochTime(2));
        buckets.add(createBucketAtEpochTime(3));
        buckets.add(createBucketAtEpochTime(4));
        buckets.add(createBucketAtEpochTime(5));
        buckets.add(createBucketAtEpochTime(6));

        provider.mergePartitionScoresIntoBucket(scores, buckets);
        assertEquals(0.0, buckets.get(0).getMaxNormalizedProbability(), 0.001);
        assertEquals(1.0, buckets.get(1).getMaxNormalizedProbability(), 0.001);
        assertEquals(2.0, buckets.get(2).getMaxNormalizedProbability(), 0.001);
        assertEquals(0.0, buckets.get(3).getMaxNormalizedProbability(), 0.001);
        assertEquals(3.0, buckets.get(4).getMaxNormalizedProbability(), 0.001);
        assertEquals(0.0, buckets.get(5).getMaxNormalizedProbability(), 0.001);
    }

    public void testMergePartitionScoresIntoBucket_WithEmptyScoresList()
            throws InterruptedException, ExecutionException {
        MockClientBuilder clientBuilder = new MockClientBuilder(CLUSTER_NAME)
                .addIndicesExistsResponse(ElasticsearchJobProvider.PRELERT_USAGE_INDEX, true)
                .addClusterStatusYellowResponse();

        ElasticsearchJobProvider provider = createProvider(clientBuilder.build());

        List<ElasticsearchJobProvider.ScoreTimestamp> scores = new ArrayList<>();

        List<Bucket> buckets = new ArrayList<>();
        buckets.add(createBucketAtEpochTime(1));
        buckets.add(createBucketAtEpochTime(2));
        buckets.add(createBucketAtEpochTime(3));
        buckets.add(createBucketAtEpochTime(4));

        provider.mergePartitionScoresIntoBucket(scores, buckets);
        assertEquals(0.0, buckets.get(0).getMaxNormalizedProbability(), 0.001);
        assertEquals(0.0, buckets.get(1).getMaxNormalizedProbability(), 0.001);
        assertEquals(0.0, buckets.get(2).getMaxNormalizedProbability(), 0.001);
        assertEquals(0.0, buckets.get(3).getMaxNormalizedProbability(), 0.001);
    }

    private Bucket createBucketAtEpochTime(long epoch) {
        Bucket b = new Bucket();
        b.setTimestamp(new Date(epoch));
        b.setMaxNormalizedProbability(10.0);
        return b;
    }

    private ElasticsearchJobProvider createProvider(Client client) {
        return new ElasticsearchJobProvider(node, client, 0);
    }

    private static GetResponse createGetResponse(boolean exists, Map<String, Object> source) {
        GetResponse getResponse = mock(GetResponse.class);
        when(getResponse.isExists()).thenReturn(exists);
        when(getResponse.getSource()).thenReturn(source);
        return getResponse;
    }

    private static SearchResponse createSearchResponse(boolean exists, List<Map<String, Object>> source) {
        SearchResponse response = mock(SearchResponse.class);
        SearchHits hits = mock(SearchHits.class);
        List<SearchHit> list = new ArrayList<>();

        for (Map<String, Object> map : source) {
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
