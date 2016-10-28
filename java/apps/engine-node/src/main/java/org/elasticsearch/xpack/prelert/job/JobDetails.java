
package org.elasticsearch.xpack.prelert.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class represents a configured and created Job. The creation time is set
 * to the time the object was constructed, Status is set to
 * {@link JobStatus#RUNNING} and the finished time and last data time fields are
 * {@code null} until the job has seen some data or it is finished respectively.
 * If the job was created to read data from a list of files FileUrls will be a
 * non-empty list else the expects data to be streamed to it.
 */
@JsonInclude(Include.NON_NULL)
public class JobDetails extends ToXContentToBytes implements Writeable {

    public static final long DEFAULT_BUCKETSPAN = 300;

    public static final String TYPE = "job";

    /*
     * Field names used in serialization
     */
    public static final ParseField ID = new ParseField("jobId");
    public static final ParseField ANALYSIS_CONFIG = new ParseField("analysisConfig");
    public static final ParseField ANALYSIS_LIMITS = new ParseField("analysisLimits");
    public static final ParseField COUNTS = new ParseField("counts");
    public static final ParseField CREATE_TIME = new ParseField("createTime");
    public static final ParseField CUSTOM_SETTINGS = new ParseField("customSettings");
    public static final ParseField DATA_DESCRIPTION = new ParseField("dataDescription");
    public static final ParseField DESCRIPTION = new ParseField("description");
    public static final ParseField FINISHED_TIME = new ParseField("finishedTime");
    public static final ParseField IGNORE_DOWNTIME = new ParseField("ignoreDowntime");
    public static final ParseField LAST_DATA_TIME = new ParseField("lastDataTime");
    public static final ParseField MODEL_DEBUG_CONFIG = new ParseField("modelDebugConfig");
    public static final ParseField SCHEDULER_CONFIG = new ParseField("schedulerConfig");
    public static final ParseField RENORMALIZATION_WINDOW_DAYS = new ParseField("renormalizationWindowDays");
    public static final ParseField BACKGROUND_PERSIST_INTERVAL = new ParseField("backgroundPersistInterval");
    public static final ParseField MODEL_SNAPSHOT_RETENTION_DAYS = new ParseField("modelSnapshotRetentionDays");
    public static final ParseField RESULTS_RETENTION_DAYS = new ParseField("resultsRetentionDays");
    public static final ParseField STATUS = new ParseField("status");
    public static final ParseField SCHEDULER_STATE = new ParseField("schedulerState");
    public static final ParseField TIMEOUT = new ParseField("timeout");
    public static final ParseField TRANSFORMS = new ParseField("transforms");
    public static final ParseField MODEL_SIZE_STATS = new ParseField("modelSizeStats");
    public static final ParseField AVERAGE_BUCKET_PROCESSING_TIME = new ParseField("averageBucketProcessingTimeMs");

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

    public static final ObjectParser<JobDetails, ParseFieldMatcherSupplier> PARSER = new ObjectParser<>("job_details", JobDetails::new);

    static {
        PARSER.declareString(JobDetails::setId, ID);
        PARSER.declareStringOrNull(JobDetails::setDescription, DESCRIPTION);
        PARSER.declareField(JobDetails::setStatus, (p, c) -> JobStatus.fromString(p.text()), STATUS, ValueType.STRING);
        PARSER.declareObject(JobDetails::setSchedulerState, SchedulerState.PARSER, SCHEDULER_STATE);
        PARSER.declareField(JobDetails::setCreateTime, (p, c) -> new Date(p.longValue()), CREATE_TIME, ValueType.LONG);
        PARSER.declareField(JobDetails::setFinishedTime, (p, c) -> new Date(p.longValue()), FINISHED_TIME, ValueType.LONG);
        PARSER.declareField(JobDetails::setLastDataTime, (p, c) -> new Date(p.longValue()), LAST_DATA_TIME, ValueType.LONG);
        PARSER.declareLong(JobDetails::setTimeout, TIMEOUT);
        PARSER.declareObject(JobDetails::setAnalysisConfig, AnalysisConfig.PARSER, ANALYSIS_CONFIG);
        PARSER.declareObject(JobDetails::setAnalysisLimits, AnalysisLimits.PARSER, ANALYSIS_LIMITS);
        PARSER.declareObject(JobDetails::setSchedulerConfig, (p, c) -> SchedulerConfig.PARSER.apply(p, c).build(), SCHEDULER_CONFIG);
        PARSER.declareObject(JobDetails::setDataDescription, DataDescription.PARSER, DATA_DESCRIPTION);
        PARSER.declareObject(JobDetails::setModelSizeStats, ModelSizeStats.PARSER, MODEL_SIZE_STATS);
        PARSER.declareObjectArray(JobDetails::setTransforms, TransformConfig.PARSER, TRANSFORMS);
        PARSER.declareObject(JobDetails::setModelDebugConfig, ModelDebugConfig.PARSER, MODEL_DEBUG_CONFIG);
        PARSER.declareObject(JobDetails::setCounts, DataCounts.PARSER, COUNTS);
        PARSER.declareField(JobDetails::setIgnoreDowntime, (p, c) -> IgnoreDowntime.fromString(p.text()), IGNORE_DOWNTIME,
                ValueType.STRING);
        PARSER.declareLong(JobDetails::setRenormalizationWindowDays, RENORMALIZATION_WINDOW_DAYS);
        PARSER.declareLong(JobDetails::setBackgroundPersistInterval, BACKGROUND_PERSIST_INTERVAL);
        PARSER.declareLong(JobDetails::setResultsRetentionDays, RESULTS_RETENTION_DAYS);
        PARSER.declareLong(JobDetails::setModelSnapshotRetentionDays, MODEL_SNAPSHOT_RETENTION_DAYS);
        PARSER.declareField(JobDetails::setCustomSettings, (p, c) -> p.map(), CUSTOM_SETTINGS, ValueType.OBJECT);
        PARSER.declareDouble(JobDetails::setAverageBucketProcessingTimeMs, AVERAGE_BUCKET_PROCESSING_TIME);
    }

    private String jobId;
    private String description;
    private JobStatus status;
    private SchedulerState schedulerState;

    // NORELEASE: Use Jodatime instead
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
    private String modelSnapshotId;

    private JobDetails() {
    }

    public JobDetails(String jobId, String description, JobStatus status, SchedulerState schedulerState, Date createTime, Date finishedTime,
            Date lastDataTime, long timeout, AnalysisConfig analysisConfig, AnalysisLimits analysisLimits, SchedulerConfig schedulerConfig,
            DataDescription dataDescription, ModelSizeStats modelSizeStats, List<TransformConfig> transforms,
            ModelDebugConfig modelDebugConfig, DataCounts counts, IgnoreDowntime ignoreDowntime, Long renormalizationWindowDays,
            Long backgroundPersistInterval, Long modelSnapshotRetentionDays, Long resultsRetentionDays, Map<String, Object> customSettings,
            Double averageBucketProcessingTimeMs) {
        this.jobId = jobId;
        this.description = description;
        this.status = status;
        this.schedulerState = schedulerState;
        this.createTime = createTime;
        this.finishedTime = finishedTime;
        this.lastDataTime = lastDataTime;
        this.timeout = timeout;
        this.analysisConfig = analysisConfig;
        this.analysisLimits = analysisLimits;
        this.schedulerConfig = schedulerConfig;
        this.dataDescription = dataDescription;
        this.modelSizeStats = modelSizeStats;
        this.transforms = transforms;
        this.modelDebugConfig = modelDebugConfig;
        this.counts = counts;
        this.ignoreDowntime = ignoreDowntime;
        this.renormalizationWindowDays = renormalizationWindowDays;
        this.backgroundPersistInterval = backgroundPersistInterval;
        this.modelSnapshotRetentionDays = modelSnapshotRetentionDays;
        this.resultsRetentionDays = resultsRetentionDays;
        this.customSettings = customSettings;
        this.averageBucketProcessingTimeMs = averageBucketProcessingTimeMs;
    }

    public JobDetails(StreamInput in) throws IOException {
        jobId = in.readString();
        description = in.readOptionalString();
        status = JobStatus.fromStream(in);
        schedulerState = in.readOptionalWriteable(SchedulerState::new);
        createTime = new Date(in.readVLong());
        if (in.readBoolean()) {
            finishedTime = new Date(in.readVLong());
        }
        if (in.readBoolean()) {
            lastDataTime = new Date(in.readVLong());
        }
        timeout = in.readVLong();
        analysisConfig = new AnalysisConfig(in);
        analysisLimits = in.readOptionalWriteable(AnalysisLimits::new);
        schedulerConfig = in.readOptionalWriteable(SchedulerConfig::new);
        dataDescription = in.readOptionalWriteable(DataDescription::new);
        modelSizeStats = in.readOptionalWriteable(ModelSizeStats::new);
        transforms = in.readList(TransformConfig::new);
        modelDebugConfig = in.readOptionalWriteable(ModelDebugConfig::new);
        counts = in.readOptionalWriteable(DataCounts::new);
        if (in.readBoolean()) {
            ignoreDowntime = IgnoreDowntime.fromStream(in);
        }
        renormalizationWindowDays = in.readOptionalLong();
        backgroundPersistInterval = in.readOptionalLong();
        modelSnapshotRetentionDays = in.readOptionalLong();
        resultsRetentionDays = in.readOptionalLong();
        customSettings = in.readMap();
        averageBucketProcessingTimeMs = in.readOptionalDouble();
    }

    /**
     * Return the Job Id. This name is preferred when serialising to the REST
     * API.
     *
     * @return The job Id string
     */
    @JsonView(JsonViews.RestApiView.class)
    public String getId() {
        return jobId;
    }

    /**
     * Set the job's Id. In general this method should not be used as the Id
     * does not change once set. This method is provided for the Jackson object
     * mapper to de-serialise this class from Json.
     *
     * @param id
     *            the job id
     */
    public void setId(String id) {
        jobId = id;
    }

    /**
     * Return the Job Id. This name is preferred when serialising to the data
     * store.
     *
     * @return The job Id string
     */
    @JsonView(JsonViews.DatastoreView.class)
    public String getJobId() {
        return jobId;
    }

    /**
     * Set the job's Id. In general this method should not be used as the Id
     * does not change once set. This method is provided for the Jackson object
     * mapper to de-serialise this class from Json.
     *
     * @param jobId
     *            the job id
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

    /**
     * Return the Job Status. Jobs are initialised to {@link JobStatus#CLOSED}
     * when created and move into the @link JobStatus#RUNNING} state when
     * processing data. Once data has been processed the status will be either
     * {@link JobStatus#CLOSED} or {@link JobStatus#FAILED}
     *
     * @return The job's status
     */
    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public SchedulerState getSchedulerState() {
        return schedulerState;
    }

    public void setSchedulerState(SchedulerState schedulerState) {
        this.schedulerState = schedulerState;
    }

    /**
     * The Job creation time. This name is preferred when serialising to the
     * REST API.
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
     * The Job creation time. This name is preferred when serialising to the
     * data store.
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
     * The last time data was uploaded to the job or <code>null</code> if no
     * data has been seen.
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
     * receive data for this period of time. The default is 600 seconds
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
        // TODO: remove once JobDetail is immutable
        if (this.analysisLimits != null) {
            long oldMemoryLimit = this.analysisLimits.getModelMemoryLimit();
            long newMemoryLimit = analysisLimits.getModelMemoryLimit();
            if (newMemoryLimit < oldMemoryLimit) {
                throw ExceptionsHelper.invalidRequestException(
                        Messages.getMessage(Messages.JOB_CONFIG_UPDATE_ANALYSIS_LIMITS_MODEL_MEMORY_LIMIT_CANNOT_BE_DECREASED,
                                oldMemoryLimit, newMemoryLimit),
                        ErrorCodes.INVALID_VALUE);
            }
        }

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
     * If not set the input data is assumed to be csv with a '_time' field in
     * epoch format.
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
     * @param counts
     *            the counts {@code DataCounts}
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
     * @param renormalizationWindowDays
     *            the renormalization window in days
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
     * @param backgroundPersistInterval
     *            the persistence interval in seconds
     */
    public void setBackgroundPersistInterval(Long backgroundPersistInterval) {
        this.backgroundPersistInterval = backgroundPersistInterval;
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

    public String getModelSnapshotId() {
        return modelSnapshotId;
    }

    public void setModelSnapshotId(String modelSnapshotId) {
        this.modelSnapshotId = modelSnapshotId;
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

        return new ArrayList<>(allFields);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(jobId);
        out.writeOptionalString(description);
        status.writeTo(out);
        out.writeOptionalWriteable(schedulerState);
        out.writeVLong(createTime.getTime());
        if (finishedTime != null) {
            out.writeBoolean(true);
            out.writeVLong(finishedTime.getTime());
        } else {
            out.writeBoolean(false);
        }
        if (lastDataTime != null) {
            out.writeBoolean(true);
            out.writeVLong(lastDataTime.getTime());
        } else {
            out.writeBoolean(false);
        }
        out.writeVLong(timeout);
        analysisConfig.writeTo(out);
        out.writeOptionalWriteable(analysisLimits);
        out.writeOptionalWriteable(schedulerConfig);
        out.writeOptionalWriteable(dataDescription);
        out.writeOptionalWriteable(modelSizeStats);
        out.writeList(transforms);
        out.writeOptionalWriteable(modelDebugConfig);
        out.writeOptionalWriteable(counts);
        boolean hasIgnoreDowntime = ignoreDowntime != null;
        out.writeBoolean(hasIgnoreDowntime);
        if (hasIgnoreDowntime) {
            ignoreDowntime.writeTo(out);
        }
        out.writeOptionalLong(renormalizationWindowDays);
        out.writeOptionalLong(backgroundPersistInterval);
        out.writeOptionalLong(modelSnapshotRetentionDays);
        out.writeOptionalLong(resultsRetentionDays);
        out.writeMap(customSettings);
        out.writeOptionalDouble(averageBucketProcessingTimeMs);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        doXContentBody(builder, params);
        builder.endObject();
        return builder;
    }

    public XContentBuilder doXContentBody(XContentBuilder builder, Params params) throws IOException {
        builder.field(ID.getPreferredName(), jobId);
        builder.field(DESCRIPTION.getPreferredName(), description);
        builder.field(STATUS.getPreferredName(), status);
        if (schedulerState != null) {
            builder.field(SCHEDULER_STATE.getPreferredName(), schedulerState);
        }
        builder.field(CREATE_TIME.getPreferredName(), createTime.getTime());
        if (finishedTime != null) {
            builder.field(FINISHED_TIME.getPreferredName(), finishedTime.getTime());
        }
        if (lastDataTime != null) {
            builder.field(LAST_DATA_TIME.getPreferredName(), lastDataTime.getTime());
        }
        builder.field(TIMEOUT.getPreferredName(), timeout);
        builder.field(ANALYSIS_CONFIG.getPreferredName(), analysisConfig);
        if (analysisLimits != null) {
            builder.field(ANALYSIS_LIMITS.getPreferredName(), analysisLimits);
        }
        if (schedulerConfig != null) {
            builder.field(SCHEDULER_CONFIG.getPreferredName(), schedulerConfig);
        }
        if (dataDescription != null) {
            builder.field(DATA_DESCRIPTION.getPreferredName(), dataDescription);
        }
        if (modelSizeStats != null) {
            builder.field(MODEL_SIZE_STATS.getPreferredName(), modelSizeStats);
        }
        builder.field(TRANSFORMS.getPreferredName(), transforms);
        if (modelDebugConfig != null) {
            builder.field(MODEL_DEBUG_CONFIG.getPreferredName(), modelDebugConfig);
        }
        if (counts != null) {
            builder.field(COUNTS.getPreferredName(), counts);
        }
        if (ignoreDowntime != null) {
            builder.field(IGNORE_DOWNTIME.getPreferredName(), ignoreDowntime);
        }
        if (renormalizationWindowDays != null) {
            builder.field(RENORMALIZATION_WINDOW_DAYS.getPreferredName(), renormalizationWindowDays);
        }
        if (backgroundPersistInterval != null) {
            builder.field(BACKGROUND_PERSIST_INTERVAL.getPreferredName(), backgroundPersistInterval);
        }
        if (modelSnapshotRetentionDays != null) {
            builder.field(MODEL_SNAPSHOT_RETENTION_DAYS.getPreferredName(), modelSnapshotRetentionDays);
        }
        if (resultsRetentionDays != null) {
            builder.field(RESULTS_RETENTION_DAYS.getPreferredName(), resultsRetentionDays);
        }
        if (customSettings != null) {
            builder.field(CUSTOM_SETTINGS.getPreferredName(), customSettings);
        }
        if (averageBucketProcessingTimeMs != null) {
            builder.field(AVERAGE_BUCKET_PROCESSING_TIME.getPreferredName(), averageBucketProcessingTimeMs);
        }
        return builder;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof JobDetails == false) {
            return false;
        }

        JobDetails that = (JobDetails) other;
        return Objects.equals(this.jobId, that.jobId) && Objects.equals(this.description, that.description) && (this.status == that.status)
                && Objects.equals(this.schedulerState, that.schedulerState) && Objects.equals(this.createTime, that.createTime)
                && Objects.equals(this.finishedTime, that.finishedTime) && Objects.equals(this.lastDataTime, that.lastDataTime)
                && (this.timeout == that.timeout) && Objects.equals(this.analysisConfig, that.analysisConfig)
                && Objects.equals(this.analysisLimits, that.analysisLimits) && Objects.equals(this.dataDescription, that.dataDescription)
                && Objects.equals(this.modelDebugConfig, that.modelDebugConfig) && Objects.equals(this.modelSizeStats, that.modelSizeStats)
                && Objects.equals(this.transforms, that.transforms) && Objects.equals(this.counts, that.counts)
                && Objects.equals(this.ignoreDowntime, that.ignoreDowntime)
                && Objects.equals(this.renormalizationWindowDays, that.renormalizationWindowDays)
                && Objects.equals(this.backgroundPersistInterval, that.backgroundPersistInterval)
                && Objects.equals(this.modelSnapshotRetentionDays, that.modelSnapshotRetentionDays)
                && Objects.equals(this.resultsRetentionDays, that.resultsRetentionDays)
                && Objects.equals(this.customSettings, that.customSettings) && Objects.equals(this.modelSnapshotId, that.modelSnapshotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, description, status, schedulerState, createTime, finishedTime, lastDataTime, timeout, analysisConfig,
                analysisLimits, dataDescription, modelDebugConfig, modelSizeStats, transforms, counts, renormalizationWindowDays,
                backgroundPersistInterval, modelSnapshotRetentionDays, resultsRetentionDays, ignoreDowntime, customSettings,
                modelSnapshotId);
    }
}
