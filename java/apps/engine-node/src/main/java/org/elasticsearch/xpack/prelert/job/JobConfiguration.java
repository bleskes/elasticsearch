/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.xpack.prelert.job.config.DefaultDetectorDescription;
import org.elasticsearch.xpack.prelert.job.config.verification.JobConfigurationVerifier;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;


/**
 * This class encapsulates all the data required to create a new job. It
 * does not represent the state of a created job (see {@linkplain JobDetails}
 * for that).
 * <p>
 * If a value has not been set it will be {@code null}. Object wrappers
 * are used around integral types &amp; booleans so they can take {@code null}
 * values.
 */
public class JobConfiguration {

    public static final long DEFAULT_TIMEOUT = 600;
    private static final int MIN_SEQUENCE_LENGTH = 5;
    private static final int HOSTNAME_ID_SEPARATORS_LENGTH = 2;
    static final AtomicLong ID_SEQUENCE = new AtomicLong();
    private static final DateTimeFormatter ID_DATEFORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String HOSTNAME;

    static {
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null) {
            hostname = System.getenv("COMPUTERNAME");
        }
        HOSTNAME = hostname != null ? hostname.toLowerCase(Locale.ROOT) : null;
    }

    private String iD;
    private String description;

    private AnalysisConfig analysisConfig = new AnalysisConfig();
    private AnalysisLimits analysisLimits;
    private SchedulerConfig.Builder schedulerConfig;
    private List<TransformConfig> transforms;
    private DataDescription dataDescription;
    private Long timeout;
    private ModelDebugConfig modelDebugConfig;
    private Long renormalizationWindowDays;
    private Long backgroundPersistInterval;
    private Long modelSnapshotRetentionDays;
    private Long resultsRetentionDays;
    private IgnoreDowntime ignoreDowntime;
    private Map<String, Object> customSettings;

    public JobConfiguration() {
    }

    public JobConfiguration(String jobId) {
        this.iD = jobId;
    }

    /**
     * The human readable job Id
     *
     * @return The provided name or null if not set
     */
    public String getId() {
        return iD;
    }

    /**
     * Set the job's ID
     *
     * @param id the id of the job
     */
    public void setId(String id) {
        iD = id;
    }

    public Map<String, Object> getCustomSettings() {
        return customSettings;
    }

    public void setCustomSettings(Map<String, Object> customSettings) {
        this.customSettings = customSettings;
    }

    /**
     * The job's human readable description
     *
     * @return the job description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the human readable description
     */
    public void setDescription(String description) {
        this.description = description;
    }


    /**
     * The analysis configuration. A properly configured job must have
     * a valid AnalysisConfig
     *
     * @return AnalysisConfig or null if not set.
     */
    public AnalysisConfig getAnalysisConfig() {
        return analysisConfig;
    }

    public void setAnalysisConfig(AnalysisConfig config) {
        analysisConfig = config;
    }

    /**
     * The analysis limits
     *
     * @return Analysis limits or null if not set.
     */
    public AnalysisLimits getAnalysisLimits() {
        return analysisLimits;
    }

    public void setAnalysisLimits(AnalysisLimits options) {
        analysisLimits = options;
    }

    /**
     * The scheduler configuration.
     *
     * @return Scheduler configuration or null if not set.
     */
    public SchedulerConfig.Builder getSchedulerConfig() {
        return schedulerConfig;
    }

    public void setSchedulerConfig(SchedulerConfig.Builder config) {
        schedulerConfig = config;
    }

    /**
     * The timeout period for the job in seconds
     *
     * @return The timeout in seconds
     */
    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public List<TransformConfig> getTransforms() {
        return transforms;
    }

    public void setTransforms(List<TransformConfig> transforms) {
        this.transforms = transforms;
    }

    /**
     * If not set the input data is assumed to be csv with a '_time' field
     * in epoch format.
     *
     * @return A DataDescription or {@code null}
     * @see DataDescription
     */
    public DataDescription getDataDescription() {
        return dataDescription;
    }

    public void setDataDescription(DataDescription description) {
        dataDescription = description;
    }

    public void setModelDebugConfig(ModelDebugConfig modelDebugConfig) {
        this.modelDebugConfig = modelDebugConfig;
    }

    public ModelDebugConfig getModelDebugConfig() {
        return modelDebugConfig;
    }

    /**
     * The background persistence interval in seconds
     *
     * @return background persistence interval in seconds
     */
    public Long getBackgroundPersistInterval() {
        return backgroundPersistInterval;
    }

    /**
     * Set the background persistence interval
     *
     * @param backgroundPersistInterval the persistence interval in seconds
     */
    public void setBackgroundPersistInterval(Long backgroundPersistInterval) {
        this.backgroundPersistInterval = backgroundPersistInterval;
    }

    /**
     * The duration of the renormalization window in days
     *
     * @return renormalization window in days
     */
    public Long getRenormalizationWindowDays() {
        return renormalizationWindowDays;
    }

    /**
     * Set the renormalization window duration
     *
     * @param renormalizationWindowDays the renormalization window in days
     */
    public void setRenormalizationWindowDays(Long renormalizationWindowDays) {
        this.renormalizationWindowDays = renormalizationWindowDays;
    }

    public Long getModelSnapshotRetentionDays() {
        return modelSnapshotRetentionDays;
    }

    public void setModelSnapshotRetentionDays(Long modelSnapshotRetentionDays) {
        this.modelSnapshotRetentionDays = modelSnapshotRetentionDays;
    }

    public Long getResultsRetentionDays() {
        return resultsRetentionDays;
    }

    public void setResultsRetentionDays(Long resultsRetentionDays) {
        this.resultsRetentionDays = resultsRetentionDays;
    }

    public IgnoreDowntime getIgnoreDowntime() {
        return ignoreDowntime;
    }

    public void setIgnoreDowntime(IgnoreDowntime ignoreDowntime) {
        this.ignoreDowntime = ignoreDowntime;
    }

    public JobDetails build() {
        String id = iD == null ? generateJobId(HOSTNAME) : iD;
        // Setting a default description should be done in Detector class itself:
        for (Detector detector : analysisConfig.getDetectors()) {
            if (detector.getDetectorDescription() == null ||
                    detector.getDetectorDescription().isEmpty()) {
                detector.setDetectorDescription(DefaultDetectorDescription.of(detector));
            }
        }
        SchedulerConfig schedulerConfig = this.schedulerConfig != null ? this.schedulerConfig.build() : null;
        return new JobDetails(
                id, description, JobStatus.CLOSED, JobSchedulerStatus.STOPPED, new Date(), null, null, DEFAULT_TIMEOUT, analysisConfig,
                analysisLimits, schedulerConfig, dataDescription, null, transforms, modelDebugConfig, new DataCounts(),
                ignoreDowntime, renormalizationWindowDays, backgroundPersistInterval, modelSnapshotRetentionDays,
                resultsRetentionDays, customSettings, null
        );
    }

    /**
     * If hostname is null the job Id is a concatenation of the date in
     * 'yyyyMMddHHmmss' format and a sequence number that is a minimum of
     * 5 digits wide left padded with zeros<br>
     * If hostname is not null the Id is the concatenation of the date in
     * 'yyyyMMddHHmmss' format the hostname and a sequence number that is a
     * minimum of 5 digits wide left padded with zeros. If hostname is long
     * and it is truncated so the job Id does not exceed the maximum length<br>
     * <p>
     * e.g. the first Id created 23rd November 2013 at 11am
     * '20131125110000-serverA-00001'
     *
     * @return The new unique job Id
     */
    static String generateJobId(String hostName) {
        String dateStr = ID_DATEFORMAT.format(LocalDateTime.now());
        long sequence = ID_SEQUENCE.incrementAndGet();
        if (hostName == null) {
            return String.format(Locale.ROOT, "%s-%05d", dateStr, sequence);
        } else {
            int formattedSequenceLen = Math.max(String.valueOf(sequence).length(), MIN_SEQUENCE_LENGTH);
            int hostnameMaxLen = JobConfigurationVerifier.MAX_JOB_ID_LENGTH - dateStr.length() -
                    formattedSequenceLen - HOSTNAME_ID_SEPARATORS_LENGTH;
            String trimmedHostName = hostName.substring(0, Math.min(hostName.length(), hostnameMaxLen));
            return String.format(Locale.ROOT, "%s-%s-%05d", dateStr, trimmedHostName, sequence);
        }
    }

}
