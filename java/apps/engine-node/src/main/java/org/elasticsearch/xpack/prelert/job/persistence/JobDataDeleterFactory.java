
package org.elasticsearch.xpack.prelert.job.persistence;

public interface JobDataDeleterFactory
{
    JobDataDeleter newDeleter(String jobId);
}
