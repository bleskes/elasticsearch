package org.elasticsearch.xpack.prelert.job.manager;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.alert.AlertObserver;
import org.elasticsearch.xpack.prelert.job.alert.AlertTrigger;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.process.autodetect.AutodetectCommunicator;
import org.elasticsearch.xpack.prelert.job.process.autodetect.AutodetectCommunicatorFactory;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.TimeRange;
import org.elasticsearch.xpack.prelert.job.process.exceptions.ClosedJobException;
import org.elasticsearch.xpack.prelert.job.results.Bucket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.elasticsearch.mock.orig.Mockito.doThrow;
import static org.elasticsearch.mock.orig.Mockito.mock;
import static org.elasticsearch.mock.orig.Mockito.verify;
import static org.elasticsearch.mock.orig.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;

/**
 * Calling the {@link AutodetectProcessManager#processData(String, InputStream, DataLoadParams)}
 * method causes an AutodetectCommunicator to be created on demand. Most of these tests have to
 * do that before they can assert other things
 */
public class AutodetectProcessManagerTest extends ESTestCase {

    public void testCreateProcessBySubmittingData()  {

        AutodetectCommunicator communicator = mock(AutodetectCommunicator.class);
        AutodetectProcessManager manager = createManager(communicator);
        assertEquals(0, manager.numberOfRunningJobs());

        DataLoadParams params = new DataLoadParams(TimeRange.builder().build());
        manager.processData("foo", createInputStream(""), params);
        assertEquals(1, manager.numberOfRunningJobs());
    }

