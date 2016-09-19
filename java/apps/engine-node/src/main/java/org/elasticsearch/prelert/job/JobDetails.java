
package org.elasticsearch.prelert.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import org.elasticsearch.prelert.job.transform.TransformConfig;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;

/**
 * This class represents a configured and created Job. The creation time is
 * set to the time the object was constructed, Status is set to
 * {@link JobStatus#RUNNING} and the finished time and last data time fields
 * are {@code null} until the job has seen some data or it is finished
 * respectively. If the job was created to read data from a list of files
 * FileUrls will be a non-empty list else the expects data to be streamed to it.
 */
@JsonInclude(Include.NON_NULL)
public class JobDetails {
    public static final long DEFAULT_TIMEOUT = 600;
    public static final long DEFAULT_BUCKETSPAN = 300;

    public static final String TYPE = "job";

    /*
     * Field names used in serialization
     */
    public static final String ANALYSIS_CONFIG = "analysisConfig";
    public static final String ANALYSIS_LIMITS = "analysisLimits";
    public static final String COUNTS = "counts";
    public static final String CREATE_TIME = "createTime";
    public static final String CUSTOM_SETTINGS = "customSettings";
    public static final String DATA_DESCRIPTION = "dataDescription";
    public static final String DESCRIPTION = "description";
    public static final String ENDPOINTS = "endpoints";
    public static final String FINISHED_TIME = "finishedTime";
    public static final String IGNORE_DOWNTIME = "ignoreDowntime";
    public static final String LAST_DATA_TIME = "lastDataTime";
    public static final String MODEL_DEBUG_CONFIG = "modelDebugConfig";
    public static final String SCHEDULER_CONFIG = "schedulerConfig";
    public static final String RENORMALIZATION_WINDOW_DAYS = "renormalizationWindowDays";
    public static final String BACKGROUND_PERSIST_INTERVAL = "backgroundPersistInterval";
    public static final String MODEL_SNAPSHOT_RETENTION_DAYS = "modelSnapshotRetentionDays";
    public static final String RESULTS_RETENTION_DAYS = "resultsRetentionDays";
    public static final String STATUS = "status";
    public static final String SCHEDULER_STATUS = "schedulerStatus";
    public static final String TIMEOUT = "timeout";
    public static final String TRANSFORMS = "transforms";

    /**
     * Endpoints key names
     */
    public static final String ALERT_LONG_POLL_ENDPOINT_KEY = "alertsLongPoll";
    public static final String BUCKETS_ENDPOINT_KEY = "buckets";
    public static final String CATEGORY_DEFINITIONS_ENDPOINT_KEY = "categoryDefinitions";
    public static final String DATA_ENDPOINT_KEY = "data";
    public static final String LOGS_ENDPOINT_KEY = "logs";
    public static final String RECORDS_ENDPOINT_KEY = "records";
    public static final String MODEL_SNAPSHOTS_ENDPOINT_KEY = "modelSnapshots";

    private String jobId;
    private String description;
    private JobStatus status;
    private JobSchedulerStatus schedulerStatus;

    private Date createTime;
    private Date finishedTime;
    private Date lastDataTime;

    private long timeout;

    private AnalysisConfig analysisConfig;
    private AnalysisLimits analysisLimits;
    private SchedulerConfig schedulerConfig;
    private DataDescription dataDescription;
    private ModelSizeStats modelSizeStats;
    private List<TransformConfig> transforms;
    private ModelDebugConfig modelDebugConfig;
    private DataCounts counts;
    private IgnoreDowntime ignoreDowntime;
    private Long renormalizationWindowDays;
    private Long backgroundPersistInterval;
    private Long modelSnapshotRetentionDays;
    private Long resultsRetentionDays;
    private Map<String, Object> customSettings;
    private Double averageBucketProcessingTimeMs;

    /* The endpoints should not be persisted in the storage */
    private URI location;


    /**
     * Default constructor required for serialisation
     */
    public JobDetails() {
        counts = new DataCounts();
        status = JobStatus.CLOSED;
        createTime = new Date();
    }

    /**
     * Create a new Job with the passed <code>jobId</code> and the
     * configuration parameters, where fields are not set in the
     * JobConfiguration defaults will be used.
     *
     * @param jobId     the job id
     * @param jobConfig the job configuration
     */
    public JobDetails(String jobId, JobConfiguration jobConfig) {
        this();

        this.jobId = jobId;
        description = jobConfig.getDescription();
        timeout = (jobConfig.getTimeout() != null) ? jobConfig.getTimeout() : DEFAULT_TIMEOUT;

        analysisConfig = jobConfig.getAnalysisConfig();
        analysisLimits = jobConfig.getAnalysisLimits();
        schedulerConfig = jobConfig.getSchedulerConfig();
        invokeIfNotNull(schedulerConfig, sc -> sc.fillDefaults());
        transforms = jobConfig.getTransforms();
        modelDebugConfig = jobConfig.getModelDebugConfig();

        invokeIfNotNull(jobConfig.getDataDescription(), dd -> dataDescription = dd);
        ignoreDowntime = jobConfig.getIgnoreDowntime();
        renormalizationWindowDays = jobConfig.getRenormalizationWindowDays();
        backgroundPersistInterval = jobConfig.getBackgroundPersistInterval();
        modelSnapshotRetentionDays = jobConfig.getModelSnapshotRetentionDays();
        resultsRetentionDays = jobConfig.getResultsRetentionDays();
        customSettings = jobConfig.getCustomSettings();
    }

