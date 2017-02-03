/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.job.config;

import org.elasticsearch.cluster.AbstractDiffable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser.Token;
import org.elasticsearch.xpack.ml.job.messages.Messages;
import org.elasticsearch.xpack.ml.utils.MlStrings;
import org.elasticsearch.xpack.ml.utils.time.TimeUtils;

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
 * to the time the object was constructed, state is set to
 * {@link JobState#OPENING} and the finished time and last data time fields are
 * {@code null} until the job has seen some data or it is finished respectively.
 * If the job was created to read data from a list of files FileUrls will be a
 * non-empty list else the expects data to be streamed to it.
 */
public class Job extends AbstractDiffable<Job> implements Writeable, ToXContent {

    public static final String TYPE = "job";

    /*
     * Field names used in serialization
     */
    public static final ParseField ID = new ParseField("job_id");
    public static final ParseField ANALYSIS_CONFIG = new ParseField("analysis_config");
    public static final ParseField ANALYSIS_LIMITS = new ParseField("analysis_limits");
    public static final ParseField CREATE_TIME = new ParseField("create_time");
    public static final ParseField CUSTOM_SETTINGS = new ParseField("custom_settings");
    public static final ParseField DATA_DESCRIPTION = new ParseField("data_description");
    public static final ParseField DESCRIPTION = new ParseField("description");
    public static final ParseField FINISHED_TIME = new ParseField("finished_time");
    public static final ParseField IGNORE_DOWNTIME = new ParseField("ignore_downtime");
    public static final ParseField LAST_DATA_TIME = new ParseField("last_data_time");
    public static final ParseField MODEL_DEBUG_CONFIG = new ParseField("model_debug_config");
    public static final ParseField RENORMALIZATION_WINDOW_DAYS = new ParseField("renormalization_window_days");
    public static final ParseField BACKGROUND_PERSIST_INTERVAL = new ParseField("background_persist_interval");
    public static final ParseField MODEL_SNAPSHOT_RETENTION_DAYS = new ParseField("model_snapshot_retention_days");
    public static final ParseField RESULTS_RETENTION_DAYS = new ParseField("results_retention_days");
    public static final ParseField TIMEOUT = new ParseField("timeout");
    public static final ParseField MODEL_SNAPSHOT_ID = new ParseField("model_snapshot_id");
    public static final ParseField INDEX_NAME = new ParseField("index_name");

    // Used for QueryPage
    public static final ParseField RESULTS_FIELD = new ParseField("jobs");

    public static final String ALL = "_all";

    public static final ObjectParser<Builder, Void> PARSER = new ObjectParser<>("job_details", Builder::new);

    public static final int MAX_JOB_ID_LENGTH = 64;
    public static final long MIN_BACKGROUND_PERSIST_INTERVAL = 3600;

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
        PARSER.declareObject(Builder::setDataDescription, DataDescription.PARSER, DATA_DESCRIPTION);
        PARSER.declareObject(Builder::setModelDebugConfig, ModelDebugConfig.PARSER, MODEL_DEBUG_CONFIG);
        PARSER.declareField(Builder::setIgnoreDowntime, (p, c) -> IgnoreDowntime.fromString(p.text()), IGNORE_DOWNTIME, ValueType.STRING);
        PARSER.declareLong(Builder::setTimeout, TIMEOUT);
        PARSER.declareLong(Builder::setRenormalizationWindowDays, RENORMALIZATION_WINDOW_DAYS);
        PARSER.declareLong(Builder::setBackgroundPersistInterval, BACKGROUND_PERSIST_INTERVAL);
        PARSER.declareLong(Builder::setResultsRetentionDays, RESULTS_RETENTION_DAYS);
        PARSER.declareLong(Builder::setModelSnapshotRetentionDays, MODEL_SNAPSHOT_RETENTION_DAYS);
        PARSER.declareField(Builder::setCustomSettings, (p, c) -> p.map(), CUSTOM_SETTINGS, ValueType.OBJECT);
        PARSER.declareStringOrNull(Builder::setModelSnapshotId, MODEL_SNAPSHOT_ID);
        PARSER.declareString(Builder::setIndexName, INDEX_NAME);
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
    private final DataDescription dataDescription;
    private final ModelDebugConfig modelDebugConfig;
    private final IgnoreDowntime ignoreDowntime;
    private final Long renormalizationWindowDays;
    private final Long backgroundPersistInterval;
    private final Long modelSnapshotRetentionDays;
    private final Long resultsRetentionDays;
    private final Map<String, Object> customSettings;
    private final String modelSnapshotId;
    private final String indexName;

    public Job(String jobId, String description, Date createTime, Date finishedTime, Date lastDataTime, long timeout,
               AnalysisConfig analysisConfig, AnalysisLimits analysisLimits,  DataDescription dataDescription,
               ModelDebugConfig modelDebugConfig, IgnoreDowntime ignoreDowntime,
               Long renormalizationWindowDays, Long backgroundPersistInterval, Long modelSnapshotRetentionDays, Long resultsRetentionDays,
               Map<String, Object> customSettings, String modelSnapshotId, String indexName) {

        if (analysisConfig == null) {
            throw new IllegalArgumentException(Messages.getMessage(Messages.JOB_CONFIG_MISSING_ANALYSISCONFIG));
        }

        checkValueNotLessThan(0, "timeout", timeout);
        checkValueNotLessThan(0, "renormalizationWindowDays", renormalizationWindowDays);
        checkValueNotLessThan(MIN_BACKGROUND_PERSIST_INTERVAL, "backgroundPersistInterval", backgroundPersistInterval);
        checkValueNotLessThan(0, "modelSnapshotRetentionDays", modelSnapshotRetentionDays);
        checkValueNotLessThan(0, "resultsRetentionDays", resultsRetentionDays);

        if (!MlStrings.isValidId(jobId)) {
            throw new IllegalArgumentException(Messages.getMessage(Messages.INVALID_ID, ID.getPreferredName(), jobId));
        }
        if (jobId.length() > MAX_JOB_ID_LENGTH) {
            throw new IllegalArgumentException(Messages.getMessage(Messages.JOB_CONFIG_ID_TOO_LONG, MAX_JOB_ID_LENGTH));
        }

        if (Strings.isNullOrEmpty(indexName)) {
            indexName = jobId;
        } else if (!MlStrings.isValidId(indexName)) {
            throw new IllegalArgumentException(Messages.getMessage(Messages.INVALID_ID, INDEX_NAME.getPreferredName()));
        }

        this.jobId = jobId;
        this.description = description;
        this.createTime = createTime;
        this.finishedTime = finishedTime;
        this.lastDataTime = lastDataTime;
        this.timeout = timeout;
        this.analysisConfig = analysisConfig;
        this.analysisLimits = analysisLimits;
        this.dataDescription = dataDescription;
        this.modelDebugConfig = modelDebugConfig;
        this.ignoreDowntime = ignoreDowntime;
        this.renormalizationWindowDays = renormalizationWindowDays;
        this.backgroundPersistInterval = backgroundPersistInterval;
        this.modelSnapshotRetentionDays = modelSnapshotRetentionDays;
        this.resultsRetentionDays = resultsRetentionDays;
        this.customSettings = customSettings;
        this.modelSnapshotId = modelSnapshotId;
        this.indexName = indexName;
    }

    public Job(StreamInput in) throws IOException {
        jobId = in.readString();
        description = in.readOptionalString();
        createTime = new Date(in.readVLong());
        finishedTime = in.readBoolean() ? new Date(in.readVLong()) : null;
        lastDataTime = in.readBoolean() ? new Date(in.readVLong()) : null;
        timeout = in.readVLong();
        analysisConfig = new AnalysisConfig(in);
        analysisLimits = in.readOptionalWriteable(AnalysisLimits::new);
        dataDescription = in.readOptionalWriteable(DataDescription::new);
        modelDebugConfig = in.readOptionalWriteable(ModelDebugConfig::new);
        ignoreDowntime = in.readOptionalWriteable(IgnoreDowntime::fromStream);
        renormalizationWindowDays = in.readOptionalLong();
        backgroundPersistInterval = in.readOptionalLong();
        modelSnapshotRetentionDays = in.readOptionalLong();
        resultsRetentionDays = in.readOptionalLong();
        customSettings = in.readMap();
        modelSnapshotId = in.readOptionalString();
        indexName = in.readString();
    }

    /**
     * Return the Job Id.
     *
     * @return The job Id string
     */
    public String getId() {
        return jobId;
    }

    /**
     * The name of the index storing the job's results and state.
     * This defaults to {@link #getId()} if a specific index name is not set.
     * @return The job's index name
     */
    public String getIndexName() {
        return indexName;
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

    public ModelDebugConfig getModelDebugConfig() {
        return modelDebugConfig;
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

    public String getModelSnapshotId() {
        return modelSnapshotId;
    }

    /**
     * Get a list of all input data fields mentioned in the job configuration,
     * namely analysis fields and the time field.
     *
     * @return the list of fields - never <code>null</code>
     */
    public List<String> allFields() {
        Set<String> allFields = new TreeSet<>();

        // analysis fields
        if (analysisConfig != null) {
            allFields.addAll(analysisConfig.analysisFields());
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
        out.writeOptionalWriteable(dataDescription);
        out.writeOptionalWriteable(modelDebugConfig);
        out.writeOptionalWriteable(ignoreDowntime);
        out.writeOptionalLong(renormalizationWindowDays);
        out.writeOptionalLong(backgroundPersistInterval);
        out.writeOptionalLong(modelSnapshotRetentionDays);
        out.writeOptionalLong(resultsRetentionDays);
        out.writeMap(customSettings);
        out.writeOptionalString(modelSnapshotId);
        out.writeString(indexName);
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
        if (description != null) {
            builder.field(DESCRIPTION.getPreferredName(), description);
        }
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
        if (dataDescription != null) {
            builder.field(DATA_DESCRIPTION.getPreferredName(), dataDescription, params);
        }
        if (modelDebugConfig != null) {
            builder.field(MODEL_DEBUG_CONFIG.getPreferredName(), modelDebugConfig, params);
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
        if (modelSnapshotId != null) {
            builder.field(MODEL_SNAPSHOT_ID.getPreferredName(), modelSnapshotId);
        }
        builder.field(INDEX_NAME.getPreferredName(), indexName);
        return builder;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof Job == false) {
            return false;
        }

        Job that = (Job) other;
        return Objects.equals(this.jobId, that.jobId) && Objects.equals(this.description, that.description)
                && Objects.equals(this.createTime, that.createTime)
                && Objects.equals(this.finishedTime, that.finishedTime)
                && Objects.equals(this.lastDataTime, that.lastDataTime)
                && (this.timeout == that.timeout)
                && Objects.equals(this.analysisConfig, that.analysisConfig)
                && Objects.equals(this.analysisLimits, that.analysisLimits) && Objects.equals(this.dataDescription, that.dataDescription)
                && Objects.equals(this.modelDebugConfig, that.modelDebugConfig)
                && Objects.equals(this.ignoreDowntime, that.ignoreDowntime)
                && Objects.equals(this.renormalizationWindowDays, that.renormalizationWindowDays)
                && Objects.equals(this.backgroundPersistInterval, that.backgroundPersistInterval)
                && Objects.equals(this.modelSnapshotRetentionDays, that.modelSnapshotRetentionDays)
                && Objects.equals(this.resultsRetentionDays, that.resultsRetentionDays)
                && Objects.equals(this.customSettings, that.customSettings)
                && Objects.equals(this.modelSnapshotId, that.modelSnapshotId)
                && Objects.equals(this.indexName, that.indexName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, description, createTime, finishedTime, lastDataTime, timeout, analysisConfig,
                analysisLimits, dataDescription, modelDebugConfig, renormalizationWindowDays,
                backgroundPersistInterval, modelSnapshotRetentionDays, resultsRetentionDays, ignoreDowntime, customSettings,
                modelSnapshotId, indexName);
    }

    // Class alreadt extends from AbstractDiffable, so copied from ToXContentToBytes#toString()
    @Override
    public final String toString() {
        return Strings.toString(this);
    }

    private static void checkValueNotLessThan(long minVal, String name, Long value) {
        if (value != null && value < minVal) {
            throw new IllegalArgumentException(Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW, name, minVal, value));
        }
    }

    public static class Builder {

        public static final long DEFAULT_TIMEOUT = 600;

        private String id;
        private String description;

        private AnalysisConfig analysisConfig;
        private AnalysisLimits analysisLimits;
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
        private IgnoreDowntime ignoreDowntime;
        private Map<String, Object> customSettings;
        private String modelSnapshotId;
        private String indexName;

        public Builder() {
        }

        public Builder(String id) {
            this.id = id;
        }

        public Builder(Job job) {
            this.id = job.getId();
            this.description = job.getDescription();
            this.analysisConfig = job.getAnalysisConfig();
            this.dataDescription = job.getDataDescription();
            this.createTime = job.getCreateTime();
            this.finishedTime = job.getFinishedTime();
            this.lastDataTime = job.getLastDataTime();
            this.timeout = job.getTimeout();
            this.modelDebugConfig = job.getModelDebugConfig();
            this.renormalizationWindowDays = job.getRenormalizationWindowDays();
            this.backgroundPersistInterval = job.getBackgroundPersistInterval();
            this.resultsRetentionDays = job.getResultsRetentionDays();
            this.ignoreDowntime = job.getIgnoreDowntime();
            this.customSettings = job.getCustomSettings();
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

        public void setAnalysisConfig(AnalysisConfig.Builder configBuilder) {
            analysisConfig = configBuilder.build();
        }

        public void setAnalysisLimits(AnalysisLimits analysisLimits) {
            if (this.analysisLimits != null) {
                long oldMemoryLimit = this.analysisLimits.getModelMemoryLimit();
                long newMemoryLimit = analysisLimits.getModelMemoryLimit();
                if (newMemoryLimit < oldMemoryLimit) {
                    throw new IllegalArgumentException(
                            Messages.getMessage(Messages.JOB_CONFIG_UPDATE_ANALYSIS_LIMITS_MODEL_MEMORY_LIMIT_CANNOT_BE_DECREASED,
                                    oldMemoryLimit, newMemoryLimit));
                }
            }
            this.analysisLimits = analysisLimits;
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

        /**
         * Set the wall clock time of the last data upload
         * @param lastDataTime Wall clock time
         */
        public void setLastDataTime(Date lastDataTime) {
            this.lastDataTime = lastDataTime;
        }

        public void setDataDescription(DataDescription.Builder description) {
            dataDescription = description.build();
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

        public void setModelSnapshotId(String modelSnapshotId) {
            this.modelSnapshotId = modelSnapshotId;
        }

        public void setIndexName(String indexName) {
            this.indexName = indexName;
        }

        public Job build() {
            return build(false, null);
        }

        public Job build(boolean fromApi, String urlJobId) {

            Date createTime;
            Date finishedTime;
            Date lastDataTime;
            String modelSnapshotId;
            if (fromApi) {
                if (id == null) {
                    id = urlJobId;
                } else if (!id.equals(urlJobId)) {
                    throw new IllegalArgumentException(Messages.getMessage(Messages.INCONSISTENT_ID, ID.getPreferredName(), id, urlJobId));
                }
                createTime = this.createTime == null ? new Date() : this.createTime;
                finishedTime = null;
                lastDataTime = null;
                modelSnapshotId = null;
            } else {
                createTime = this.createTime;
                finishedTime = this.finishedTime;
                lastDataTime = this.lastDataTime;
                modelSnapshotId = this.modelSnapshotId;
            }

            return new Job(
                    id, description, createTime, finishedTime, lastDataTime, timeout, analysisConfig, analysisLimits,
                    dataDescription, modelDebugConfig, ignoreDowntime, renormalizationWindowDays,
                    backgroundPersistInterval, modelSnapshotRetentionDays, resultsRetentionDays, customSettings, modelSnapshotId,
                    indexName
            );
        }
    }
}
