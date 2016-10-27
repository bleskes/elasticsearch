package org.elasticsearch.xpack.prelert.job.manager;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.JobSchedulerStatus;
import org.elasticsearch.xpack.prelert.job.SchedulerState;
import org.elasticsearch.xpack.prelert.job.config.DefaultFrequency;
import org.elasticsearch.xpack.prelert.job.data.DataProcessor;
import org.elasticsearch.xpack.prelert.job.extraction.DataExtractorFactory;
import org.elasticsearch.xpack.prelert.job.logging.JobLoggerFactory;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.scheduler.JobScheduler;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JobScheduledService {

    private static final Logger LOGGER = Loggers.getLogger(JobScheduledService.class);

    private final Map<String, JobScheduler> jobToScheduler;
    private final JobManager jobManager;
    private final JobProvider jobProvider;
    private final DataProcessor dataProcessor;
    private final DataExtractorFactory dataExtractorFactory;
    private final JobLoggerFactory jobLoggerFactory;

    public JobScheduledService(JobProvider jobProvider, JobManager jobManager, DataProcessor dataProcessor,
                               DataExtractorFactory dataExtractorFactory, JobLoggerFactory jobLoggerFactory) {
        jobToScheduler = new HashMap<>();
        this.jobManager = Objects.requireNonNull(jobManager);
        this.jobProvider = Objects.requireNonNull(jobProvider);
        this.dataProcessor = Objects.requireNonNull(dataProcessor);
        this.dataExtractorFactory = Objects.requireNonNull(dataExtractorFactory);
        this.jobLoggerFactory = Objects.requireNonNull(jobLoggerFactory);
    }

    public void start(JobDetails job) {
        if (!jobToScheduler.containsKey(job.getId())) {
            SchedulerState schedulerState = job.getSchedulerState();
            if (schedulerState != null && schedulerState.getStatus() == JobSchedulerStatus.STARTED) {
                LOGGER.info("Starting scheduler for job: " + job.getId());
                createJobScheduler(job);
                jobToScheduler.get(job.getId()).start(job);
            }
        }
    }

    private JobScheduler createJobScheduler(JobDetails job) {
        Duration bucketSpan = Duration.ofSeconds(job.getAnalysisConfig().getBucketSpan());
        Duration frequency = getFrequencyOrDefault(job);
        Duration queryDelay = Duration.ofSeconds(job.getSchedulerConfig().getQueryDelay());
        JobScheduler jobScheduler = new JobScheduler(job.getId(), bucketSpan, frequency, queryDelay,
                dataExtractorFactory.newExtractor(job), dataProcessor, jobProvider, jobLoggerFactory,
                () -> jobManager.getJobOrThrowIfUnknown(job.getId()).getSchedulerState().getStatus(),
                new SchedulerStatusListener(job.getId()));
        jobToScheduler.put(job.getId(), jobScheduler);
        return jobScheduler;
    }

    private static Duration getFrequencyOrDefault(JobDetails job) {
        Long frequency = job.getSchedulerConfig().getFrequency();
        Long bucketSpan = job.getAnalysisConfig().getBucketSpan();
        return frequency == null ? DefaultFrequency.ofBucketSpan(bucketSpan) : Duration.ofSeconds(frequency);
    }

    public void stop(String jobId) {
        if (jobToScheduler.containsKey(jobId)) {
            JobDetails job = jobManager.getJobOrThrowIfUnknown(jobId);
            SchedulerState schedulerState = job.getSchedulerState();
            if (schedulerState != null && schedulerState.getStatus() == JobSchedulerStatus.STOPPING) {
                LOGGER.info("Stopping scheduler for job: " + jobId);
                jobToScheduler.get(jobId).stopManual();
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
            jobManager.updateSchedulerStatus(jobId, newStatus);
        }
    }
}