    private <T> void invokeIfNotNull(T obj, Consumer<T> consumer) {
        if (obj != null) {
            consumer.accept(obj);
        }
    }

    /**
     * Return the Job Id.
     * This name is preferred when serialising to the REST API.
     *
     * @return The job Id string
     */
    @JsonView(JsonViews.RestApiView.class)
    public String getId() {
        return jobId;
    }

    /**
     * Set the job's Id.
     * In general this method should not be used as the Id does not change
     * once set. This method is provided for the Jackson object mapper to
     * de-serialise this class from Json.
     *
     * @param id the job id
     */
    public void setId(String id) {
        jobId = id;
    }

    /**
     * Return the Job Id.
     * This name is preferred when serialising to the data store.
     *
     * @return The job Id string
     */
    @JsonView(JsonViews.DatastoreView.class)
    public String getJobId() {
        return jobId;
    }

    /**
     * Set the job's Id.
     * In general this method should not be used as the Id does not change
     * once set. This method is provided for the Jackson object mapper to
     * de-serialise this class from Json.
     *
     * @param jobId the job id
     */
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    /**
     * The job description
     *
     * @return job description
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URI getLocation() {
        return location;
    }

    public void setLocation(URI location) {
        this.location = location;
    }

    /**
     * Return the Job Status. Jobs are initialised to {@link JobStatus#CLOSED}
     * when created and move into the @link JobStatus#RUNNING} state when
     * processing data. Once data has been processed the status will be
     * either {@link JobStatus#CLOSED} or {@link JobStatus#FAILED}
     *
     * @return The job's status
     */
    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public JobSchedulerStatus getSchedulerStatus() {
        return schedulerStatus;
    }

    public void setSchedulerStatus(JobSchedulerStatus schedulerStatus) {
        this.schedulerStatus = schedulerStatus;
    }

