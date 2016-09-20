
package org.elasticsearch.xpack.prelert.job.audit;

import java.util.Date;

public class AuditActivity
{
    public static final String TYPE = "auditActivity";

    public static final String TOTAL_JOBS = "totalJobs";
    public static final String TOTAL_DETECTORS = "totalDetectors";
    public static final String RUNNING_JOBS = "runningJobs";
    public static final String RUNNING_DETECTORS = "runningDetectors";

    private int totalJobs;
    private int totalDetectors;
    private int runningJobs;
    private int runningDetectors;
    private Date timestamp;

    public AuditActivity()
    {
        // Default constructor
    }

    private AuditActivity(int totalJobs, int totalDetectors, int runningJobs, int runningDetectors)
    {
        this.totalJobs = totalJobs;
        this.totalDetectors = totalDetectors;
        this.runningJobs = runningJobs;
        this.runningDetectors = runningDetectors;
        timestamp = new Date();
    }

    public int getTotalJobs()
    {
        return totalJobs;
    }

    public void setTotalJobs(int totalJobs)
    {
        this.totalJobs = totalJobs;
    }

    public int getTotalDetectors()
    {
        return totalDetectors;
    }

    public void setTotalDetectors(int totalDetectors)
    {
        this.totalDetectors = totalDetectors;
    }

    public int getRunningJobs()
    {
        return runningJobs;
    }

    public void setRunningJobs(int runningJobs)
    {
        this.runningJobs = runningJobs;
    }

    public int getRunningDetectors()
    {
        return runningDetectors;
    }

    public void setRunningDetectors(int runningDetectors)
    {
        this.runningDetectors = runningDetectors;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    public static AuditActivity newActivity(int totalJobs, int totalDetectors, int runningJobs, int runningDetectors)
    {
        return new AuditActivity(totalJobs, totalDetectors, runningJobs, runningDetectors);
    }
}
