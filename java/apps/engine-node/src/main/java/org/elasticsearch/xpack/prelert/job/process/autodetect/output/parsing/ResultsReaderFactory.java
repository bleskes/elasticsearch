
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