    /**
     * The Job creation time.
     * This name is preferred when serialising to the REST API.
     *
     * @return The date the job was created
     */
    @JsonView(JsonViews.RestApiView.class)
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date time) {
        createTime = time;
    }

    /**
     * The Job creation time.
     * This name is preferred when serialising to the data store.
     *
     * @return The date the job was created
     */
    @JsonView(JsonViews.DatastoreView.class)
    @JsonProperty("@timestamp")
    public Date getAtTimestamp() {
        return createTime;
    }

    @JsonProperty("@timestamp")
    public void setAtTimestamp(Date time) {
        createTime = time;
    }

    /**
     * The time the job was finished or <code>null</code> if not finished.
     *
     * @return The date the job was last retired or <code>null</code>
     */
    public Date getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(Date finishedTime) {
        this.finishedTime = finishedTime;
    }

    /**
     * The last time data was uploaded to the job or <code>null</code>
     * if no data has been seen.
     *
     * @return The date at which the last data was processed
     */
    public Date getLastDataTime() {
        return lastDataTime;
    }

    public void setLastDataTime(Date lastTime) {
        lastDataTime = lastTime;
    }

    /**
     * The job timeout setting in seconds. Jobs are retired if they do not
     * receive data for this period of time.
     * The default is 600 seconds
     *
     * @return The timeout period in seconds
     */
    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }


    /**
     * The analysis configuration object
     *
     * @return The AnalysisConfig
     */
    public AnalysisConfig getAnalysisConfig() {
        return analysisConfig;
    }

    public void setAnalysisConfig(AnalysisConfig config) {
        analysisConfig = config;
    }

    /**
     * The analysis options object
     *
     * @return The AnalysisLimits
     */
    public AnalysisLimits getAnalysisLimits() {
        return analysisLimits;
    }

    public void setAnalysisLimits(AnalysisLimits analysisLimits) {
        this.analysisLimits = analysisLimits;
    }

    public IgnoreDowntime getIgnoreDowntime() {
        return ignoreDowntime;
    }

    public void setIgnoreDowntime(IgnoreDowntime ignoreDowntime) {
        this.ignoreDowntime = ignoreDowntime;
    }

    public SchedulerConfig getSchedulerConfig() {
        return schedulerConfig;
    }

    public void setSchedulerConfig(SchedulerConfig schedulerConfig) {
        this.schedulerConfig = schedulerConfig;
    }

    public ModelDebugConfig getModelDebugConfig() {
        return modelDebugConfig;
    }

    public void setModelDebugConfig(ModelDebugConfig modelDebugConfig) {
        this.modelDebugConfig = modelDebugConfig;
    }

    /**
     * The memory usage object
     *
     * @return The ModelSizeStats
     */
    public ModelSizeStats getModelSizeStats() {
        return modelSizeStats;
    }

    public void setModelSizeStats(ModelSizeStats modelSizeStats) {
        this.modelSizeStats = modelSizeStats;
    }

    /**
     * If not set the input data is assumed to be csv with a '_time' field
     * in epoch format.
     *
     * @return A DataDescription or <code>null</code>
     * @see DataDescription
     */
    public DataDescription getDataDescription() {
        return dataDescription;
    }

    public void setDataDescription(DataDescription dd) {
        dataDescription = dd;
    }

    public List<TransformConfig> getTransforms() {
        return transforms;
    }

    public void setTransforms(List<TransformConfig> transforms) {
        this.transforms = transforms;
    }

    /**
     * Processed records count
     *
     * @return the processed records counts
     */
    public DataCounts getCounts() {
        return counts;
    }

    /**
     * Processed records count
     *
     * @param counts the counts {@code DataCounts}
     */
    public void setCounts(DataCounts counts) {
        this.counts = counts;
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
        backgroundPersistInterval = backgroundPersistInterval;
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

    public Map<String, Object> getCustomSettings() {
        return customSettings;
    }

    public void setCustomSettings(Map<String, Object> customSettings) {
        this.customSettings = customSettings;
    }

    public Double getAverageBucketProcessingTimeMs() {
        return averageBucketProcessingTimeMs;
    }

    public void setAverageBucketProcessingTimeMs(Double value) {
        averageBucketProcessingTimeMs = value;
    }

    /**
     * Get a list of all input data fields mentioned in the job configuration,
     * namely analysis fields, time field and transform input fields.
     *
     * @return the list of fields - never <code>null</code>
     */
    public List<String> allFields() {
        Set<String> allFields = new TreeSet<>();

        // analysis fields
        if (analysisConfig != null) {
            allFields.addAll(analysisConfig.analysisFields());
        }

        // transform input fields
        if (transforms != null) {
            for (TransformConfig tc : transforms) {
                List<String> inputFields = tc.getInputs();
                if (inputFields != null) {
                    allFields.addAll(inputFields);
                }
            }
        }

        // time field
        if (dataDescription != null) {
            String timeField = dataDescription.getTimeField();
            if (timeField != null) {
                allFields.add(timeField);
            }
        }

        // remove empty strings
        allFields.remove("");

        return new ArrayList<String>(allFields);
    }

    /**
     * Prints the more salient fields in a JSON-like format suitable for logging.
     * If every field was written it would spam the log file.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("id:").append(getId())
                .append(" description:").append(getDescription())
                .append(" status:").append(getStatus())
                .append(" createTime:").append(getCreateTime())
                .append(" lastDataTime:").append(getLastDataTime())
                .append("}");

        return sb.toString();
    }

    /**
     * Equality test
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof JobDetails == false) {
            return false;
        }

        JobDetails that = (JobDetails) other;

        return Objects.equals(this.jobId, that.jobId) &&
                Objects.equals(this.description, that.description) &&
                (this.status == that.status) &&
                (this.schedulerStatus == that.schedulerStatus) &&
                Objects.equals(this.createTime, that.createTime) &&
                Objects.equals(this.finishedTime, that.finishedTime) &&
                Objects.equals(this.lastDataTime, that.lastDataTime) &&
                (this.timeout == that.timeout) &&
                Objects.equals(this.analysisConfig, that.analysisConfig) &&
                Objects.equals(this.analysisLimits, that.analysisLimits) &&
                Objects.equals(this.dataDescription, that.dataDescription) &&
                Objects.equals(this.modelDebugConfig, that.modelDebugConfig) &&
                Objects.equals(this.modelSizeStats, that.modelSizeStats) &&
                Objects.equals(this.transforms, that.transforms) &&
                Objects.equals(this.counts, that.counts) &&
                Objects.equals(this.ignoreDowntime, that.ignoreDowntime) &&
                Objects.equals(this.renormalizationWindowDays, that.renormalizationWindowDays) &&
                Objects.equals(this.backgroundPersistInterval, that.backgroundPersistInterval) &&
                Objects.equals(this.modelSnapshotRetentionDays, that.modelSnapshotRetentionDays) &&
                Objects.equals(this.resultsRetentionDays, that.resultsRetentionDays) &&
                Objects.equals(this.customSettings, that.customSettings) &&
                Objects.equals(this.location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, description, status, schedulerStatus, createTime,
                finishedTime, lastDataTime, timeout, analysisConfig, analysisLimits,
                dataDescription, modelDebugConfig, modelSizeStats, transforms, counts,
                renormalizationWindowDays, backgroundPersistInterval, modelSnapshotRetentionDays,
                resultsRetentionDays, ignoreDowntime, customSettings, location);
    }
}
