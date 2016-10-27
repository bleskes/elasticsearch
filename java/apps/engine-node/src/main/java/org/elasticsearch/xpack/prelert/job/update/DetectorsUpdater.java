package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.config.DefaultDetectorDescription;
import org.elasticsearch.xpack.prelert.job.detectionrules.DetectionRule;
import org.elasticsearch.xpack.prelert.job.detectionrules.verification.DetectionRuleVerifier;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DetectorsUpdater extends AbstractUpdater {

    private static final String DETECTOR_INDEX = "index";
    private static final String DESCRIPTION = "description";
    private static final String DETECTOR_RULES = "detectorRules";
    private static final Set<String> REQUIRED_PARAMS = new LinkedHashSet<>(Arrays.asList(DETECTOR_INDEX));
    private static final Set<String> OPTIONAL_PARAMS = new LinkedHashSet<>(Arrays.asList(DESCRIPTION, DETECTOR_RULES));

    private final StringWriter configWriter;
    private List<UpdateParams> updates;

    public DetectorsUpdater(JobDetails job, String updateKey, StringWriter configWriter) {
        super(job, updateKey);
        updates = new ArrayList<>();
        this.configWriter = configWriter;
    }

    @Override
    void update(JsonNode node) {
        JobDetails job = job();
        parseUpdate(node);
        int detectorsCount = job.getAnalysisConfig().getDetectors().size();
        for (UpdateParams update : updates) {
            validateDetectorIndex(update, detectorsCount);
            fillDefaultDescriptionIfSetEmpty(job, update);
            validateDetectorRules(job, update);
            update.commit();
        }
    }

    private void parseUpdate(JsonNode node) {
        if (!node.isArray()) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_UPDATE_DETECTORS_INVALID);
            throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.INVALID_VALUE);
        }
        Iterator<JsonNode> iterator = node.iterator();
        while (iterator.hasNext()) {
            parseArrayElement(iterator.next());
        }
    }

    private void parseArrayElement(JsonNode node) {
        Map<String, Object> updateParams = convertToMap(node, () -> createInvalidParamsMsg());
        Set<String> updateKeys = updateParams.keySet();
        if (updateKeys.size() < 2 || !updateKeys.containsAll(REQUIRED_PARAMS) || updateKeys.stream().anyMatch(s -> !isValidParam(s))) {
            throw ExceptionsHelper.invalidRequestException(createInvalidParamsMsg(), ErrorCodes.INVALID_VALUE);
        }
        parseUpdateParams(updateParams);
    }

    private static String createInvalidParamsMsg() {
        return Messages.getMessage(Messages.JOB_CONFIG_UPDATE_DETECTORS_MISSING_PARAMS, REQUIRED_PARAMS, OPTIONAL_PARAMS);
    }

    private static boolean isValidParam(String key) {
        return REQUIRED_PARAMS.contains(key) || OPTIONAL_PARAMS.contains(key);
    }

    private void parseUpdateParams(Map<String, Object> updateParams) {
        UpdateParams parsedParams = new UpdateParams(parseDetectorIndex(updateParams.get(DETECTOR_INDEX)));

        if (updateParams.containsKey(DESCRIPTION)) {
            parsedParams.detectorDescription = parseDescription(updateParams.get(DESCRIPTION));
        }

        if (updateParams.containsKey(DETECTOR_RULES)) {
            parsedParams.detectorRules = parseDetectorRules(updateParams.get(DETECTOR_RULES));
        }

        updates.add(parsedParams);
    }

    private static int parseDetectorIndex(Object detectorIndex) {
        if (!(detectorIndex instanceof Integer)) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_UPDATE_DETECTORS_DETECTOR_INDEX_SHOULD_BE_INTEGER, detectorIndex);
            throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.INVALID_VALUE);
        }
        return (int) detectorIndex;
    }

    private static String parseDescription(Object description) {
        if (!(description instanceof String)) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_UPDATE_DETECTORS_DESCRIPTION_SHOULD_BE_STRING, description);
            throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.INVALID_VALUE);
        }
        return (String) description;
    }

    private static List<DetectionRule> parseDetectorRules(Object rules) {
        try {
            return JSON_MAPPER.convertValue(rules, new TypeReference<List<DetectionRule>>() {});
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw ExceptionsHelper.parseException(Messages.getMessage(Messages.JOB_CONFIG_UPDATE_DETECTOR_RULES_PARSE_ERROR),
                    ErrorCodes.INVALID_VALUE);
        }
    }

    private void validateDetectorIndex(UpdateParams update, int detectorsCount) {
        if (update.detectorIndex < 0 || update.detectorIndex >= detectorsCount) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_UPDATE_DETECTORS_INVALID_DETECTOR_INDEX, 0, detectorsCount - 1,
                    update.detectorIndex);
            throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.INVALID_VALUE);
        }
    }

    private void fillDefaultDescriptionIfSetEmpty(JobDetails job, UpdateParams update) {
        if (update.detectorDescription != null && update.detectorDescription.isEmpty()) {
            update.detectorDescription = DefaultDetectorDescription.of(job.getAnalysisConfig().getDetectors().get(update.detectorIndex));
        }
    }

    private static void validateDetectorRules(JobDetails job, UpdateParams update) {
        if (update.detectorRules == null || update.detectorRules.isEmpty()) {
            return;
        }
        Detector detector = job.getAnalysisConfig().getDetectors().get(update.detectorIndex);
        for (DetectionRule rule : update.detectorRules) {
            DetectionRuleVerifier.verify(rule, detector);
        }
    }

    private class UpdateParams {
        final int detectorIndex;
        String detectorDescription;
        List<DetectionRule> detectorRules;

        public UpdateParams(int detectorIndex) {
            this.detectorIndex = detectorIndex;
        }

        void commit() {
            commitDescription();
            commitRules();
        }

        private void commitDescription() {
            if (detectorDescription != null) {
                job().getAnalysisConfig().getDetectors().get(detectorIndex).setDetectorDescription(detectorDescription);
            }
        }

        private void commitRules() {
            if (detectorRules != null) {
                job().getAnalysisConfig().getDetectors().get(detectorIndex).setDetectorRules(detectorRules);
                writeRules();
            }
        }

        private void writeRules() {
            String rulesJson = "";
            try {
                rulesJson = JSON_MAPPER.writeValueAsString(detectorRules);
            } catch (JsonProcessingException e) {
                throw ExceptionsHelper.invalidRequestException("Failed to write detectorRules", ErrorCodes.UNKNOWN_ERROR, e);
            }
            configWriter.write("[detectorRules]\n");
            configWriter.write("detectorIndex = " + detectorIndex + "\n");
            configWriter.write("rulesJson = " + rulesJson + "\n");
        }
    }
}
