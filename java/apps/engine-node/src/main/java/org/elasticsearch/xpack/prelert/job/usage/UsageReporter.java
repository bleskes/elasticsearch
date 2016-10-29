
package org.elasticsearch.xpack.prelert.job.usage;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.persistence.UsagePersister;
import org.elasticsearch.xpack.prelert.settings.PrelertSettings;

import java.util.Locale;

/**
 * Reports the number of bytes, fields and records read.
 * Persistence is done via {@linkplain UsagePersister}
 * The main difference betweeen this and the {@linkplain org.elasticsearch.xpack.prelert.job.status.StatusReporter}
 * is that this writes hourly reports i.e. how much data was read in an hour
 */
public class UsageReporter {
    public static final String UPDATE_INTERVAL_PROP = "usage.update.interval";
    private static final long UPDATE_AFTER_COUNT_SECS = 300;

    private final String jobId;
    private final Logger logger;

    private long bytesReadSinceLastReport;
    private long fieldsReadSinceLastReport;
    private long recordsReadSinceLastReport;

    private long lastUpdateTimeMs;
    private long updateIntervalMs = UPDATE_AFTER_COUNT_SECS * 1000;

    private final UsagePersister persister;

    public UsageReporter(Environment env, String jobId, UsagePersister persister, Logger logger) {
        bytesReadSinceLastReport = 0;
        fieldsReadSinceLastReport = 0;
        recordsReadSinceLastReport = 0;

        this.jobId = jobId;
        this.persister = persister;
        this.logger = logger;

        lastUpdateTimeMs = System.currentTimeMillis();

        long interval = PrelertSettings.getSettingOrDefault(env, UPDATE_INTERVAL_PROP, UPDATE_AFTER_COUNT_SECS);
        updateIntervalMs = interval * 1000;
        this.logger.info("Setting usage update interval to " + interval + " seconds");
    }

    /**
     * Add <code>bytesRead</code> to the running total
     */
    public void addBytesRead(long bytesRead) {
        bytesReadSinceLastReport += bytesRead;

        long now = System.currentTimeMillis();

        if (now - lastUpdateTimeMs > updateIntervalMs) {
            reportUsage(now);
        }
    }

    public void addFieldsRecordsRead(long fieldsRead) {
        fieldsReadSinceLastReport += fieldsRead;
        ++recordsReadSinceLastReport;
    }

    public long getBytesReadSinceLastReport() {
        return bytesReadSinceLastReport;
    }

    public long getFieldsReadSinceLastReport() {
        return fieldsReadSinceLastReport;
    }

    public long getRecordsReadSinceLastReport() {
        return recordsReadSinceLastReport;
    }


    public String getJobId() {
        return jobId;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Logs total bytes written and calls {@linkplain UsagePersister#persistUsage(String, long, long, long)}
     * bytesReadSinceLastReport, fieldsReadSinceLastReport and
     * recordsReadSinceLastReport are reset to 0 after this has been called.
     */
    public void reportUsage() {
        reportUsage(System.currentTimeMillis());
    }

    /**
     * See {@linkplain #reportUsage()}
     *
     * @param epochMs The time now - saved as the last update time
     */
    private void reportUsage(long epochMs) {
        logger.info(String.format(Locale.ROOT, "An additional %dKiB, %d fields and %d records read by job %s",
                bytesReadSinceLastReport >> 10, fieldsReadSinceLastReport, recordsReadSinceLastReport, jobId));

        try {
            persister.persistUsage(jobId, bytesReadSinceLastReport, fieldsReadSinceLastReport, recordsReadSinceLastReport);
        } catch (JobException e) {
            logger.error("Error persisting usage for job " + jobId, e);
        }

        lastUpdateTimeMs = epochMs;

        bytesReadSinceLastReport = 0;
        fieldsReadSinceLastReport = 0;
        recordsReadSinceLastReport = 0;
    }
}