    public void testProcessDataThrowsElasticsearchStatusException_onIoException() throws JobException, IOException {
        AutodetectCommunicator communicator = mock(AutodetectCommunicator.class);
        AutodetectProcessManager manager = createManager(communicator);

        InputStream inputStream = createInputStream("");
        doThrow(new IOException("blah")).when(communicator).writeToJob(inputStream);

        ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> manager.processData("foo", inputStream, mock(DataLoadParams.class)));
    }

    public void testCloseJob() {

        AutodetectCommunicator communicator = mock(AutodetectCommunicator.class);
        AutodetectCommunicatorFactory factory = mockCommunicatorFactory(communicator);
        JobManager jobManager = mockJobManager();
        when(jobManager.getJobOrThrowIfUnknown("foo")).thenReturn(createJobDetails("foo"));
        AutodetectProcessManager manager = new AutodetectProcessManager(factory, jobManager);
        assertEquals(0, manager.numberOfRunningJobs());

        manager.processData("foo", createInputStream(""), mock(DataLoadParams.class));

        // job is created
        assertEquals(1, manager.numberOfRunningJobs());
        manager.closeJob("foo");
        assertEquals(0, manager.numberOfRunningJobs());

//        verify(jobManager).setJobFinishedTimeAndStatus(eq("foo"), any(), eq(JobStatus.CLOSED));
    }

    public void testBucketResetMessageIsSent() throws IOException, JobException {

        AutodetectCommunicator communicator = mock(AutodetectCommunicator.class);
        AutodetectProcessManager manager = createManager(communicator);

        DataLoadParams params = new DataLoadParams(TimeRange.builder().startTime("1000").endTime("2000").build(), true);
        InputStream inputStream = createInputStream("");
        manager.processData("foo", inputStream, params);

        verify(communicator).writeResetBucketsControlMessage(params);
        verify(communicator).writeToJob(inputStream);
    }

    public void testFlush() throws IOException {

        AutodetectCommunicator communicator = mock(AutodetectCommunicator.class);
        AutodetectCommunicatorFactory factory = mockCommunicatorFactory(communicator);
        JobManager jobManager = mockJobManager();
        when(jobManager.getJobOrThrowIfUnknown("foo")).thenReturn(createJobDetails("foo"));

        AutodetectProcessManager manager = new AutodetectProcessManager(factory, jobManager);

        InputStream inputStream = createInputStream("");
        manager.processData("foo", inputStream, mock(DataLoadParams.class));

        InterimResultsParams params = InterimResultsParams.builder().build();
        manager.flushJob("foo", params);

        verify(communicator).flushJob(params);
    }

    public void testFlushThrows() throws JobException, IOException {

        AutodetectCommunicator communicator = mock(AutodetectCommunicator.class);
        AutodetectProcessManager manager = createManagerAndCallProcessData(communicator, "foo");

        InterimResultsParams params = InterimResultsParams.builder().build();
        doThrow(new IOException("blah")).when(communicator).flushJob(params);

        ElasticsearchStatusException e = ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> manager.flushJob("foo", params));
        assertEquals(ErrorCodes.NATIVE_PROCESS_WRITE_ERROR.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testWriteUpdateConfigMessage() throws IOException {

        AutodetectCommunicator communicator = mock(AutodetectCommunicator.class);
        AutodetectProcessManager manager = createManagerAndCallProcessData(communicator, "foo");
        manager.writeUpdateConfigMessage("foo", "go faster");
        verify(communicator).writeUpdateConfigMessage("go faster");
    }

    public void testJobHasActiveAutodetectProcess()throws JobException, IOException {

        AutodetectCommunicator communicator = mock(AutodetectCommunicator.class);
        AutodetectProcessManager manager = createManager(communicator);
        assertFalse(manager.jobHasActiveAutodetectProcess("foo"));

        manager.processData("foo", createInputStream(""), mock(DataLoadParams.class));

        assertTrue(manager.jobHasActiveAutodetectProcess("foo"));
        assertFalse(manager.jobHasActiveAutodetectProcess("bar"));
    }

    public void testAddAlertObserver_OnClosedJob() {

        AutodetectCommunicator communicator = mock(AutodetectCommunicator.class);
        AutodetectProcessManager manager = createManager(communicator);

        ESTestCase.expectThrows(ClosedJobException.class, () -> manager.addAlertObserver("foo", mock(AlertObserver.class)));
    }

    public void testAddRemoveAlertObserver() throws JobException {

        AutodetectCommunicator communicator = mock(AutodetectCommunicator.class);
        AutodetectProcessManager manager = createManagerAndCallProcessData(communicator, "foo");

        AlertObserver observer = new AlertObserver(new AlertTrigger[] {}, "foo") {
            @Override
            public void fire(Bucket bucket, AlertTrigger trigger) {

            }
        };
        manager.addAlertObserver("foo", observer);
        verify(communicator).addAlertObserver(observer);

        manager.removeAlertObserver("foo", observer);
        verify(communicator).removeAlertObserver(observer);
    }


    private AutodetectProcessManager createManager(AutodetectCommunicator communicator) {
        AutodetectCommunicatorFactory factory = mockCommunicatorFactory(communicator);
        JobManager jobManager = mockJobManager();
        when(jobManager.getJobOrThrowIfUnknown("foo")).thenReturn(createJobDetails("foo"));

        return new AutodetectProcessManager(factory, jobManager);
    }

    private AutodetectProcessManager createManagerAndCallProcessData(AutodetectCommunicator communicator, String jobId) {
        AutodetectProcessManager manager = createManager(communicator);
        manager.processData(jobId, createInputStream(""), mock(DataLoadParams.class));
        return manager;
    }

    private AutodetectCommunicatorFactory mockCommunicatorFactory(AutodetectCommunicator instanceReturnedByFactory) {

        AutodetectCommunicatorFactory factory = mock(AutodetectCommunicatorFactory.class);

        when(factory.create(any(), anyBoolean())).thenReturn(instanceReturnedByFactory);

        return factory;
    }

    private JobManager mockJobManager() {
        return mock(JobManager.class);
    }

    private JobDetails createJobDetails(String jobId) {

        DataDescription dd = new DataDescription();
        dd.setFormat(DataDescription.DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');

        Detector d = new Detector("metric", "value");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Collections.singletonList(d));

        JobDetails jobDetails = new JobConfiguration(jobId).build();
        jobDetails.setDataDescription(dd);
        jobDetails.setAnalysisConfig(ac);

        return jobDetails;
    }

    private static InputStream createInputStream(String input)
    {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }
}
