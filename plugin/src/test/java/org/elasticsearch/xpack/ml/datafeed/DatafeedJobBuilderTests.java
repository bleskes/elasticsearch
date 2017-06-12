/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.datafeed;

import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.Client;
import org.elasticsearch.mock.orig.Mockito;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.action.util.QueryPage;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.persistence.JobProvider;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCounts;
import org.elasticsearch.xpack.ml.job.results.Bucket;
import org.elasticsearch.xpack.ml.notifications.Auditor;
import org.junit.Before;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatafeedJobBuilderTests extends ESTestCase {

    private Client client;
    private Auditor auditor;
    private JobProvider jobProvider;
    private Consumer<Exception> taskHandler;

    private DatafeedJobBuilder datafeedJobBuilder;

    @Before
    public void init() {
        client = mock(Client.class);
        auditor = mock(Auditor.class);
        jobProvider = mock(JobProvider.class);
        taskHandler = mock(Consumer.class);
        datafeedJobBuilder = new DatafeedJobBuilder(client, jobProvider, auditor, System::currentTimeMillis);

        Mockito.doAnswer(invocationOnMock -> {
            String jobId = (String) invocationOnMock.getArguments()[0];
            @SuppressWarnings("unchecked")
            Consumer<DataCounts> handler = (Consumer<DataCounts>) invocationOnMock.getArguments()[1];
            handler.accept(new DataCounts(jobId));
            return null;
        }).when(jobProvider).dataCounts(any(), any(), any());

        doAnswer(invocationOnMock -> {
            @SuppressWarnings("rawtypes")
            Consumer consumer = (Consumer) invocationOnMock.getArguments()[3];
            consumer.accept(new ResourceNotFoundException("dummy"));
            return null;
        }).when(jobProvider).bucketsViaInternalClient(any(), any(), any(), any());
    }

    public void testBuild_GivenScrollDatafeedAndNewJob() throws Exception {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeField("time");
        Job.Builder jobBuilder = DatafeedManagerTests.createDatafeedJob();
        jobBuilder.setDataDescription(dataDescription);
        DatafeedConfig datafeed = DatafeedManagerTests.createDatafeedConfig("datafeed1", "foo").build();

        AtomicBoolean wasHandlerCalled = new AtomicBoolean(false);
        ActionListener<DatafeedJob> datafeedJobHandler = ActionListener.wrap(
                datafeedJob -> {
                    assertThat(datafeedJob.isRunning(), is(true));
                    assertThat(datafeedJob.isIsolated(), is(false));
                    assertThat(datafeedJob.lastEndTimeMs(), is(nullValue()));
                    wasHandlerCalled.compareAndSet(false, true);
                }, e -> fail()
        );

        datafeedJobBuilder.build(jobBuilder.build(new Date()), datafeed, datafeedJobHandler);

        assertBusy(() -> wasHandlerCalled.get());
    }

    public void testBuild_GivenScrollDatafeedAndOldJobWithLatestRecordTimestampAfterLatestBucket() throws Exception {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeField("time");
        Job.Builder jobBuilder = DatafeedManagerTests.createDatafeedJob();
        jobBuilder.setDataDescription(dataDescription);
        DatafeedConfig datafeed = DatafeedManagerTests.createDatafeedConfig("datafeed1", "foo").build();

        givenLatestTimes(7_200_000L, 3_600_000L);

        AtomicBoolean wasHandlerCalled = new AtomicBoolean(false);
        ActionListener<DatafeedJob> datafeedJobHandler = ActionListener.wrap(
                datafeedJob -> {
                    assertThat(datafeedJob.isRunning(), is(true));
                    assertThat(datafeedJob.isIsolated(), is(false));
                    assertThat(datafeedJob.lastEndTimeMs(), equalTo(7_200_000L));
                    wasHandlerCalled.compareAndSet(false, true);
                }, e -> fail()
        );

        datafeedJobBuilder.build(jobBuilder.build(new Date()), datafeed, datafeedJobHandler);

        assertBusy(() -> wasHandlerCalled.get());
    }

    public void testBuild_GivenScrollDatafeedAndOldJobWithLatestBucketAfterLatestRecordTimestamp() throws Exception {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeField("time");
        Job.Builder jobBuilder = DatafeedManagerTests.createDatafeedJob();
        jobBuilder.setDataDescription(dataDescription);
        DatafeedConfig datafeed = DatafeedManagerTests.createDatafeedConfig("datafeed1", "foo").build();

        givenLatestTimes(3_800_000L, 3_600_000L);

        AtomicBoolean wasHandlerCalled = new AtomicBoolean(false);
        ActionListener<DatafeedJob> datafeedJobHandler = ActionListener.wrap(
                datafeedJob -> {
                    assertThat(datafeedJob.isRunning(), is(true));
                    assertThat(datafeedJob.isIsolated(), is(false));
                    assertThat(datafeedJob.lastEndTimeMs(), equalTo(7_199_999L));
                    wasHandlerCalled.compareAndSet(false, true);
                }, e -> fail()
        );

        datafeedJobBuilder.build(jobBuilder.build(new Date()), datafeed, datafeedJobHandler);

        assertBusy(() -> wasHandlerCalled.get());
    }

    public void testBuild_GivenBucketsRequestFails() {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeField("time");
        Job.Builder jobBuilder = DatafeedManagerTests.createDatafeedJob();
        jobBuilder.setDataDescription(dataDescription);
        DatafeedConfig datafeed = DatafeedManagerTests.createDatafeedConfig("datafeed1", "foo").build();

        Exception error = new RuntimeException("error");
        doAnswer(invocationOnMock -> {
            @SuppressWarnings("rawtypes")
            Consumer consumer = (Consumer) invocationOnMock.getArguments()[3];
            consumer.accept(error);
            return null;
        }).when(jobProvider).bucketsViaInternalClient(any(), any(), any(), any());

        datafeedJobBuilder.build(jobBuilder.build(new Date()), datafeed, ActionListener.wrap(datafeedJob -> fail(), taskHandler));

        verify(taskHandler).accept(error);
    }

    private void givenLatestTimes(long latestRecordTimestamp, long latestBucketTimestamp) {
        Mockito.doAnswer(invocationOnMock -> {
            String jobId = (String) invocationOnMock.getArguments()[0];
            @SuppressWarnings("unchecked")
            Consumer<DataCounts> handler = (Consumer<DataCounts>) invocationOnMock.getArguments()[1];
            DataCounts dataCounts = new DataCounts(jobId);
            dataCounts.setLatestRecordTimeStamp(new Date(latestRecordTimestamp));
            handler.accept(dataCounts);
            return null;
        }).when(jobProvider).dataCounts(any(), any(), any());

        doAnswer(invocationOnMock -> {
            @SuppressWarnings("rawtypes")
            Consumer consumer = (Consumer) invocationOnMock.getArguments()[2];
            Bucket bucket = mock(Bucket.class);
            when(bucket.getTimestamp()).thenReturn(new Date(latestBucketTimestamp));
            QueryPage<Bucket> bucketQueryPage = new QueryPage(Collections.singletonList(bucket), 1, Bucket.RESULTS_FIELD);
            consumer.accept(bucketQueryPage);
            return null;
        }).when(jobProvider).bucketsViaInternalClient(any(), any(), any(), any());
    }
}