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

import org.elasticsearch.test.ESTestCase;

// NOCOMMIT fix this test to not use system properties
public class JobManagerTests extends ESTestCase {

    // private ArgumentCaptor<Map<String, Object>> jobUpdateCaptor;
    //
    // private ClusterService clusterService;
    // private JobProvider jobProvider;
    // private Auditor auditor;
    // private JobDataDeleterFactory jobDataDeleter;
    //
    // @SuppressWarnings({ "unchecked", "rawtypes" })
    // @Before
    // public void setupMocks()
    // {
    // jobUpdateCaptor = ArgumentCaptor.forClass((Class) HashMap.class);
    //
    // clusterService = mock(ClusterService.class);
    // jobProvider = mock(JobProvider.class);
    // auditor = mock(Auditor.class);
    // jobDataDeleter = mock(JobDataDeleterFactory.class);
    //
    // when(jobProvider.jobIdIsUnique("not-unique")).thenReturn(false);
    // when(jobProvider.jobIdIsUnique(not(eq("not-unique")))).thenReturn(true);
    // when(jobProvider.audit(anyString())).thenReturn(auditor);
    // }
    //
    // @After
    // public void clearSystemProp()
    // {
    // System.clearProperty("max.jobs.factor");
    // }
    //
    // public void testGetJob() throws UnknownJobException {
    // JobManager jobManager = createJobManager();
    // PrelertMetadata.Builder builder = new PrelertMetadata.Builder();
    // builder.putJob(new Job(new JobConfiguration("foo").build()), false);
    // ClusterState clusterState = ClusterState.builder(new ClusterName("name"))
    // .metaData(MetaData.builder().putCustom(PrelertMetadata.TYPE,
    // builder.build()))
    // .build();
    // Optional<JobDetails> doc = jobManager.getJob("foo", clusterState);
    // assertTrue(doc.isPresent());
    // }
    //
    // public void testFilter()
    //
    // {
    // Set<String> running = new HashSet<String>(Arrays.asList("henry", "dim",
    // "dave"));
    // Set<String> diff = new HashSet<String>(Arrays.asList("dave",
    // "tom")).stream()
    // .filter((s) -> !running.contains(s))
    // .collect(Collectors.toCollection(HashSet::new));
    //
    // assertTrue(diff.size() == 1);
    // assertTrue(diff.contains("tom"));
    // }
    //
    // public void testDeleteJob_GivenJobActionIsNotAvailable() throws
    // UnknownJobException, InterruptedException, ExecutionException {
    // JobManager jobManager = createJobManager();
    // ClusterState clusterState = ClusterState.builder(new
    // ClusterName("_name")).build();
    // JobDetails jobDetails = new JobConfiguration().build();
    // jobDetails.setJobId("foo");
    // clusterState = jobManager.innerPutJob(jobDetails, false, clusterState);
    // when(clusterService.state()).thenReturn(clusterState);
    //
    // doAnswerSleep(200).when(clusterService).submitStateUpdateTask(eq("delete-job-foo"),
    // any(AckedClusterStateUpdateTask.class));
    //
    // ExecutorService executor = Executors.newFixedThreadPool(2);
    // DeleteJobAction.Request request = new DeleteJobAction.Request("foo");
    // request.setJobId("foo");
    // @SuppressWarnings("unchecked")
    // ActionListener<DeleteJobAction.Response> actionListener =
    // mock(ActionListener.class);
    // Future<Throwable> task_1_result = executor.submit(new
    // ExceptionCallable(() -> jobManager.deleteJob(request, actionListener)));
    // Future<Throwable> task_2_result = executor.submit(new
    // ExceptionCallable(() -> jobManager.deleteJob(request, actionListener)));
    // executor.shutdown();
    //
    // Throwable result1 = task_1_result.get();
    // Throwable result2 = task_2_result.get();
    // assertTrue(result1 == null || result2 == null);
    // Throwable exception = result1 != null ? result1 : result2;
    // assertTrue(exception instanceof ElasticsearchStatusException);
    // assertEquals("Cannot delete job foo while another connection is deleting
    // the job",
    // exception.getMessage());
    // }
    //
    // public void testRemoveJobFromClusterState_GivenExistingMetadata() {
    // JobManager jobManager = createJobManager();
    // ClusterState clusterState = ClusterState.builder(new
    // ClusterName("_name")).build();
    // JobDetails jobDetails = new JobConfiguration().build();
    // jobDetails.setJobId("foo");
    // clusterState = jobManager.innerPutJob(jobDetails, false, clusterState);
    //
    // clusterState = jobManager.removeJobFromClusterState("foo", clusterState);
    //
    // PrelertMetadata prelertMetadata =
    // clusterState.metaData().custom(PrelertMetadata.TYPE);
    // assertThat(prelertMetadata.getJobs().containsKey("foo"), is(false));
    // }
    //
    // public void testRemoveJobFromClusterState_GivenNoMetadata() {
    // JobManager jobManager = createJobManager();
    // ClusterState clusterStateBefore = ClusterState.builder(new
    // ClusterName("_name")).build();
    // JobDetails jobDetails = new JobConfiguration().build();
    // jobDetails.setJobId("foo");
    //
    // ClusterState clusterStateAfter =
    // jobManager.removeJobFromClusterState("foo", clusterStateBefore);
    //
    // assertThat(clusterStateAfter, is(equalTo(clusterStateBefore)));
    // }
    //
    // public void testGetJobOrThrowIfUnknown_GivenUnknownJob()
    // {
    // JobManager jobManager = createJobManager();
    // ClusterState cs = ClusterState.builder(new ClusterName("_name")).build();
    // ESTestCase.expectThrows(ResourceNotFoundException.class, () ->
    // jobManager.getJobOrThrowIfUnknown(cs, "foo"));
    // }
    //
    //
    // public void testGetJobOrThrowIfUnknown_GivenKnownJob() throws
    // UnknownJobException
    // {
    // JobManager jobManager = createJobManager();
    // JobDetails job = new JobConfiguration().build();
    // job.setId("foo");
    // PrelertMetadata prelertMetadata = new
    // PrelertMetadata.Builder().putJob(new Job(job), false).build();
    // ClusterState cs = ClusterState.builder(new ClusterName("_name"))
    // .metaData(MetaData.builder().putCustom(PrelertMetadata.TYPE,
    // prelertMetadata)).build();
    //
    // assertEquals(job, jobManager.getJobOrThrowIfUnknown(cs, "foo"));
    // }
    //
    // public void testGetJobs() {
    // PrelertMetadata.Builder prelertMetadata = new PrelertMetadata.Builder();
    // for (int i = 0; i < 10; i++) {
    // prelertMetadata.putJob(new Job(new
    // JobConfiguration(Integer.toString(i)).build()), false);
    // }
    // ClusterState clusterState = ClusterState.builder(new
    // ClusterName("_name"))
    // .metaData(MetaData.builder().putCustom(PrelertMetadata.TYPE,
    // prelertMetadata.build()))
    // .build();
    //
    // JobManager jobManager = createJobManager();
    // QueryPage<JobDetails> result = jobManager.getJobs(0, 10, clusterState);
    // assertThat(result.hitCount(), equalTo(10L));
    // assertThat(result.hits().get(0).getId(), equalTo("0"));
    // assertThat(result.hits().get(1).getId(), equalTo("1"));
    // assertThat(result.hits().get(2).getId(), equalTo("2"));
    // assertThat(result.hits().get(3).getId(), equalTo("3"));
    // assertThat(result.hits().get(4).getId(), equalTo("4"));
    // assertThat(result.hits().get(5).getId(), equalTo("5"));
    // assertThat(result.hits().get(6).getId(), equalTo("6"));
    // assertThat(result.hits().get(7).getId(), equalTo("7"));
    // assertThat(result.hits().get(8).getId(), equalTo("8"));
    // assertThat(result.hits().get(9).getId(), equalTo("9"));
    //
    // result = jobManager.getJobs(0, 5, clusterState);
    // assertThat(result.hitCount(), equalTo(10L));
    // assertThat(result.hits().get(0).getId(), equalTo("0"));
    // assertThat(result.hits().get(1).getId(), equalTo("1"));
    // assertThat(result.hits().get(2).getId(), equalTo("2"));
    // assertThat(result.hits().get(3).getId(), equalTo("3"));
    // assertThat(result.hits().get(4).getId(), equalTo("4"));
    //
    // result = jobManager.getJobs(5, 5, clusterState);
    // assertThat(result.hitCount(), equalTo(10L));
    // assertThat(result.hits().get(0).getId(), equalTo("5"));
    // assertThat(result.hits().get(1).getId(), equalTo("6"));
    // assertThat(result.hits().get(2).getId(), equalTo("7"));
    // assertThat(result.hits().get(3).getId(), equalTo("8"));
    // assertThat(result.hits().get(4).getId(), equalTo("9"));
    //
    // result = jobManager.getJobs(9, 1, clusterState);
    // assertThat(result.hitCount(), equalTo(10L));
    // assertThat(result.hits().get(0).getId(), equalTo("9"));
    //
    // result = jobManager.getJobs(9, 10, clusterState);
    // assertThat(result.hitCount(), equalTo(10L));
    // assertThat(result.hits().get(0).getId(), equalTo("9"));
    // }
    //
    // public void testInnerPutJob() {
    // JobManager jobManager = createJobManager();
    // ClusterState cs = ClusterState.builder(new ClusterName("_name")).build();
    //
    // JobDetails jobDetails1 = new JobConfiguration("_id").build();
    // ClusterState result1 = jobManager.innerPutJob(jobDetails1, false, cs);
    // PrelertMetadata pm = result1.getMetaData().custom(PrelertMetadata.TYPE);
    // assertThat(pm.getJobs().get("_id").getJobDetails(),
    // sameInstance(jobDetails1));
    //
    // JobDetails jobDetails2 = new JobConfiguration("_id").build();
    // expectThrows(ElasticsearchStatusException.class, () ->
    // jobManager.innerPutJob(jobDetails2, false, result1));
    //
    // ClusterState result2 = jobManager.innerPutJob(jobDetails2, true,
    // result1);
    // pm = result2.getMetaData().custom(PrelertMetadata.TYPE);
    // assertThat(pm.getJobs().get("_id").getJobDetails(),
    // sameInstance(jobDetails2));
    // }
    //
    // private List<String> createJobIds(int jobCount)
    // {
    // List<String> jobIds = new ArrayList<>();
    // for (int i=0; i<jobCount; i++)
    // {
    // jobIds.add(Integer.toString(i));
    // }
    // return jobIds;
    // }
    //
    // private JobManager createJobManager() {
    // Environment env = new Environment(
    // Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(),
    // createTempDir().toString()).build());
    // return new JobManager(env, jobProvider, clusterService, new
    // LocalActionGuardian<>(Action.CLOSED));
    // }
    //
    // private static Stubber doAnswerSleep(long millis)
    // {
    // return doAnswer(new Answer<Void>()
    // {
    // @Override
    // public Void answer(InvocationOnMock invocation) throws Throwable
    // {
    // Thread.sleep(millis);
    // return null;
    // }
    // });
    // }
    //
    // private static class ExceptionCallable implements Callable<Throwable>
    // {
    // private interface ExceptionTask
    // {
    // void run() throws Exception;
    // }
    //
    // private final ExceptionTask task;
    //
    // private ExceptionCallable(ExceptionTask task)
    // {
    // this.task = task;
    // }
    //
    // @Override
    // public Throwable call()
    // {
    // try
    // {
    // task.run();
    // } catch (Exception e)
    // {
    // return e;
    // }
    // return null;
    // }
    // }
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
