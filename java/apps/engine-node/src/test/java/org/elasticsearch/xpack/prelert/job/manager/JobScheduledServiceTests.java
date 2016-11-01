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

import org.apache.logging.log4j.Logger;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.JobSchedulerStatus;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.SchedulerState;
import org.elasticsearch.xpack.prelert.job.audit.Auditor;
import org.elasticsearch.xpack.prelert.job.data.DataProcessor;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.extraction.DataExtractor;
import org.elasticsearch.xpack.prelert.job.extraction.DataExtractorFactory;
import org.elasticsearch.xpack.prelert.job.logging.JobLoggerFactory;
import org.elasticsearch.xpack.prelert.job.persistence.BucketsQueryBuilder;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.junit.Before;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.elasticsearch.mock.orig.Mockito.times;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobScheduledServiceTests extends ESTestCase {

    private JobProvider jobProvider;
    private JobManager jobManager;
    private DataProcessor dataProcessor;
    private DataExtractorFactory dataExtractorFactory;
    private JobLoggerFactory jobLoggerFactory;
    private Auditor auditor;

    private JobScheduledService jobScheduledService;

    @Before
    public void setUpTests() {
        jobProvider = mock(JobProvider.class);
        jobManager = mock(JobManager.class);
        dataProcessor = mock(DataProcessor.class);
        dataExtractorFactory = mock(DataExtractorFactory.class);
        jobLoggerFactory = mock(JobLoggerFactory.class);
        auditor = mock(Auditor.class);

        jobScheduledService = new JobScheduledService(jobProvider, jobManager, dataProcessor, dataExtractorFactory, jobLoggerFactory);

        when(jobProvider.audit(anyString())).thenReturn(auditor);
        when(jobProvider.buckets(anyString(), any(BucketsQueryBuilder.BucketsQuery.class))).thenReturn(
                new QueryPage<>(Collections.emptyList(), 0));
    }

    public void testStart_GivenNewlyCreatedJob() throws JobException, IOException {
        JobDetails job = createScheduledJob();
        job.setSchedulerState(new SchedulerState(JobSchedulerStatus.STARTED, 0, null));
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(0));
        job.setCounts(dataCounts);
        when(jobManager.getJobOrThrowIfUnknown("foo")).thenReturn(job);

        Logger jobLogger = mock(Logger.class);
        when(jobLoggerFactory.newLogger("foo")).thenReturn(jobLogger);
        DataExtractor dataExtractor = mock(DataExtractor.class);
        when(dataExtractorFactory.newExtractor(job)).thenReturn(dataExtractor);

        jobScheduledService.start(job);

        job.setSchedulerState(new SchedulerState(JobSchedulerStatus.STOPPING, 0, null));
        jobScheduledService.stop("foo");

        verify(dataExtractor).newSearch(anyLong(), anyLong(), eq(jobLogger));
        verify(dataProcessor).closeJob("foo");
    }

    public void testStop_GivenNonScheduledJob() {
        jobScheduledService.stop("foo");
    }

    public void testStop_GivenStartedScheduledJob() throws JobException, IOException {
        JobDetails job = createScheduledJob();
        job.setSchedulerState(new SchedulerState(JobSchedulerStatus.STARTED, 0, null));
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(0));
        job.setCounts(dataCounts);
        when(jobManager.getJobOrThrowIfUnknown("foo")).thenReturn(job);

        Logger jobLogger = mock(Logger.class);
        when(jobLoggerFactory.newLogger("foo")).thenReturn(jobLogger);
        DataExtractor dataExtractor = mock(DataExtractor.class);
        when(dataExtractorFactory.newExtractor(job)).thenReturn(dataExtractor);

        jobScheduledService.start(job);

        jobScheduledService.stop("foo");

        // Properly stop it to avoid leaking threads in the test
        job.setSchedulerState(new SchedulerState(JobSchedulerStatus.STOPPING, 0, null));
        jobScheduledService.stop("foo");

        verify(dataExtractor).newSearch(anyLong(), anyLong(), eq(jobLogger));

        // We stopped twice but the first time should have been ignored. We can assert that indirectly
        // by verifying that the job was closed only once.
        verify(dataProcessor, times(1)).closeJob("foo");
    }

    private static JobDetails createScheduledJob()
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(3600L);
        analysisConfig.setDetectors(Arrays.asList(new Detector("metric")));

        SchedulerConfig.Builder schedulerConfig = new SchedulerConfig.Builder(SchedulerConfig.DataSource.ELASTICSEARCH);
        schedulerConfig.setBaseUrl("http://localhost");
        schedulerConfig.setIndexes(Arrays.asList("myIndex"));
        schedulerConfig.setTypes(Arrays.asList("myType"));

        JobConfiguration jobConfig = new JobConfiguration("foo");
        jobConfig.setAnalysisConfig(analysisConfig);
        jobConfig.setSchedulerConfig(schedulerConfig);
        return jobConfig.build();
    }
}