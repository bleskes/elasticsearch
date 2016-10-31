package org.elasticsearch.xpack.prelert.job.process.autodetect;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.logging.JobLoggerFactory;
import org.elasticsearch.xpack.prelert.job.persistence.JobDataCountsPersisterFactory;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPeristerFactory;
import org.elasticsearch.xpack.prelert.job.persistence.UsagePersisterFactory;
import org.elasticsearch.xpack.prelert.job.status.StatusReporter;
import org.elasticsearch.xpack.prelert.job.usage.UsageReporter;

import java.util.Objects;

public class AutodetectCommunicatorFactory {

    private final AutodetectProcessFactory autodetectProcessFactory;
    private final JobResultsPeristerFactory persisterFactory;
    private final JobDataCountsPersisterFactory dataCountsPersisterFactory;
    private final UsagePersisterFactory usagePersisterFactory;
    private final JobLoggerFactory jobLoggerFactory;
    private final Environment env;
    private final Settings settings;

    public AutodetectCommunicatorFactory(Environment env, Settings settings, AutodetectProcessFactory autodetectProcessFactory,
            JobResultsPeristerFactory persisterFactory, JobDataCountsPersisterFactory dataCountsPersisterFactory,
            UsagePersisterFactory usagePersisterFactory, JobLoggerFactory loggerFactory) {
        this.env = env;
        this.settings = settings;
        this.autodetectProcessFactory = Objects.requireNonNull(autodetectProcessFactory);
        this.persisterFactory = Objects.requireNonNull(persisterFactory);
        this.dataCountsPersisterFactory = Objects.requireNonNull(dataCountsPersisterFactory);
        this.usagePersisterFactory = Objects.requireNonNull(usagePersisterFactory);
        this.jobLoggerFactory = Objects.requireNonNull(loggerFactory);
    }

    public AutodetectCommunicator create(JobDetails job, boolean ignoreDowntime) {

        Logger jobLogger = jobLoggerFactory.newLogger(job.getJobId());
        UsageReporter usageReporter = new UsageReporter(settings, job.getJobId(), usagePersisterFactory.getInstance(jobLogger), jobLogger);

        StatusReporter statusReporter = new StatusReporter(env, settings, job.getJobId(), job.getCounts(), usageReporter,
                dataCountsPersisterFactory.getInstance(jobLogger),
                jobLogger, job.getAnalysisConfig().getBucketSpanOrDefault());


        return new AutodetectCommunicator(job, autodetectProcessFactory.createAutodetectProcess(job, ignoreDowntime),
                jobLogger, persisterFactory.jobResultsPersister(job.getJobId()), statusReporter);

    }
}
