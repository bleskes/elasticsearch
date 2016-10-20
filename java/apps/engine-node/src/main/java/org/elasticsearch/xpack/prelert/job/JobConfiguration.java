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

import com.fasterxml.jackson.annotation.JsonInclude;
import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.prelert.job.config.DefaultDetectorDescription;
import org.elasticsearch.xpack.prelert.job.config.verification.JobConfigurationVerifier;
import org.elasticsearch.xpack.prelert.job.metadata.Job;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
// NORELEASE: this is now a builder only for when a users creates a new job, but it should be for other cases too,
// for example when a job gets updated. This will also allow us to reuse serialization logic between JobDetails and this class.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobConfiguration extends ToXContentToBytes implements Writeable {

    public static final long DEFAULT_TIMEOUT = 600;
    private static final int MIN_SEQUENCE_LENGTH = 5;
    private static final int HOSTNAME_ID_SEPARATORS_LENGTH = 2;
    static final AtomicLong ID_SEQUENCE = new AtomicLong(); // package protected for testing
    private static final DateTimeFormatter ID_DATEFORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String HOSTNAME;

    static {
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null) {
            hostname = System.getenv("COMPUTERNAME");
        }
        HOSTNAME = hostname != null ? hostname.toLowerCase(Locale.ROOT) : null;
    }

    public static final ObjectParser<JobConfiguration, ParseFieldMatcherSupplier> PARSER =
            new ObjectParser<>("job_configuration", JobConfiguration::new);

    static {
        PARSER.declareString(JobConfiguration::setId, JobDetails.ID);
        PARSER.declareStringOrNull(JobConfiguration::setDescription, JobDetails.DESCRIPTION);
        PARSER.declareLong(JobConfiguration::setTimeout, JobDetails.TIMEOUT);
        PARSER.declareObject(JobConfiguration::setAnalysisConfig, AnalysisConfig.PARSER, JobDetails.ANALYSIS_CONFIG);
        PARSER.declareObject(JobConfiguration::setAnalysisLimits, AnalysisLimits.PARSER, JobDetails.ANALYSIS_LIMITS);
        PARSER.declareObject(JobConfiguration::setSchedulerConfig, SchedulerConfig.PARSER, JobDetails.SCHEDULER_CONFIG);
        PARSER.declareObject(JobConfiguration::setDataDescription, DataDescription.PARSER, JobDetails.DATA_DESCRIPTION);
        PARSER.declareObjectArray(JobConfiguration::setTransforms, TransformConfig.PARSER, JobDetails.TRANSFORMS);
        PARSER.declareObject(JobConfiguration::setModelDebugConfig, ModelDebugConfig.PARSER, JobDetails.MODEL_DEBUG_CONFIG);
        PARSER.declareField(JobConfiguration::setIgnoreDowntime, (p, c) -> IgnoreDowntime.fromString(p.text()),
                JobDetails.IGNORE_DOWNTIME, ObjectParser.ValueType.STRING);
        PARSER.declareLong(JobConfiguration::setRenormalizationWindowDays, JobDetails.RENORMALIZATION_WINDOW_DAYS);
        PARSER.declareLong(JobConfiguration::setBackgroundPersistInterval, JobDetails.BACKGROUND_PERSIST_INTERVAL);
        PARSER.declareLong(JobConfiguration::setResultsRetentionDays, JobDetails.RESULTS_RETENTION_DAYS);
        PARSER.declareLong(JobConfiguration::setModelSnapshotRetentionDays, JobDetails.MODEL_SNAPSHOT_RETENTION_DAYS);
        PARSER.declareField(JobConfiguration::setCustomSettings, (p, c) -> p.map(), JobDetails.CUSTOM_SETTINGS,
                ObjectParser.ValueType.OBJECT);
    }

    private String iD;
    private String description;

    private AnalysisConfig analysisConfig = new AnalysisConfig();
    private AnalysisLimits analysisLimits;
    private SchedulerConfig schedulerConfig;
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

    public JobConfiguration(StreamInput in) throws IOException {
        iD = in.readString();
        description = in.readOptionalString();
        analysisConfig = new AnalysisConfig(in);
        analysisLimits = in.readOptionalWriteable(AnalysisLimits::new);
        schedulerConfig = in.readOptionalWriteable(SchedulerConfig::new);
        if (in.readBoolean()) {
            transforms = in.readList(TransformConfig::new);
        }
        dataDescription = in.readOptionalWriteable(DataDescription::new);
        timeout = in.readOptionalLong();
        modelDebugConfig = in.readOptionalWriteable(ModelDebugConfig::new);
        renormalizationWindowDays = in.readOptionalLong();
        backgroundPersistInterval = in.readOptionalLong();
        modelSnapshotRetentionDays = in.readOptionalLong();
        resultsRetentionDays = in.readOptionalLong();
        ignoreDowntime = in.readOptionalWriteable(IgnoreDowntime::fromStream);
        customSettings = in.readMap();
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
    public SchedulerConfig getSchedulerConfig() {
        return schedulerConfig;
    }

    public void setSchedulerConfig(SchedulerConfig.Builder config) {
        schedulerConfig = config.build();
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
        return new JobDetails(
                id, description, JobStatus.CLOSED, JobSchedulerStatus.STOPPED, new Date(), null, null, DEFAULT_TIMEOUT, analysisConfig,
                analysisLimits, schedulerConfig, dataDescription, null, transforms, modelDebugConfig, new DataCounts(),
                ignoreDowntime, renormalizationWindowDays, backgroundPersistInterval, modelSnapshotRetentionDays,
                resultsRetentionDays, customSettings, null
        );
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(iD);
        out.writeOptionalString(description);
        analysisConfig.writeTo(out);
        out.writeOptionalWriteable(analysisLimits);
        out.writeOptionalWriteable(schedulerConfig);
        if (transforms != null) {
            out.writeBoolean(true);
            out.writeList(transforms);
        } else {
            out.writeBoolean(false);
        }
        out.writeOptionalWriteable(dataDescription);
        out.writeOptionalLong(timeout);
        out.writeOptionalWriteable(modelDebugConfig);
        out.writeOptionalLong(renormalizationWindowDays);
        out.writeOptionalLong(backgroundPersistInterval);
        out.writeOptionalLong(modelSnapshotRetentionDays);
        out.writeOptionalLong(resultsRetentionDays);
        out.writeOptionalWriteable(ignoreDowntime);
        out.writeMap(customSettings);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(JobDetails.ID.getPreferredName(), iD);
        if (description != null) {
            builder.field(JobDetails.DESCRIPTION.getPreferredName(), description);
        }
        builder.field(JobDetails.ANALYSIS_CONFIG.getPreferredName(), analysisConfig);
        if (analysisLimits != null) {
            builder.field(JobDetails.ANALYSIS_LIMITS.getPreferredName(), analysisLimits);
        }
        if (schedulerConfig != null) {
            builder.field(JobDetails.SCHEDULER_CONFIG.getPreferredName(), schedulerConfig);
        }
        if (transforms != null) {
            builder.field(JobDetails.TRANSFORMS.getPreferredName(), transforms);
        }
        if (dataDescription != null) {
            builder.field(JobDetails.DATA_DESCRIPTION.getPreferredName(), dataDescription);
        }
        if (timeout != null) {
            builder.field(JobDetails.TIMEOUT.getPreferredName(), timeout);
        }
        if (modelDebugConfig != null) {
            builder.field(JobDetails.MODEL_DEBUG_CONFIG.getPreferredName(), modelDebugConfig);
        }
        if (renormalizationWindowDays != null) {
            builder.field(JobDetails.RENORMALIZATION_WINDOW_DAYS.getPreferredName(), renormalizationWindowDays);
        }
        if (backgroundPersistInterval != null) {
            builder.field(JobDetails.BACKGROUND_PERSIST_INTERVAL.getPreferredName(), backgroundPersistInterval);
        }
        if (modelSnapshotRetentionDays != null) {
            builder.field(JobDetails.MODEL_SNAPSHOT_RETENTION_DAYS.getPreferredName(), modelSnapshotRetentionDays);
        }
        if (resultsRetentionDays != null) {
            builder.field(JobDetails.RESULTS_RETENTION_DAYS.getPreferredName(), resultsRetentionDays);
        }
        if (ignoreDowntime != null) {
            builder.field(JobDetails.IGNORE_DOWNTIME.getPreferredName(), ignoreDowntime);
        }
        if (customSettings != null) {
            builder.field(JobDetails.CUSTOM_SETTINGS.getPreferredName(), customSettings);
        }
        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobConfiguration that = (JobConfiguration) o;
        return Objects.equals(iD, that.iD) &&
                Objects.equals(description, that.description) &&
                Objects.equals(analysisConfig, that.analysisConfig) &&
                Objects.equals(analysisLimits, that.analysisLimits) &&
                Objects.equals(schedulerConfig, that.schedulerConfig) &&
                Objects.equals(transforms, that.transforms) &&
                Objects.equals(dataDescription, that.dataDescription) &&
                Objects.equals(timeout, that.timeout) &&
                Objects.equals(modelDebugConfig, that.modelDebugConfig) &&
                Objects.equals(renormalizationWindowDays, that.renormalizationWindowDays) &&
                Objects.equals(backgroundPersistInterval, that.backgroundPersistInterval) &&
                Objects.equals(modelSnapshotRetentionDays, that.modelSnapshotRetentionDays) &&
                Objects.equals(resultsRetentionDays, that.resultsRetentionDays) &&
                ignoreDowntime == that.ignoreDowntime &&
                Objects.equals(customSettings, that.customSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                iD, description, analysisConfig, analysisLimits, schedulerConfig, transforms, dataDescription, timeout,
                modelDebugConfig, renormalizationWindowDays, backgroundPersistInterval, modelSnapshotRetentionDays,
                resultsRetentionDays, ignoreDowntime, customSettings
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
