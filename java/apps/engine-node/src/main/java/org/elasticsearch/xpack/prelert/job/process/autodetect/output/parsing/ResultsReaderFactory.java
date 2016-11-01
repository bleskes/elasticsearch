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
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPeristerFactory;
import org.elasticsearch.xpack.prelert.job.process.normalizer.RenormaliserFactory;

import java.io.InputStream;

/**
 * Factory method for creating new {@linkplain ResultsReader} objects
 * to parse the autodetect output.
 * Requires 2 other factories for creating the {@linkplain ResultsReader}
 */
public class ResultsReaderFactory {
    private final JobResultsPeristerFactory persisterFactory;
    private final RenormaliserFactory renormaliserFactory;

    public ResultsReaderFactory(JobResultsPeristerFactory persisterFactory,
            RenormaliserFactory renormaliserFactory) {
        this.persisterFactory = persisterFactory;
        this.renormaliserFactory = renormaliserFactory;
    }

    public ResultsReader newResultsParser(String jobId, InputStream autoDetectOutputStream,
            Logger logger, boolean isPerPartitionNormalization) {
        return new ResultsReader(renormaliserFactory.create(jobId),
                persisterFactory.jobResultsPersister(jobId),
                autoDetectOutputStream, logger, isPerPartitionNormalization);
    }
}
