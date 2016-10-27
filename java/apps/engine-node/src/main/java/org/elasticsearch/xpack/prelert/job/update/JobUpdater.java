package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;

public class JobUpdater {

    private static final Logger LOGGER = Loggers.getLogger(JobUpdater.class);

    private static final String ANALYSIS_LIMITS_KEY = "analysisLimits";
    private static final String BACKGROUND_PERSIST_INTERVAL_KEY = "backgroundPersistInterval";
    private static final String CATEGORIZATION_FILTERS_KEY = "categorizationFilters";
    private static final String CUSTOM_SETTINGS = "customSettings";
    private static final String DETECTORS_KEY = "detectors";
    private static final String JOB_DESCRIPTION_KEY = "description";
    private static final String IGNORE_DOWNTIME_KEY = "ignoreDowntime";
    private static final String MODEL_DEBUG_CONFIG_KEY = "modelDebugConfig";
    private static final String RENORMALIZATION_WINDOW_DAYS_KEY = "renormalizationWindowDays";
    private static final String MODEL_SNAPSHOT_RETENTION_DAYS_KEY = "modelSnapshotRetentionDays";
    private static final String RESULTS_RETENTION_DAYS_KEY = "resultsRetentionDays";
    private static final String SCHEDULER_CONFIG_KEY = "schedulerConfig";

    private final JobDetails job;
    private final StringWriter configWriter;

    public JobUpdater(JobDetails job) {
        // NORELEASE The updaters should be passed in a job builder after job becomes immutable
        this.job = Objects.requireNonNull(job);
        configWriter = new StringWriter();
    }

    /**
     * Performs a job update according to the given JSON input. The update is done in 2 steps:
     * <ol>
     *   <li> Prepare update (performs validations)
     *   <li> Commit update
     * </ol>
     * If there are invalid updates, none of the updates is applied.
     *
     * @param updateJson the JSON input that contains the requested updates
     * @return a {@code Response}
     * @throws JobException If the update fails (e.g. the job is unavailable for updating or the process fails during update)
     */
    public void update(String updateJson) throws JobException {
        JsonNode node = parse(updateJson);
        if (!node.isObject() || node.size() == 0) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_UPDATE_REQUIRES_NON_EMPTY_OBJECT),
                    ErrorCodes.JOB_CONFIG_PARSE_ERROR);
        }

        Map<String, Supplier<AbstractUpdater>> updaterPerKey = createUpdaterPerKeyMap();
        List<String> keysToUpdate = new ArrayList<>();
        Iterator<Entry<String, JsonNode>> fieldsIterator = node.fields();
        while (fieldsIterator.hasNext()) {
            Entry<String, JsonNode> keyValue = fieldsIterator.next();
            LOGGER.debug("Updating job config for key: " + keyValue.getKey());
            AbstractUpdater updater = createKeyValueUpdater(updaterPerKey, keyValue.getKey());
            keysToUpdate.add(keyValue.getKey());
            updater.update(keyValue.getValue());
        }

        writeUpdateConfigMessage();

        // NORELEASE Figure how to audit this
        // jobManager.audit(job.getId()).info(Messages.getMessage(Messages.JOB_AUDIT_UPDATED, keysToUpdate));
    }

    private JsonNode parse(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(json);
        } catch (IOException e) {
            throw ExceptionsHelper.parseException(Messages.getMessage(Messages.JOB_CONFIG_UPDATE_PARSE_ERROR),
                    ErrorCodes.JOB_CONFIG_PARSE_ERROR);
        }
    }

    private Map<String, Supplier<AbstractUpdater>> createUpdaterPerKeyMap() {
        Map<String, Supplier<AbstractUpdater>> map = new HashMap<>();
        map.put(ANALYSIS_LIMITS_KEY, () -> new AnalysisLimitsUpdater(job, ANALYSIS_LIMITS_KEY));
        map.put(BACKGROUND_PERSIST_INTERVAL_KEY, () -> new BackgroundPersistIntervalUpdater(job, BACKGROUND_PERSIST_INTERVAL_KEY));
        map.put(CATEGORIZATION_FILTERS_KEY, () -> new CategorizationFiltersUpdater(job, CATEGORIZATION_FILTERS_KEY));
        map.put(CUSTOM_SETTINGS, () -> new CustomSettingsUpdater(job, CUSTOM_SETTINGS));
        map.put(DETECTORS_KEY, () -> new DetectorsUpdater(job, DETECTORS_KEY, configWriter));
        map.put(JOB_DESCRIPTION_KEY, () -> new JobDescriptionUpdater(job, JOB_DESCRIPTION_KEY));
        map.put(IGNORE_DOWNTIME_KEY, () -> new IgnoreDowntimeUpdater(job, IGNORE_DOWNTIME_KEY));
        map.put(MODEL_DEBUG_CONFIG_KEY, () -> new ModelDebugConfigUpdater(job, MODEL_DEBUG_CONFIG_KEY, configWriter));
        map.put(RENORMALIZATION_WINDOW_DAYS_KEY, () -> new RenormalizationWindowDaysUpdater(job, RENORMALIZATION_WINDOW_DAYS_KEY));
        map.put(MODEL_SNAPSHOT_RETENTION_DAYS_KEY, () -> new ModelSnapshotRetentionDaysUpdater(job, MODEL_SNAPSHOT_RETENTION_DAYS_KEY));
        map.put(RESULTS_RETENTION_DAYS_KEY, () -> new ResultsRetentionDaysUpdater(job, RESULTS_RETENTION_DAYS_KEY));
        map.put(SCHEDULER_CONFIG_KEY, () -> new SchedulerConfigUpdater(job, SCHEDULER_CONFIG_KEY));
        return map;
    }

    private static AbstractUpdater createKeyValueUpdater(Map<String, Supplier<AbstractUpdater>> updaterPerKey, String key) {
        if (updaterPerKey.containsKey(key) == false) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_UPDATE_INVALID_KEY, key),
                    ErrorCodes.INVALID_UPDATE_KEY);
        }
        return updaterPerKey.get(key).get();
    }

    private void writeUpdateConfigMessage() {
        String config = configWriter.toString();
        if (!config.isEmpty()) {
            // NORELEASE Write update config using new interface
//            jobManager.writeUpdateConfigMessage(jobId, config);
        }
    }
}
