package org.elasticsearch.xpack.prelert.job.extraction;

import org.elasticsearch.xpack.prelert.job.JobDetails;

public interface DataExtractorFactory
{
    DataExtractor newExtractor(JobDetails job);
}
