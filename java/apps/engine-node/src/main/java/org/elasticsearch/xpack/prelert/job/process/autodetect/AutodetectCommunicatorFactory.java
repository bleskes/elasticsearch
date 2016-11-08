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
package org.elasticsearch.xpack.prelert.job.process.autodetect;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.prelert.job.Job;
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
    private final Environment env;
    private final Settings settings;
    private ParseFieldMatcherSupplier parseFieldMatcherSupplier;

    public AutodetectCommunicatorFactory(Environment env, Settings settings, AutodetectProcessFactory autodetectProcessFactory,
            JobResultsPeristerFactory persisterFactory, JobDataCountsPersisterFactory dataCountsPersisterFactory,
            UsagePersisterFactory usagePersisterFactory, ParseFieldMatcherSupplier parseFieldMatcherSupplier) {
        this.env = env;
        this.settings = settings;
        this.parseFieldMatcherSupplier = parseFieldMatcherSupplier;
        this.autodetectProcessFactory = Objects.requireNonNull(autodetectProcessFactory);
        this.persisterFactory = Objects.requireNonNull(persisterFactory);
        this.dataCountsPersisterFactory = Objects.requireNonNull(dataCountsPersisterFactory);
        this.usagePersisterFactory = Objects.requireNonNull(usagePersisterFactory);
    }

    public AutodetectCommunicator create(Job job, boolean ignoreDowntime) {
        Logger jobLogger = Loggers.getLogger(job.getJobId());
        UsageReporter usageReporter = new UsageReporter(settings, job.getJobId(), usagePersisterFactory.getInstance(jobLogger), jobLogger);

        StatusReporter statusReporter = new StatusReporter(env, settings, job.getJobId(), job.getCounts(), usageReporter,
                dataCountsPersisterFactory.getInstance(jobLogger),
                jobLogger, job.getAnalysisConfig().getBucketSpanOrDefault());


        return new AutodetectCommunicator(job, autodetectProcessFactory.createAutodetectProcess(job, ignoreDowntime),
                jobLogger, persisterFactory.jobResultsPersister(job.getJobId()), statusReporter, parseFieldMatcherSupplier);

    }
}
