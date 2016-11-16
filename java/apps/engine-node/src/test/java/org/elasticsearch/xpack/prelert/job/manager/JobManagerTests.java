/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.manager;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.AckedClusterStateUpdateTask;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.action.DeleteJobAction;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.audit.Auditor;
import org.elasticsearch.xpack.prelert.job.manager.actions.Action;
import org.elasticsearch.xpack.prelert.job.manager.actions.LocalActionGuardian;
import org.elasticsearch.xpack.prelert.job.metadata.PrelertMetadata;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

import static org.elasticsearch.xpack.prelert.job.JobTests.buildJobBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobManagerTests extends ESTestCase {

    private ClusterService clusterService;
    private JobProvider jobProvider;
    private Auditor auditor;

    @Before
    public void setupMocks() {
        clusterService = mock(ClusterService.class);
        jobProvider = mock(JobProvider.class);
        auditor = mock(Auditor.class);
        when(jobProvider.audit(anyString())).thenReturn(auditor);
    }

    public void testGetJob() {
        JobManager jobManager = createJobManager();
        PrelertMetadata.Builder builder = new PrelertMetadata.Builder();
        builder.putJob(buildJobBuilder("foo").build(), false);
        ClusterState clusterState = ClusterState.builder(new ClusterName("name"))
                .metaData(MetaData.builder().putCustom(PrelertMetadata.TYPE, builder.build())).build();
        Optional<Job> doc = jobManager.getJob("foo", clusterState);
        assertTrue(doc.isPresent());
    }

    public void testFilter() {
        Set<String> running = new HashSet<String>(Arrays.asList("henry", "dim", "dave"));
        Set<String> diff = new HashSet<>(Arrays.asList("dave", "tom")).stream().filter((s) -> !running.contains(s))
                .collect(Collectors.toCollection(HashSet::new));

        assertTrue(diff.size() == 1);
        assertTrue(diff.contains("tom"));
    }

    public void testDeleteJob_GivenJobActionIsNotAvailable() throws InterruptedException, ExecutionException {
        JobManager jobManager = createJobManager();
        ClusterState clusterState = createClusterState();
        Job job = buildJobBuilder("foo").build();
        clusterState = jobManager.innerPutJob(job, false, clusterState);
        PrelertMetadata currentPrelertMetadata = clusterState.metaData().custom(PrelertMetadata.TYPE);
        PrelertMetadata.Builder builder = new PrelertMetadata.Builder(currentPrelertMetadata);
        builder.putAllocation("nodeId", "foo");
        clusterState = ClusterState.builder(clusterState).metaData(MetaData.builder(clusterState.getMetaData())
                .putCustom(PrelertMetadata.TYPE, builder.build()).build()).build();
        when(clusterService.state()).thenReturn(clusterState);

        doAnswerSleep(200).when(clusterService).submitStateUpdateTask(eq("delete-job-foo"), any(AckedClusterStateUpdateTask.class));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        DeleteJobAction.Request request = new DeleteJobAction.Request("foo");
        request.setJobId("foo");
        @SuppressWarnings("unchecked")
        ActionListener<DeleteJobAction.Response> actionListener = mock(ActionListener.class);
        Future<Throwable> task_1_result = executor.submit(new ExceptionCallable(() -> jobManager.deleteJob(request, actionListener)));
        Future<Throwable> task_2_result = executor.submit(new ExceptionCallable(() -> jobManager.deleteJob(request, actionListener)));
        executor.shutdown();

        Throwable result1 = task_1_result.get();
        Throwable result2 = task_2_result.get();
        assertTrue(result1 == null || result2 == null);
        Throwable exception = result1 != null ? result1 : result2;
        assertTrue(exception instanceof RejectedExecutionException);
        assertEquals("Cannot delete job foo while another connection is deleting the job", exception.getMessage());
    }

    public void testRemoveJobFromClusterState() {
        JobManager jobManager = createJobManager();
        ClusterState clusterState = createClusterState();
        Job job = buildJobBuilder("foo").build();
        clusterState = jobManager.innerPutJob(job, false, clusterState);

        clusterState = jobManager.removeJobFromClusterState("foo", clusterState);

        PrelertMetadata prelertMetadata = clusterState.metaData().custom(PrelertMetadata.TYPE);
        assertThat(prelertMetadata.getJobs().containsKey("foo"), is(false));
    }

    public void testRemoveJobFromClusterState_jobMissing() {
        JobManager jobManager = createJobManager();
        ClusterState clusterState = createClusterState();
        Job job = buildJobBuilder("foo").build();
        ClusterState clusterState2 = jobManager.innerPutJob(job, false, clusterState);
        Exception e = expectThrows(ResourceNotFoundException.class, () -> jobManager.removeJobFromClusterState("bar", clusterState2));
        assertThat(e.getMessage(), equalTo("job [bar] does not exist"));
    }

    public void testGetJobOrThrowIfUnknown_GivenUnknownJob() {
        JobManager jobManager = createJobManager();
        ClusterState cs = createClusterState();
        ESTestCase.expectThrows(ResourceNotFoundException.class, () -> jobManager.getJobOrThrowIfUnknown(cs, "foo"));
    }

    public void testGetJobOrThrowIfUnknown_GivenKnownJob() {
        JobManager jobManager = createJobManager();
        Job job = buildJobBuilder("foo").build();
        PrelertMetadata prelertMetadata = new PrelertMetadata.Builder().putJob(job, false).build();
        ClusterState cs = ClusterState.builder(new ClusterName("_name"))
                .metaData(MetaData.builder().putCustom(PrelertMetadata.TYPE, prelertMetadata)).build();

        assertEquals(job, jobManager.getJobOrThrowIfUnknown(cs, "foo"));
    }

    public void tesGetJobAllocation() {
        JobManager jobManager = createJobManager();
        Job job = buildJobBuilder("foo").build();
        PrelertMetadata prelertMetadata = new PrelertMetadata.Builder()
                .putJob(job, false)
                .putAllocation("nodeId", "foo")
                .build();
        ClusterState cs = ClusterState.builder(new ClusterName("_name"))
                .metaData(MetaData.builder().putCustom(PrelertMetadata.TYPE, prelertMetadata)).build();
        when(clusterService.state()).thenReturn(cs);

        assertEquals("nodeId", jobManager.getJobAllocation("foo").getNodeId());
        expectThrows(ResourceNotFoundException.class, () -> jobManager.getJobAllocation("bar"));
    }

    public void testGetJobs() {
        PrelertMetadata.Builder prelertMetadata = new PrelertMetadata.Builder();
        for (int i = 0; i < 10; i++) {
            prelertMetadata.putJob(buildJobBuilder(Integer.toString(i)).build(), false);
        }
        ClusterState clusterState = ClusterState.builder(new ClusterName("_name"))
                .metaData(MetaData.builder().putCustom(PrelertMetadata.TYPE, prelertMetadata.build())).build();

        JobManager jobManager = createJobManager();
        QueryPage<Job> result = jobManager.getJobs(0, 10, clusterState);
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
        ClusterState cs = createClusterState();

        Job job1 = buildJobBuilder("_id").build();
        ClusterState result1 = jobManager.innerPutJob(job1, false, cs);
        PrelertMetadata pm = result1.getMetaData().custom(PrelertMetadata.TYPE);
        assertThat(pm.getJobs().get("_id"), sameInstance(job1));

        Job job2 = buildJobBuilder("_id").build();
        expectThrows(ElasticsearchStatusException.class, () -> jobManager.innerPutJob(job2, false, result1));

        ClusterState result2 = jobManager.innerPutJob(job2, true, result1);
        pm = result2.getMetaData().custom(PrelertMetadata.TYPE);
        assertThat(pm.getJobs().get("_id"), sameInstance(job2));
    }

    private JobManager createJobManager() {
        Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build();
        Environment env = new Environment(
                settings);
        return new JobManager(env, settings, jobProvider, clusterService, new LocalActionGuardian<>(Action.CLOSED));
    }

    private static Stubber doAnswerSleep(long millis) {
        return doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(millis);
                return null;
            }
        });
    }

    private static class ExceptionCallable implements Callable<Throwable> {
        private interface ExceptionTask {
            void run() throws Exception;
        }

        private final ExceptionTask task;

        private ExceptionCallable(ExceptionTask task) {
            this.task = task;
        }

        @Override
        public Throwable call() {
            try {
                task.run();
            } catch (Exception e) {
                return e;
            }
            return null;
        }
    }

    private ClusterState createClusterState() {
        ClusterState.Builder builder = ClusterState.builder(new ClusterName("_name"));
        builder.metaData(MetaData.builder().putCustom(PrelertMetadata.TYPE, PrelertMetadata.PROTO));
        return builder.build();
    }
}
