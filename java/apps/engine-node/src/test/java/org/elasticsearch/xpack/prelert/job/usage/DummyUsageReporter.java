
package org.elasticsearch.xpack.prelert.job.usage;

import org.apache.logging.log4j.Logger;

import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.persistence.UsagePersister;

public class DummyUsageReporter extends UsageReporter {
    long totalByteCount;
    long totalFieldCount;
    long totalRecordCount;

    public DummyUsageReporter(String jobId, Logger logger) {
        super(jobId, new UsagePersister() {
            @Override
            public void persistUsage(String jobId, long bytesRead, long fieldsRead, long recordsRead) throws JobException {

            }
        }, logger);

        totalByteCount = 0;
        totalFieldCount = 0;
        totalRecordCount = 0;
    }

    public DummyUsageReporter(String jobId, UsagePersister persister, Logger logger) {
        super(jobId, persister, logger);

        totalByteCount = 0;
        totalFieldCount = 0;
        totalRecordCount = 0;
    }


    @Override
    public void addBytesRead(long bytesRead) {
        super.addBytesRead(bytesRead);

        totalByteCount += bytesRead;
    }

    @Override
    public void addFieldsRecordsRead(long fieldsRead) {
        super.addFieldsRecordsRead(fieldsRead);

        totalFieldCount += fieldsRead;
        ++totalRecordCount;
    }

    public long getTotalBytesRead() {
        return totalByteCount;
    }

    public long getTotalFieldsRead() {
        return totalFieldCount;
    }

    public long getTotalRecordsRead() {
        return totalRecordCount;
    }

}
