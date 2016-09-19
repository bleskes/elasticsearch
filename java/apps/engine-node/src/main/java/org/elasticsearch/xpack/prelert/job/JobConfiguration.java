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

import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;

import java.util.List;
import java.util.Map;


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
    private String iD;
    private String description;

    private AnalysisConfig analysisConfig;
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

    public JobConfiguration(AnalysisConfig analysisConfig) {
        this();
        this.analysisConfig = analysisConfig;
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

    public void setSchedulerConfig(SchedulerConfig config) {
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
}
