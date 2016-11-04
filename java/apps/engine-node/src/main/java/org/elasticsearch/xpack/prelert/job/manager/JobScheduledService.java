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
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.action.UpdateJobSchedulerStatusAction;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.JobSchedulerStatus;
import org.elasticsearch.xpack.prelert.job.SchedulerState;
import org.elasticsearch.xpack.prelert.job.config.DefaultFrequency;
import org.elasticsearch.xpack.prelert.job.data.DataProcessor;
import org.elasticsearch.xpack.prelert.job.extraction.DataExtractorFactory;
import org.elasticsearch.xpack.prelert.job.logging.JobLoggerFactory;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.metadata.Allocation;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.scheduler.JobScheduler;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JobScheduledService {

    private static final Logger LOGGER = Loggers.getLogger(JobScheduledService.class);

    private final Client client;
    private final Map<String, JobScheduler> jobToScheduler;
    private final JobManager jobManager;
    private final JobProvider jobProvider;
    private final DataProcessor dataProcessor;
    private final DataExtractorFactory dataExtractorFactory;
    private final JobLoggerFactory jobLoggerFactory;

    public JobScheduledService(Client client, JobProvider jobProvider, JobManager jobManager, DataProcessor dataProcessor,
                               DataExtractorFactory dataExtractorFactory, JobLoggerFactory jobLoggerFactory) {
        jobToScheduler = new HashMap<>();
        this.client = Objects.requireNonNull(client);
        this.jobManager = Objects.requireNonNull(jobManager);
        this.jobProvider = Objects.requireNonNull(jobProvider);
        this.dataProcessor = Objects.requireNonNull(dataProcessor);
        this.dataExtractorFactory = Objects.requireNonNull(dataExtractorFactory);
        this.jobLoggerFactory = Objects.requireNonNull(jobLoggerFactory);
    }

    public void start(Job job, Allocation allocation) {
        if (!jobToScheduler.containsKey(allocation.getJobId())) {
            SchedulerState schedulerState = allocation.getSchedulerState();
            if (schedulerState != null && schedulerState.getStatus() == JobSchedulerStatus.STARTED) {
                LOGGER.info("Starting scheduler for job: " + allocation.getJobId());
                createJobScheduler(job);
                jobToScheduler.get(job.getId()).start(job, allocation);
            }
        }
    }

    private JobScheduler createJobScheduler(Job job) {
        Duration bucketSpan = Duration.ofSeconds(job.getAnalysisConfig().getBucketSpan());
        Duration frequency = getFrequencyOrDefault(job);
        Duration queryDelay = Duration.ofSeconds(job.getSchedulerConfig().getQueryDelay());
        JobScheduler jobScheduler = new JobScheduler(job.getId(), bucketSpan, frequency, queryDelay,
                dataExtractorFactory.newExtractor(job), dataProcessor, jobProvider, jobLoggerFactory,
                () -> jobManager.getJobAllocation(job.getJobId()).getSchedulerState().getStatus(),
                new SchedulerStatusListener(job.getId()));
        jobToScheduler.put(job.getId(), jobScheduler);
        return jobScheduler;
    }

    private static Duration getFrequencyOrDefault(Job job) {
        Long frequency = job.getSchedulerConfig().getFrequency();
        Long bucketSpan = job.getAnalysisConfig().getBucketSpan();
        return frequency == null ? DefaultFrequency.ofBucketSpan(bucketSpan) : Duration.ofSeconds(frequency);
    }

    public void stop(Allocation allocation) {
        if (jobToScheduler.containsKey(allocation.getJobId())) {
            SchedulerState schedulerState = allocation.getSchedulerState();
            if (schedulerState != null && schedulerState.getStatus() == JobSchedulerStatus.STOPPING) {
                LOGGER.info("Stopping scheduler for job: " + allocation.getJobId());
                jobToScheduler.get(allocation.getJobId()).stopManual();
            }
        }
    }

    private class SchedulerStatusListener implements JobScheduler.Listener {

        private final String jobId;

        private SchedulerStatusListener(String jobId) {
            this.jobId = Objects.requireNonNull(jobId);
        }

        @Override
        public void statusChanged(JobSchedulerStatus newStatus) {
            if (newStatus == JobSchedulerStatus.STOPPED) {
                try {
                    dataProcessor.closeJob(jobId);
                    jobToScheduler.remove(jobId);
                } catch (ElasticsearchException e) {
                    LOGGER.error(Messages.getMessage(Messages.JOB_SCHEDULER_FAILED_TO_STOP), e);
                }
            }
            UpdateJobSchedulerStatusAction.Request request = new UpdateJobSchedulerStatusAction.Request(jobId, newStatus);
            client.execute(UpdateJobSchedulerStatusAction.INSTANCE, request, new ActionListener<UpdateJobSchedulerStatusAction.Response>() {
                @Override
                public void onResponse(UpdateJobSchedulerStatusAction.Response response) {
                    // Do nothing
                }

                @Override
                public void onFailure(Exception e) {
                    // Do nothing
                }
            });
        }
    }
}
