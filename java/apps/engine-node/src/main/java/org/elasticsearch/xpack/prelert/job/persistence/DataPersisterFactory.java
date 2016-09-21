
package org.elasticsearch.xpack.prelert.job.persistence;


/**
 * Abstract Factory method for creating new {@link JobDataPersister}
 * instances.
 */
public interface DataPersisterFactory
{
    JobDataPersister newDataPersister(String jobId);
}

