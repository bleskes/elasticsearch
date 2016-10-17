/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */
package org.elasticsearch.xpack.prelert.job.manager;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.action.DeleteJobAction;
import org.elasticsearch.xpack.prelert.job.AnalysisLimits;
import org.elasticsearch.xpack.prelert.action.UpdateJobAction;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.audit.Auditor;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.exceptions.JobInUseException;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.manager.actions.Action;
import org.elasticsearch.xpack.prelert.job.manager.actions.LocalActionGuardian;
import org.elasticsearch.xpack.prelert.job.manager.actions.ScheduledAction;
import org.elasticsearch.xpack.prelert.job.metadata.Job;
import org.elasticsearch.xpack.prelert.job.metadata.PrelertMetadata;
import org.elasticsearch.xpack.prelert.job.persistence.DataStoreException;
import org.elasticsearch.xpack.prelert.job.persistence.JobDataDeleterFactory;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.junit.After;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobManagerTest extends ESTestCase {

    private ArgumentCaptor<Map<String, Object>> jobUpdateCaptor;

    private ClusterService clusterService;
    private JobProvider jobProvider;
    private Auditor auditor;
    private JobDataDeleterFactory jobDataDeleter;

    @Before
    public void setupMocks()
    {
        jobUpdateCaptor = ArgumentCaptor.forClass((Class) HashMap.class);

        clusterService = mock(ClusterService.class);
        jobProvider = mock(JobProvider.class);
        auditor = mock(Auditor.class);
        jobDataDeleter = mock(JobDataDeleterFactory.class);

        when(jobProvider.jobIdIsUnique("not-unique")).thenReturn(false);
        when(jobProvider.jobIdIsUnique(not(eq("not-unique")))).thenReturn(true);
        when(jobProvider.audit(anyString())).thenReturn(auditor);
    }

    @After
    public void clearSystemProp()
    {
        System.clearProperty("max.jobs.factor");
    }

    public void testGetJob() throws UnknownJobException {
        JobManager jobManager = createJobManager();
        PrelertMetadata.Builder builder = new PrelertMetadata.Builder();
        builder.putJob(new Job(new JobDetails("foo", new JobConfiguration())), false);
        ClusterState clusterState = ClusterState.builder(new ClusterName("name"))
                .metaData(MetaData.builder().putCustom(PrelertMetadata.TYPE, builder.build()))
                .build();
        Optional<JobDetails> doc = jobManager.getJob("foo", clusterState);
        assertTrue(doc.isPresent());
    }

    public void testFilter()

    {
        Set<String> running = new HashSet<String>(Arrays.asList("henry", "dim", "dave"));
        Set<String> diff = new HashSet<String>(Arrays.asList("dave", "tom")).stream()
                                    .filter((s) -> !running.contains(s))
                                    .collect(Collectors.toCollection(HashSet::new));

        assertTrue(diff.size() == 1);
        assertTrue(diff.contains("tom"));
    }

    public void testDeleteJob_GivenJobActionIsNotAvailable() throws UnknownJobException,
            DataStoreException, InterruptedException, ExecutionException {
        JobManager jobManager = createJobManager();

        doAnswerSleep(200).when(jobProvider).deleteJob("foo");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        DeleteJobAction.Request request = new DeleteJobAction.Request("foo");
        request.setJobId("foo");
        ActionListener<DeleteJobAction.Response> actionListener = new ActionListener<DeleteJobAction.Response>() {
            @Override
            public void onResponse(DeleteJobAction.Response response) {

            }

            @Override
            public void onFailure(Exception e) {

            }
        };
        Future<Throwable> task_1_result = executor.submit(new ExceptionCallable(() -> jobManager.deleteJob(request, actionListener)));
        Future<Throwable> task_2_result = executor.submit(new ExceptionCallable(() -> jobManager.deleteJob(request, actionListener)));
        executor.shutdown();

        Throwable result1 = task_1_result.get();
        Throwable result2 = task_2_result.get();
        assertTrue(result1 == null || result2 == null);
        Throwable exception = result1 != null ? result1 : result2;
        assertTrue(exception instanceof JobInUseException);
        assertEquals("Cannot delete job foo while another connection is deleting the job",
                exception.getMessage());

        verify(jobProvider).deleteJob("foo");
    }

    public void testRemoveJobFromClusterState_GivenExistingMetadata()  {
        JobManager jobManager = createJobManager();
        ClusterState clusterState = ClusterState.builder(new ClusterName("_name")).build();
        JobDetails jobDetails = new JobDetails();
        jobDetails.setJobId("foo");
        clusterState = jobManager.innerPutJob(jobDetails, false, clusterState);

        clusterState = jobManager.removeJobFromClusterState("foo", clusterState);

        PrelertMetadata prelertMetadata = clusterState.metaData().custom(PrelertMetadata.TYPE);
        assertThat(prelertMetadata.getJobs().containsKey("foo"), is(false));
    }

    public void testRemoveJobFromClusterState_GivenNoMetadata()  {
        JobManager jobManager = createJobManager();
        ClusterState clusterStateBefore = ClusterState.builder(new ClusterName("_name")).build();
        JobDetails jobDetails = new JobDetails();
        jobDetails.setJobId("foo");

        ClusterState clusterStateAfter = jobManager.removeJobFromClusterState("foo", clusterStateBefore);

        assertThat(clusterStateAfter, is(equalTo(clusterStateBefore)));
    }

    public void testGetJobOrThrowIfUnknown_GivenUnknownJob()
    {
        JobManager jobManager = createJobManager();
        ClusterState cs = ClusterState.builder(new ClusterName("_name")).build();
        ESTestCase.expectThrows(ResourceNotFoundException.class, () -> jobManager.getJobOrThrowIfUnknown(cs, "foo"));
    }


    public void testGetJobOrThrowIfUnknown_GivenKnownJob() throws UnknownJobException
    {
        JobManager jobManager = createJobManager();
        JobDetails job = new JobDetails();
        job.setId("foo");
        PrelertMetadata prelertMetadata = new PrelertMetadata.Builder().putJob(new Job(job), false).build();
        ClusterState cs = ClusterState.builder(new ClusterName("_name"))
                .metaData(MetaData.builder().putCustom(PrelertMetadata.TYPE, prelertMetadata)).build();

        assertEquals(job, jobManager.getJobOrThrowIfUnknown(cs, "foo"));
    }

    public void testGetJobs() {
        PrelertMetadata.Builder prelertMetadata = new PrelertMetadata.Builder();
        for (int i = 0; i < 10; i++) {
            prelertMetadata.putJob(new Job(new JobDetails(Integer.toString(i), new JobConfiguration())), false);
        }
        ClusterState clusterState = ClusterState.builder(new ClusterName("_name"))
                .metaData(MetaData.builder().putCustom(PrelertMetadata.TYPE, prelertMetadata.build()))
                .build();

        JobManager jobManager = createJobManager();
        QueryPage<JobDetails> result = jobManager.getJobs(0, 10, clusterState);
        assertThat(result.hitCount(), equalTo(10L));
        assertThat(result.hits().get(0).getId(), equalTo("0"));
        assertThat(result.hits().get(1).getId(), equalTo("1"));
        assertThat(result.hits().get(2).getId(), equalTo("2"));
        assertThat(result.hits().get(3).getId(), equalTo("3"));
        assertThat(result.hits().get(4).getId(), equalTo("4"));
        assertThat(result.hits().get(5).getId(), equalTo("5"));
        assertThat(result.hits().get(6).getId(), equalTo("6"));
        assertThat(result.hits().get(7).getId(), equalTo("7"));
        assertThat(result.hits().get(8).getId(), equalTo("8"));
        assertThat(result.hits().get(9).getId(), equalTo("9"));

        result = jobManager.getJobs(0, 5, clusterState);
        assertThat(result.hitCount(), equalTo(10L));
        assertThat(result.hits().get(0).getId(), equalTo("0"));
        assertThat(result.hits().get(1).getId(), equalTo("1"));
        assertThat(result.hits().get(2).getId(), equalTo("2"));
        assertThat(result.hits().get(3).getId(), equalTo("3"));
        assertThat(result.hits().get(4).getId(), equalTo("4"));

        result = jobManager.getJobs(5, 5, clusterState);
        assertThat(result.hitCount(), equalTo(10L));
        assertThat(result.hits().get(0).getId(), equalTo("5"));
        assertThat(result.hits().get(1).getId(), equalTo("6"));
        assertThat(result.hits().get(2).getId(), equalTo("7"));
        assertThat(result.hits().get(3).getId(), equalTo("8"));
        assertThat(result.hits().get(4).getId(), equalTo("9"));

        result = jobManager.getJobs(9, 1, clusterState);
        assertThat(result.hitCount(), equalTo(10L));
        assertThat(result.hits().get(0).getId(), equalTo("9"));

        result = jobManager.getJobs(9, 10, clusterState);
        assertThat(result.hitCount(), equalTo(10L));
        assertThat(result.hits().get(0).getId(), equalTo("9"));
    }

    public void testInnerPutJob() {
        JobManager jobManager = createJobManager();
        ClusterState cs = ClusterState.builder(new ClusterName("_name")).build();

        JobDetails jobDetails1 = new JobDetails("_id", new JobConfiguration());
        ClusterState result1 = jobManager.innerPutJob(jobDetails1, false, cs);
        PrelertMetadata pm = result1.getMetaData().custom(PrelertMetadata.TYPE);
        assertThat(pm.getJobs().get("_id").getJobDetails(), sameInstance(jobDetails1));

        JobDetails jobDetails2 = new JobDetails("_id", new JobConfiguration());
        expectThrows(ElasticsearchStatusException.class, () -> jobManager.innerPutJob(jobDetails2, false, result1));

        ClusterState result2 = jobManager.innerPutJob(jobDetails2, true, result1);
        pm = result2.getMetaData().custom(PrelertMetadata.TYPE);
        assertThat(pm.getJobs().get("_id").getJobDetails(), sameInstance(jobDetails2));
    }

    private List<String> createJobIds(int jobCount)
    {
        List<String> jobIds = new ArrayList<>();
        for (int i=0; i<jobCount; i++)
        {
            jobIds.add(Integer.toString(i));
        }
        return jobIds;
    }

    private JobManager createJobManager() {
        return new JobManager(jobProvider, clusterService, new LocalActionGuardian<>(Action.CLOSED),
                new LocalActionGuardian<>(ScheduledAction.STOPPED));
    }

    private static Stubber doAnswerSleep(long millis)
    {
        return doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                Thread.sleep(millis);
                return null;
            }
        });
    }

    private static class ExceptionCallable implements Callable<Throwable>
    {
        private interface ExceptionTask
        {
            void run() throws Exception;
        }

        private final ExceptionTask task;

        private ExceptionCallable(ExceptionTask task)
        {
            this.task = task;
        }

        @Override
        public Throwable call()
        {
            try
            {
                task.run();
            } catch (Exception e)
            {
                return e;
            }
            return null;
        }
    }
    /*
    private static MockBatchedDocumentsIterator<JobDetails> newBatchedJobsIterator(List<JobDetails> jobs)
    {
        Deque<JobDetails> batch1 = new ArrayDeque<>();
        Deque<JobDetails> batch2 = new ArrayDeque<>();
        for (int i = 0; i < jobs.size(); i++)
        {
            if (i == 0)
            {
                batch1.add(jobs.get(i));
            }
            else
            {
                batch2.add(jobs.get(i));
            }
        }
        List<Deque<JobDetails>> batches = new ArrayList<>();
        batches.add(batch1);
        batches.add(batch2);
        return new MockBatchedDocumentsIterator<>(batches);
    }


    private static Answer<Object> writeToWriter()
    {
        return new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws IOException
            {
                CsvRecordWriter writer = (CsvRecordWriter) invocation.getArguments()[1];
                writer.writeRecord(new String [] {"csv","header","one"});
                writer.flush();
                return null;
            }
        };
    }
    */
}
