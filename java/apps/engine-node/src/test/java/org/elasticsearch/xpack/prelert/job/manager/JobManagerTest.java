
package org.elasticsearch.xpack.prelert.job.manager;

import org.elasticsearch.xpack.prelert.job.*;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig.DataSource;
import org.elasticsearch.xpack.prelert.job.audit.Auditor;
import org.elasticsearch.xpack.prelert.job.exceptions.*;
import org.elasticsearch.xpack.prelert.job.manager.actions.Action;
import org.elasticsearch.xpack.prelert.job.manager.actions.LocalActionGuardian;
import org.elasticsearch.xpack.prelert.job.manager.actions.ScheduledAction;
import org.elasticsearch.xpack.prelert.job.persistence.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class JobManagerTest
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<Map<String, Object>> jobUpdateCaptor;

    @Mock private JobProvider jobProvider;
    @Mock private Auditor auditor;
    @Mock private JobDataDeleterFactory jobDataDeleter;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        when(jobProvider.jobIdIsUnique("not-unique")).thenReturn(false);
        when(jobProvider.jobIdIsUnique(not(eq("not-unique")))).thenReturn(true);
        when(jobProvider.audit(anyString())).thenReturn(auditor);
    }

    @After
    public void tearDown()
    {
        System.clearProperty("max.jobs.factor");
    }

    @Test
    public void testFilter()

    {
        Set<String> running = new HashSet<String>(Arrays.asList("henry", "dim", "dave"));
        Set<String> diff = new HashSet<String>(Arrays.asList("dave", "tom")).stream()
                                    .filter((s) -> !running.contains(s))
                                    .collect(Collectors.toCollection(HashSet::new));

        assertTrue(diff.size() == 1);
        assertTrue(diff.contains("tom"));
    }

    @Test
    public void testDeleteJob_GivenJobActionIsNotAvailable() throws UnknownJobException,
            DataStoreException, InterruptedException, ExecutionException
    {
        JobManager jobManager = createJobManager();

        when(jobProvider.getJobDetails("foo")).thenReturn(Optional.of(new JobDetails()));

        doAnswerSleep(200).when(jobProvider).deleteJob("foo");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Throwable> task_1_result = executor.submit(
                new ExceptionCallable(() -> jobManager.deleteJob("foo")));
        Future<Throwable> task_2_result = executor.submit(
                new ExceptionCallable(() -> jobManager.deleteJob("foo")));
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

    @SuppressWarnings("unchecked")
    @Test
    public void testSetModelDebugConfig_GivenConfig() throws UnknownJobException
    {
        ModelDebugConfig config = new ModelDebugConfig(85.0, "bar");
        JobManager jobManager = createJobManager();

        jobManager.setModelDebugConfig("foo", config);

        verify(jobProvider).updateJob(eq("foo"), jobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = jobUpdateCaptor.getValue();
        Map<String, Object> configUpdate = (Map<String, Object>) jobUpdate.get("modelDebugConfig");
        assertEquals(85.0, configUpdate.get("boundsPercentile"));
        assertEquals("bar", configUpdate.get("terms"));
    }

    @Test
    public void testSetModelDebugConfig_GivenNull() throws UnknownJobException
    {
        JobManager jobManager = createJobManager();

        jobManager.setModelDebugConfig("foo", null);

        verify(jobProvider).updateJob(eq("foo"), jobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = jobUpdateCaptor.getValue();
        assertNull(jobUpdate.get("modelDebugConfig"));
    }

    @Test
    public void testSetDesciption() throws UnknownJobException
    {
        JobManager jobManager = createJobManager();

        jobManager.setDescription("foo", "foo job");

        verify(jobProvider).updateJob(eq("foo"), jobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = jobUpdateCaptor.getValue();
        assertEquals("foo job", jobUpdate.get(JobDetails.DESCRIPTION));
    }

    @Test
    public void testSetBackgroundPersistInterval() throws UnknownJobException
    {
        JobManager jobManager = createJobManager();

        jobManager.setBackgroundPersistInterval("foo", 36000L);

        verify(jobProvider).updateJob(eq("foo"), jobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = jobUpdateCaptor.getValue();
        assertEquals(36000L, jobUpdate.get(JobDetails.BACKGROUND_PERSIST_INTERVAL));
    }

    @Test
    public void testSetRenormalizationWindowDays() throws UnknownJobException
    {
        JobManager jobManager = createJobManager();

        jobManager.setRenormalizationWindowDays("foo", 7L);

        verify(jobProvider).updateJob(eq("foo"), jobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = jobUpdateCaptor.getValue();
        assertEquals(new Long(7), jobUpdate.get(JobDetails.RENORMALIZATION_WINDOW_DAYS));
    }

    @Test
    public void testSetModelSnapshotRetentionDays() throws UnknownJobException
    {
        JobManager jobManager = createJobManager();

        jobManager.setModelSnapshotRetentionDays("foo", 20L);

        verify(jobProvider).updateJob(eq("foo"), jobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = jobUpdateCaptor.getValue();
        assertEquals(new Long(20), jobUpdate.get(JobDetails.MODEL_SNAPSHOT_RETENTION_DAYS));
    }

    @Test
    public void testSetResultsRetentionDays() throws UnknownJobException
    {
        JobManager jobManager = createJobManager();

        jobManager.setResultsRetentionDays("foo", 90L);

        verify(jobProvider).updateJob(eq("foo"), jobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = jobUpdateCaptor.getValue();
        assertEquals(new Long(90), jobUpdate.get(JobDetails.RESULTS_RETENTION_DAYS));
    }

    @Test
    public void testGetJobOrThrowIfUnknown_GivenUnknownJob() throws UnknownJobException
    {
        JobManager jobManager = createJobManager();
        when(jobProvider.getJobDetails("foo")).thenReturn(Optional.empty());

        expectedException.expect(UnknownJobException.class);

        jobManager.getJobOrThrowIfUnknown("foo");
    }

    @Test
    public void testGetJobOrThrowIfUnknown_GivenKnownJob() throws UnknownJobException
    {
        JobManager jobManager = createJobManager();
        JobDetails job = new JobDetails();
        when(jobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));

        assertEquals(job, jobManager.getJobOrThrowIfUnknown("foo"));
    }

    @Test
    public void testUpdateCustomSettings() throws UnknownJobException
    {
        JobManager jobManager = createJobManager();
        Map<String, Object> customSettings = new HashMap<>();
        customSettings.put("answer", 42);

        jobManager.updateCustomSettings("foo", customSettings);

        verify(jobProvider).updateJob(eq("foo"), jobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = jobUpdateCaptor.getValue();
        assertEquals(customSettings, jobUpdate.get(JobDetails.CUSTOM_SETTINGS));
    }

    @Test
    public void testSetAnalysisLimits() throws UnknownJobException
    {
        JobManager jobManager = createJobManager();

        AnalysisLimits newLimits = new AnalysisLimits();
        newLimits.setModelMemoryLimit(1L);
        newLimits.setCategorizationExamplesLimit(2L);

        jobManager.setAnalysisLimits("foo", newLimits);

        verify(jobProvider).updateJob(eq("foo"), jobUpdateCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> capturedLimits = (Map<String, Object>) jobUpdateCaptor.getValue()
                .get(JobDetails.ANALYSIS_LIMITS);
        assertNotNull(capturedLimits);
        assertEquals(1L, capturedLimits.get(AnalysisLimits.MODEL_MEMORY_LIMIT));
        assertEquals(2L, capturedLimits.get(AnalysisLimits.CATEGORIZATION_EXAMPLES_LIMIT));
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

    private JobManager createJobManager()
    {
        return new JobManager(jobProvider,
                new LocalActionGuardian<Action>(Action.CLOSED),
                new LocalActionGuardian<ScheduledAction>(ScheduledAction.STOPPED));
    }

    private static JobConfiguration createScheduledJobConfig()
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(3600L);
        analysisConfig.setDetectors(Arrays.asList(new Detector()));

        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        schedulerConfig.setBaseUrl("http://localhost");

        JobConfiguration jobConfig = new JobConfiguration();
        jobConfig.setId("foo");
        jobConfig.setAnalysisConfig(analysisConfig);
        jobConfig.setSchedulerConfig(schedulerConfig);
        return jobConfig;
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
