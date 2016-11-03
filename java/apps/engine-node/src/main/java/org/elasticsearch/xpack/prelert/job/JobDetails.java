/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.cluster.AbstractDiffable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.common.xcontent.XContentParser.Token;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.xpack.prelert.job.config.verification.AnalysisConfigVerifier;
import org.elasticsearch.xpack.prelert.job.config.verification.DataDescriptionVerifier;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfigs;
import org.elasticsearch.xpack.prelert.job.transform.verification.TransformConfigsVerifier;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;
import org.elasticsearch.xpack.prelert.utils.time.TimeUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * This class represents a configured and created Job. The creation time is set
 * to the time the object was constructed, Status is set to
 * {@link JobStatus#RUNNING} and the finished time and last data time fields are
 * {@code null} until the job has seen some data or it is finished respectively.
 * If the job was created to read data from a list of files FileUrls will be a
 * non-empty list else the expects data to be streamed to it.
 */
public class JobDetails extends AbstractDiffable<JobDetails> implements Writeable, ToXContent {

    public static final JobDetails PROTO = new JobDetails(null, null, null, null, null, 0L, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null);

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
    public static final ParseField TIMEOUT = new ParseField("timeout");
    public static final ParseField TRANSFORMS = new ParseField("transforms");
    public static final ParseField MODEL_SIZE_STATS = new ParseField("modelSizeStats");
    public static final ParseField AVERAGE_BUCKET_PROCESSING_TIME = new ParseField("averageBucketProcessingTimeMs");
    public static final ParseField MODEL_SNAPSHOT_ID = new ParseField("modelSnapshotId");

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

    public static final ObjectParser<Builder, ParseFieldMatcherSupplier> PARSER = new ObjectParser<>("job_details", Builder::new);

    static {
        PARSER.declareString(Builder::setId, ID);
        PARSER.declareStringOrNull(Builder::setDescription, DESCRIPTION);
        PARSER.declareField(Builder::setCreateTime, p -> {
            if (p.currentToken() == Token.VALUE_NUMBER) {
                return new Date(p.longValue());
            } else if (p.currentToken() == Token.VALUE_STRING) {
                return new Date(TimeUtils.dateStringToEpoch(p.text()));
            }
            throw new IllegalArgumentException("unexpected token [" + p.currentToken() + "] for [" + CREATE_TIME.getPreferredName() + "]");
        }, CREATE_TIME, ValueType.VALUE);
        PARSER.declareField(Builder::setFinishedTime, p -> {
            if (p.currentToken() == Token.VALUE_NUMBER) {
                return new Date(p.longValue());
            } else if (p.currentToken() == Token.VALUE_STRING) {
                return new Date(TimeUtils.dateStringToEpoch(p.text()));
            }
            throw new IllegalArgumentException(
                    "unexpected token [" + p.currentToken() + "] for [" + FINISHED_TIME.getPreferredName() + "]");
        }, FINISHED_TIME, ValueType.VALUE);
        PARSER.declareField(Builder::setLastDataTime, p -> {
            if (p.currentToken() == Token.VALUE_NUMBER) {
                return new Date(p.longValue());
            } else if (p.currentToken() == Token.VALUE_STRING) {
                return new Date(TimeUtils.dateStringToEpoch(p.text()));
            }
            throw new IllegalArgumentException(
                    "unexpected token [" + p.currentToken() + "] for [" + LAST_DATA_TIME.getPreferredName() + "]");
        }, LAST_DATA_TIME, ValueType.VALUE);
        PARSER.declareObject(Builder::setAnalysisConfig, AnalysisConfig.PARSER, ANALYSIS_CONFIG);
        PARSER.declareObject(Builder::setAnalysisLimits, AnalysisLimits.PARSER, ANALYSIS_LIMITS);
        PARSER.declareObject(Builder::setSchedulerConfig, SchedulerConfig.PARSER, SCHEDULER_CONFIG);
        PARSER.declareObject(Builder::setDataDescription, DataDescription.PARSER, DATA_DESCRIPTION);
        PARSER.declareObject(Builder::setModelSizeStats, ModelSizeStats.PARSER, MODEL_SIZE_STATS);
        PARSER.declareObjectArray(Builder::setTransforms, TransformConfig.PARSER, TRANSFORMS);
        PARSER.declareObject(Builder::setModelDebugConfig, ModelDebugConfig.PARSER, MODEL_DEBUG_CONFIG);
        PARSER.declareObject(Builder::setCounts, DataCounts.PARSER, COUNTS);
        PARSER.declareField(Builder::setIgnoreDowntime, (p, c) -> IgnoreDowntime.fromString(p.text()), IGNORE_DOWNTIME, ValueType.STRING);
        PARSER.declareLong(Builder::setTimeout, TIMEOUT);
        PARSER.declareLong(Builder::setRenormalizationWindowDays, RENORMALIZATION_WINDOW_DAYS);
        PARSER.declareLong(Builder::setBackgroundPersistInterval, BACKGROUND_PERSIST_INTERVAL);
        PARSER.declareLong(Builder::setResultsRetentionDays, RESULTS_RETENTION_DAYS);
        PARSER.declareLong(Builder::setModelSnapshotRetentionDays, MODEL_SNAPSHOT_RETENTION_DAYS);
        PARSER.declareField(Builder::setCustomSettings, (p, c) -> p.map(), CUSTOM_SETTINGS, ValueType.OBJECT);
        PARSER.declareDouble(Builder::setAverageBucketProcessingTimeMs, AVERAGE_BUCKET_PROCESSING_TIME);
        PARSER.declareStringOrNull(Builder::setModelSnapshotId, MODEL_SNAPSHOT_ID);
    }

    private final String jobId;
    private final String description;
    // NORELEASE: Use Jodatime instead
    private final Date createTime;
    private final Date finishedTime;
    private final Date lastDataTime;
    private final long timeout;
    private final AnalysisConfig analysisConfig;
    private final AnalysisLimits analysisLimits;
    private final SchedulerConfig schedulerConfig;
    private final DataDescription dataDescription;
    private final ModelSizeStats modelSizeStats;
    private final List<TransformConfig> transforms;
    private final ModelDebugConfig modelDebugConfig;
    private final DataCounts counts;
    private final IgnoreDowntime ignoreDowntime;
    private final Long renormalizationWindowDays;
    private final Long backgroundPersistInterval;
    private final Long modelSnapshotRetentionDays;
    private final Long resultsRetentionDays;
    private final Map<String, Object> customSettings;
    private final Double averageBucketProcessingTimeMs;
    private final String modelSnapshotId;

    public JobDetails(String jobId, String description, Date createTime, Date finishedTime,
            Date lastDataTime, long timeout, AnalysisConfig analysisConfig, AnalysisLimits analysisLimits, SchedulerConfig schedulerConfig,
            DataDescription dataDescription, ModelSizeStats modelSizeStats, List<TransformConfig> transforms,
            ModelDebugConfig modelDebugConfig, DataCounts counts, IgnoreDowntime ignoreDowntime, Long renormalizationWindowDays,
            Long backgroundPersistInterval, Long modelSnapshotRetentionDays, Long resultsRetentionDays, Map<String, Object> customSettings,
            Double averageBucketProcessingTimeMs, String modelSnapshotId) {
        this.jobId = jobId;
        this.description = description;
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
        this.modelSnapshotId = modelSnapshotId;
    }

    public JobDetails(StreamInput in) throws IOException {
        jobId = in.readString();
        description = in.readOptionalString();
        createTime = new Date(in.readVLong());
        finishedTime = in.readBoolean() ? new Date(in.readVLong()) : null;
        lastDataTime = in.readBoolean() ? new Date(in.readVLong()) : null;
        timeout = in.readVLong();
        analysisConfig = new AnalysisConfig(in);
        analysisLimits = in.readOptionalWriteable(AnalysisLimits::new);
        schedulerConfig = in.readOptionalWriteable(SchedulerConfig::new);
        dataDescription = in.readOptionalWriteable(DataDescription::new);
        modelSizeStats = in.readOptionalWriteable(ModelSizeStats::new);
        transforms = in.readList(TransformConfig::new);
        modelDebugConfig = in.readOptionalWriteable(ModelDebugConfig::new);
        counts = in.readOptionalWriteable(DataCounts::new);
        ignoreDowntime = in.readOptionalWriteable(IgnoreDowntime::fromStream);
        renormalizationWindowDays = in.readOptionalLong();
        backgroundPersistInterval = in.readOptionalLong();
        modelSnapshotRetentionDays = in.readOptionalLong();
        resultsRetentionDays = in.readOptionalLong();
        customSettings = in.readMap();
        averageBucketProcessingTimeMs = in.readOptionalDouble();
        modelSnapshotId = in.readOptionalString();
    }

    @Override
    public JobDetails readFrom(StreamInput in) throws IOException {
        return new JobDetails(in);
    }

    /**
     * Return the Job Id. This name is preferred when serialising to the REST
     * API.
     *
     * @return The job Id string
     */
    public String getId() {
        return jobId;
    }

    /**
     * Return the Job Id. This name is preferred when serialising to the data
     * store.
     *
     * @return The job Id string
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * The job description
     *
     * @return job description
     */
    public String getDescription() {
        return description;
    }

    /**
     * The Job creation time. This name is preferred when serialising to the
     * REST API.
     *
     * @return The date the job was created
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * The Job creation time. This name is preferred when serialising to the
     * data store.
     *
     * @return The date the job was created
     */
    public Date getAtTimestamp() {
        return createTime;
    }

    /**
     * The time the job was finished or <code>null</code> if not finished.
     *
     * @return The date the job was last retired or <code>null</code>
     */
    public Date getFinishedTime() {
        return finishedTime;
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

    /**
     * The job timeout setting in seconds. Jobs are retired if they do not
     * receive data for this period of time. The default is 600 seconds
     *
     * @return The timeout period in seconds
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * The analysis configuration object
     *
     * @return The AnalysisConfig
     */
    public AnalysisConfig getAnalysisConfig() {
        return analysisConfig;
    }

    /**
     * The analysis options object
     *
     * @return The AnalysisLimits
     */
    public AnalysisLimits getAnalysisLimits() {
        return analysisLimits;
    }

    public IgnoreDowntime getIgnoreDowntime() {
        return ignoreDowntime;
    }

    public SchedulerConfig getSchedulerConfig() {
        return schedulerConfig;
    }

    public ModelDebugConfig getModelDebugConfig() {
        return modelDebugConfig;
    }

    /**
     * The memory usage object
     *
     * @return The ModelSizeStats
     */
    public ModelSizeStats getModelSizeStats() {
        return modelSizeStats;
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

    public List<TransformConfig> getTransforms() {
        return transforms;
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
     * The duration of the renormalization window in days
     *
     * @return renormalization window in days
     */
    public Long getRenormalizationWindowDays() {
        return renormalizationWindowDays;
    }

    /**
     * The background persistence interval in seconds
     *
     * @return background persistence interval in seconds
     */
    public Long getBackgroundPersistInterval() {
        return backgroundPersistInterval;
    }

    public Long getModelSnapshotRetentionDays() {
        return modelSnapshotRetentionDays;
    }

    public Long getResultsRetentionDays() {
        return resultsRetentionDays;
    }

    public Map<String, Object> getCustomSettings() {
        return customSettings;
    }

    public Double getAverageBucketProcessingTimeMs() {
        return averageBucketProcessingTimeMs;
    }

    public String getModelSnapshotId() {
        return modelSnapshotId;
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
        out.writeOptionalWriteable(ignoreDowntime);
        out.writeOptionalLong(renormalizationWindowDays);
        out.writeOptionalLong(backgroundPersistInterval);
        out.writeOptionalLong(modelSnapshotRetentionDays);
        out.writeOptionalLong(resultsRetentionDays);
        out.writeMap(customSettings);
        out.writeOptionalDouble(averageBucketProcessingTimeMs);
        out.writeOptionalString(modelSnapshotId);
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
        builder.field(CREATE_TIME.getPreferredName(), createTime.getTime());
        if (finishedTime != null) {
            builder.field(FINISHED_TIME.getPreferredName(), finishedTime.getTime());
        }
        if (lastDataTime != null) {
            builder.field(LAST_DATA_TIME.getPreferredName(), lastDataTime.getTime());
        }
        builder.field(TIMEOUT.getPreferredName(), timeout);
        builder.field(ANALYSIS_CONFIG.getPreferredName(), analysisConfig, params);
        if (analysisLimits != null) {
            builder.field(ANALYSIS_LIMITS.getPreferredName(), analysisLimits, params);
        }
        if (schedulerConfig != null) {
            builder.field(SCHEDULER_CONFIG.getPreferredName(), schedulerConfig, params);
        }
        if (dataDescription != null) {
            builder.field(DATA_DESCRIPTION.getPreferredName(), dataDescription, params);
        }
        if (modelSizeStats != null) {
            builder.field(MODEL_SIZE_STATS.getPreferredName(), modelSizeStats, params);
        }
        if (transforms != null) {
            builder.field(TRANSFORMS.getPreferredName(), transforms);
        }
        if (modelDebugConfig != null) {
            builder.field(MODEL_DEBUG_CONFIG.getPreferredName(), modelDebugConfig, params);
        }
        if (counts != null) {
            builder.field(COUNTS.getPreferredName(), counts, params);
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
        if (modelSnapshotId != null){
            builder.field(MODEL_SNAPSHOT_ID.getPreferredName(), modelSnapshotId);
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
        return Objects.equals(this.jobId, that.jobId) && Objects.equals(this.description, that.description)
                && Objects.equals(this.createTime, that.createTime)
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
                && Objects.equals(this.customSettings, that.customSettings)
                && Objects.equals(this.modelSnapshotId, that.modelSnapshotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, description, createTime, finishedTime, lastDataTime, timeout, analysisConfig,
                analysisLimits, dataDescription, modelDebugConfig, modelSizeStats, transforms, counts, renormalizationWindowDays,
                backgroundPersistInterval, modelSnapshotRetentionDays, resultsRetentionDays, ignoreDowntime, customSettings,
                modelSnapshotId);
    }

    // Class alreadt extends from AbstractDiffable, so copied from ToXContentToBytes#toString()
    @SuppressWarnings("deprecation")
    @Override
    public final String toString() {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.prettyPrint();
            toXContent(builder, EMPTY_PARAMS);
            return builder.string();
        } catch (Exception e) {
            // So we have a stack trace logged somewhere
            return "{ \"error\" : \"" + org.elasticsearch.ExceptionsHelper.detailedMessage(e) + "\"}";
        }
    }

    public static class Builder {

        /**
         * Valid jobId characters. Note that '.' is allowed but not documented.
         */
        private static final Pattern VALID_JOB_ID_CHAR_PATTERN = Pattern.compile("[a-z0-9_\\-\\.]+");
        public static final int MAX_JOB_ID_LENGTH = 64;
        public static final long MIN_BACKGROUND_PERSIST_INTERVAL = 3600;
        public static final long DEFAULT_TIMEOUT = 600;
        private static final int MIN_SEQUENCE_LENGTH = 5;
        private static final int HOSTNAME_ID_SEPARATORS_LENGTH = 2;
        static final AtomicLong ID_SEQUENCE = new AtomicLong(); // package protected for testing
        private static final DateTimeFormatter ID_DATEFORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT);
        private static final String HOSTNAME;

        static {
            String hostname = System.getenv("HOSTNAME");
            if (hostname == null) {
                hostname = System.getenv("COMPUTERNAME");
            }
            HOSTNAME = hostname != null ? hostname.toLowerCase(Locale.ROOT) : null;
        }

        private String id;
        private String description;

        private AnalysisConfig analysisConfig = new AnalysisConfig();
        private AnalysisLimits analysisLimits;
        private SchedulerConfig schedulerConfig;
        private List<TransformConfig> transforms = new ArrayList<>();
        private ModelSizeStats modelSizeStats;
        private DataDescription dataDescription;
        private Date createTime;
        private Date finishedTime;
        private Date lastDataTime;
        private Long timeout = DEFAULT_TIMEOUT;
        private ModelDebugConfig modelDebugConfig;
        private Long renormalizationWindowDays;
        private Long backgroundPersistInterval;
        private Long modelSnapshotRetentionDays;
        private Long resultsRetentionDays;
        private DataCounts counts;
        private IgnoreDowntime ignoreDowntime;
        private Map<String, Object> customSettings;
        private Double averageBucketProcessingTimeMs;
        private String modelSnapshotId;

        public Builder() {
        }

        public Builder(String id) {
            this.id = id;
        }

        public Builder(JobDetails job) {
            this.id = job.getId();
            this.description = job.getDescription();
            this.analysisConfig = job.getAnalysisConfig();
            this.schedulerConfig = job.getSchedulerConfig();
            this.transforms = job.getTransforms();
            this.modelSizeStats = job.getModelSizeStats();
            this.dataDescription = job.getDataDescription();
            this.createTime = job.getCreateTime();
            this.finishedTime = job.getFinishedTime();
            this.lastDataTime = job.getLastDataTime();
            this.timeout = job.getTimeout();
            this.modelDebugConfig = job.getModelDebugConfig();
            this.renormalizationWindowDays = job.getRenormalizationWindowDays();
            this.backgroundPersistInterval = job.getBackgroundPersistInterval();
            this.resultsRetentionDays = job.getResultsRetentionDays();
            this.counts = job.getCounts();
            this.ignoreDowntime = job.getIgnoreDowntime();
            this.customSettings = job.getCustomSettings();
            this.averageBucketProcessingTimeMs = job.getAverageBucketProcessingTimeMs();
            this.modelSnapshotId = job.getModelSnapshotId();
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setCustomSettings(Map<String, Object> customSettings) {
            this.customSettings = customSettings;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setAnalysisConfig(AnalysisConfig config) {
            analysisConfig = config;
        }

        public void setAnalysisLimits(AnalysisLimits analysisLimits) {
            if (this.analysisLimits != null) {
                long oldMemoryLimit = this.analysisLimits.getModelMemoryLimit();
                long newMemoryLimit = analysisLimits.getModelMemoryLimit();
                if (newMemoryLimit < oldMemoryLimit) {
                    throw ExceptionsHelper.invalidRequestException(
                            Messages.getMessage(Messages.JOB_CONFIG_UPDATE_ANALYSIS_LIMITS_MODEL_MEMORY_LIMIT_CANNOT_BE_DECREASED,
                                    oldMemoryLimit, newMemoryLimit), ErrorCodes.INVALID_VALUE);
                }
            }
            this.analysisLimits = analysisLimits;
        }

        public void setSchedulerConfig(SchedulerConfig.Builder config) {
            schedulerConfig = config.build();
        }

        public void setTimeout(Long timeout) {
            this.timeout = timeout;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }

        public void setFinishedTime(Date finishedTime) {
            this.finishedTime = finishedTime;
        }

        public void setLastDataTime(Date lastDataTime) {
            this.lastDataTime = lastDataTime;
        }

        public void setTransforms(List<TransformConfig> transforms) {
            this.transforms = transforms;
        }

        public void setModelSizeStats(ModelSizeStats modelSizeStats) {
            this.modelSizeStats = modelSizeStats;
        }

        public void setDataDescription(DataDescription description) {
            dataDescription = description;
        }

        public void setModelDebugConfig(ModelDebugConfig modelDebugConfig) {
            this.modelDebugConfig = modelDebugConfig;
        }

        public void setBackgroundPersistInterval(Long backgroundPersistInterval) {
            this.backgroundPersistInterval = backgroundPersistInterval;
        }

        public void setRenormalizationWindowDays(Long renormalizationWindowDays) {
            this.renormalizationWindowDays = renormalizationWindowDays;
        }

        public void setModelSnapshotRetentionDays(Long modelSnapshotRetentionDays) {
            this.modelSnapshotRetentionDays = modelSnapshotRetentionDays;
        }

        public void setResultsRetentionDays(Long resultsRetentionDays) {
            this.resultsRetentionDays = resultsRetentionDays;
        }

        public void setIgnoreDowntime(IgnoreDowntime ignoreDowntime) {
            this.ignoreDowntime = ignoreDowntime;
        }

        public void setCounts(DataCounts counts) {
            this.counts = counts;
        }

        public void setAverageBucketProcessingTimeMs(Double averageBucketProcessingTimeMs) {
            this.averageBucketProcessingTimeMs = averageBucketProcessingTimeMs;
        }

        public void setModelSnapshotId(String modelSnapshotId) {
            this.modelSnapshotId = modelSnapshotId;
        }

        public JobDetails build() {
            return build(false);
        }

        public JobDetails build(boolean fromApi) {
            if (id.length() > MAX_JOB_ID_LENGTH) {
                throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_ID_TOO_LONG, MAX_JOB_ID_LENGTH),
                        ErrorCodes.JOB_ID_TOO_LONG);
            }
            if (!VALID_JOB_ID_CHAR_PATTERN.matcher(id).matches()) {
                throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_INVALID_JOBID_CHARS),
                        ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID);
            }
            if (analysisConfig == null) {
                throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_MISSING_ANALYSISCONFIG),
                        ErrorCodes.INCOMPLETE_CONFIGURATION);
            }
            // Move to AnalysisConfig.Builder once created:
            AnalysisConfigVerifier.verify(analysisConfig);

            if (schedulerConfig != null) {
                if (analysisConfig.getBucketSpan() == null) {
                    throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_REQUIRES_BUCKET_SPAN),
                            ErrorCodes.SCHEDULER_REQUIRES_BUCKET_SPAN);
                }
                if (schedulerConfig.getDataSource() == SchedulerConfig.DataSource.ELASTICSEARCH) {
                    if (analysisConfig.getLatency() != null && analysisConfig.getLatency() > 0) {
                        throw ExceptionsHelper.invalidRequestException(
                                Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_ELASTICSEARCH_DOES_NOT_SUPPORT_LATENCY),
                                ErrorCodes.SCHEDULER_ELASTICSEARCH_DOES_NOT_SUPPORT_LATENCY);
                    }
                    if (schedulerConfig.getAggregationsOrAggs() != null
                            && !SchedulerConfig.DOC_COUNT.equals(analysisConfig.getSummaryCountFieldName())) {
                        throw ExceptionsHelper.invalidRequestException(
                                Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD,
                                        SchedulerConfig.DataSource.ELASTICSEARCH.toString(), SchedulerConfig.DOC_COUNT),
                                ErrorCodes.SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD);
                    }
                    if (dataDescription == null || dataDescription.getFormat() != DataDescription.DataFormat.ELASTICSEARCH) {
                        throw ExceptionsHelper.invalidRequestException(
                                Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_ELASTICSEARCH_REQUIRES_DATAFORMAT_ELASTICSEARCH),
                                ErrorCodes.SCHEDULER_ELASTICSEARCH_REQUIRES_DATAFORMAT_ELASTICSEARCH);
                    }
                }
            }

            if (dataDescription != null) {
                // Move to DataDescription.Builder once created:
                DataDescriptionVerifier.verify(dataDescription);
            }
            if (transforms != null && transforms.isEmpty() == false) {
                TransformConfigsVerifier.verify(transforms);
                checkTransformOutputIsUsed();
            } else {
                if (dataDescription != null && dataDescription.getFormat() == DataDescription.DataFormat.SINGLE_LINE) {
                    String msg = Messages.getMessage(
                            Messages.JOB_CONFIG_DATAFORMAT_REQUIRES_TRANSFORM,
                            DataDescription.DataFormat.SINGLE_LINE);

                    throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.DATA_FORMAT_IS_SINGLE_LINE_BUT_NO_TRANSFORMS);
                }
            }


            checkValueNotLessThan(0, "timeout", timeout);
            checkValueNotLessThan(0, "renormalizationWindowDays", renormalizationWindowDays);
            checkValueNotLessThan(MIN_BACKGROUND_PERSIST_INTERVAL, "backgroundPersistInterval", backgroundPersistInterval);
            checkValueNotLessThan(0, "modelSnapshotRetentionDays", modelSnapshotRetentionDays);
            checkValueNotLessThan(0, "resultsRetentionDays", resultsRetentionDays);

            String id;
            Date createTime;
            Date finishedTime;
            Date lastDataTime;
            DataCounts counts;
            ModelSizeStats modelSizeStats;
            Double averageBucketProcessingTimeMs;
            String modelSnapshotId;
            if (fromApi) {
                id = this.id == null ? generateJobId(HOSTNAME): this.id;
                createTime = this.createTime == null ? new Date() : this.createTime;
                finishedTime = null;
                lastDataTime = null;
                counts = new DataCounts();
                modelSizeStats = null;
                averageBucketProcessingTimeMs = null;
                modelSnapshotId = null;
            } else {
                id = this.id;
                createTime = this.createTime;
                finishedTime = this.finishedTime;
                lastDataTime = this.lastDataTime;
                counts = this.counts;
                modelSizeStats = this.modelSizeStats;
                averageBucketProcessingTimeMs = this.averageBucketProcessingTimeMs;
                modelSnapshotId = this.modelSnapshotId;
            }
            return new JobDetails(
                    id, description, createTime, finishedTime, lastDataTime, timeout, analysisConfig, analysisLimits,
                    schedulerConfig, dataDescription, modelSizeStats, transforms, modelDebugConfig, counts,
                    ignoreDowntime, renormalizationWindowDays, backgroundPersistInterval, modelSnapshotRetentionDays,
                    resultsRetentionDays, customSettings, averageBucketProcessingTimeMs, modelSnapshotId
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
            String dateStr = ID_DATEFORMAT.format(LocalDateTime.now(ZoneId.systemDefault()));
            long sequence = ID_SEQUENCE.incrementAndGet();
            if (hostName == null) {
                return String.format(Locale.ROOT, "%s-%05d", dateStr, sequence);
            } else {
                int formattedSequenceLen = Math.max(String.valueOf(sequence).length(), MIN_SEQUENCE_LENGTH);
                int hostnameMaxLen = MAX_JOB_ID_LENGTH - dateStr.length() - formattedSequenceLen - HOSTNAME_ID_SEPARATORS_LENGTH;
                String trimmedHostName = hostName.substring(0, Math.min(hostName.length(), hostnameMaxLen));
                return String.format(Locale.ROOT, "%s-%s-%05d", dateStr, trimmedHostName, sequence);
            }
        }

        private static void checkValueNotLessThan(long minVal, String name, Long value) {
            if (value != null && value < minVal) {
                throw ExceptionsHelper.invalidRequestException(
                        Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW, name, minVal, value), ErrorCodes.INVALID_VALUE);
            }
        }

        /**
         * Transform outputs should be used in either the date field,
         * as an analysis field or input to another transform
         */
        private boolean checkTransformOutputIsUsed() {
            Set<String> usedFields = new TransformConfigs(transforms).inputFieldNames();
            usedFields.addAll(analysisConfig.analysisFields());
            String summaryCountFieldName = analysisConfig.getSummaryCountFieldName();
            boolean isSummarised = !Strings.isNullOrEmpty(summaryCountFieldName);
            if (isSummarised) {
                usedFields.remove(summaryCountFieldName);
            }

            String timeField = dataDescription == null ? DataDescription.DEFAULT_TIME_FIELD : dataDescription.getTimeField();
            usedFields.add(timeField);

            for (TransformConfig tc : transforms) {
                // if the type has no default outputs it doesn't need an output
                boolean usesAnOutput = tc.type().defaultOutputNames().isEmpty()
                        || tc.getOutputs().stream().anyMatch(outputName -> usedFields.contains(outputName));

                if (isSummarised && tc.getOutputs().contains(summaryCountFieldName)) {
                    String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_DUPLICATED_OUTPUT_NAME, tc.type().prettyName());
                    throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.DUPLICATED_TRANSFORM_OUTPUT_NAME);
                }

                if (!usesAnOutput) {
                    String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_OUTPUTS_UNUSED,
                            tc.type().prettyName());
                    throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.TRANSFORM_OUTPUTS_UNUSED);
                }
            }

            return false;
        }

    }

}
