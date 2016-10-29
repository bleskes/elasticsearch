package org.elasticsearch.xpack.prelert.job.process.autodetect;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
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

    private static final Logger LOGGER = Loggers.getLogger(AutodetectCommunicatorFactory.class);

    private final AutodetectProcessFactory autodetectProcessFactory;
    private final JobResultsPeristerFactory persisterFactory;
    private final JobDataCountsPersisterFactory dataCountsPersisterFactory;
    private final UsagePersisterFactory usagePersisterFactory;
    private final JobLoggerFactory jobLoggerFactory;
    private final Environment env;

    public AutodetectCommunicatorFactory(Environment env, AutodetectProcessFactory autodetectProcessFactory,
            JobResultsPeristerFactory persisterFactory, JobDataCountsPersisterFactory dataCountsPersisterFactory,
            UsagePersisterFactory usagePersisterFactory, JobLoggerFactory loggerFactory) {
        this.env = env;
        this.autodetectProcessFactory = Objects.requireNonNull(autodetectProcessFactory);
        this.persisterFactory = Objects.requireNonNull(persisterFactory);
        this.dataCountsPersisterFactory = Objects.requireNonNull(dataCountsPersisterFactory);
        this.usagePersisterFactory = Objects.requireNonNull(usagePersisterFactory);
        this.jobLoggerFactory = Objects.requireNonNull(loggerFactory);
    }

    public AutodetectCommunicator create(JobDetails job, boolean ignoreDowntime) {

        Logger jobLogger = jobLoggerFactory.newLogger(job.getJobId());
        UsageReporter usageReporter = new UsageReporter(env, job.getJobId(), usagePersisterFactory.getInstance(jobLogger), jobLogger);

        StatusReporter statusReporter = new StatusReporter(env, job.getJobId(), job.getCounts(), usageReporter,
                dataCountsPersisterFactory.getInstance(jobLogger),
                jobLogger, job.getAnalysisConfig().getBucketSpanOrDefault());


        return new AutodetectCommunicator(job, autodetectProcessFactory.createAutodetectProcess(job, ignoreDowntime),
                jobLogger, persisterFactory.jobResultsPersister(job.getJobId()), statusReporter);

    }
}
