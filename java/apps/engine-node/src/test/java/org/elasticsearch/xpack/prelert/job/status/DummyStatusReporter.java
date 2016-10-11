
package org.elasticsearch.xpack.prelert.job.status;

import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.persistence.JobDataCountsPersister;
import org.elasticsearch.xpack.prelert.job.usage.UsageReporter;

/**
 * Dummy StatusReporter for testing abstract class
 */
public class DummyStatusReporter extends StatusReporter {
    boolean statusReported = false;

    public DummyStatusReporter(UsageReporter usageReporter) {
        super("DummyJobId", usageReporter, new JobDataCountsPersister() {
            @Override
            public void persistDataCounts(String jobId, DataCounts counts) {

            }
        }, null, 1);
    }

    public DummyStatusReporter(DataCounts counts,
                               UsageReporter usageReporter) {
        super("DummyJobId", counts, usageReporter, new JobDataCountsPersister() {
            @Override
            public void persistDataCounts(String jobId, DataCounts counts) {

            }
        }, null, 1);
    }


    public boolean isStatusReported() {
        return statusReported;
    }
}
