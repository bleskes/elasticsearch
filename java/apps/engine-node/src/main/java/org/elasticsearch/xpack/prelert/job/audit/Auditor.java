
package org.elasticsearch.xpack.prelert.job.audit;

public interface Auditor
{
    void info(String message);
    void warning(String message);
    void error(String message);
    void activity(String message);
    void activity(int totalJobs, int totalDetectors, int runningJobs, int runningDetectors);
}
