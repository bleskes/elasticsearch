
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.detectionrules.DetectionRule;
import org.elasticsearch.xpack.prelert.job.detectionrules.verification.DetectionRuleVerifier;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

public final class DetectorVerifier {
    private static final String[] VALID_EXCLUDE_FREQUENT_SETTINGS = { "true", "false", "t", "f", "yes", "no", "y", "n", "on", "off", "by",
            "over", "all" };

    private DetectorVerifier() {
    }

    /**
     * Checks the configuration is valid
     * <ol>
     * <li>One of fieldName, byFieldName, overFieldName or function must be
     * set</li>
     * <li>Unless the function is 'count' one of fieldName, byFieldName or
     * overFieldName must be set</li>
     * <li>If byFieldName is set function or fieldName must bet set</li>
     * <li>If overFieldName is set function or fieldName must bet set</li>
     * <li>function is one of the strings in the set
     * {@link Detector#ANALYSIS_FUNCTIONS}</li>
     * <li>Function cannot be 'metric' (explicitly or implicitly) in jobs that
     * take pre-summarised input</li>
     * <li>If function is not set but the fieldname happens to be the same as
     * one of the function names (e.g.a field called 'count') set function to
     * 'metric'</li>
     * <li>Check the metric/by/over fields are set as required by the different
     * functions</li>
     * <li>Check the metric/by/over fields that cannot be set with certain
     * functions are not set</li>
     * <li>If the function is NON_ZERO_COUNT or NZC then overFieldName must not
     * be set</li>
     * </ol>
     *
     * @param detector
     *            The detector configuration
     * @param isSummarised
     *            Is this detector in a pre-summarised job?
     * @return true
     */
    public static boolean verify(Detector detector, boolean isSummarised) throws ElasticsearchParseException {
        boolean emptyField = detector.getFieldName() == null || detector.getFieldName().isEmpty();
        boolean emptyByField = detector.getByFieldName() == null || detector.getByFieldName().isEmpty();
        boolean emptyOverField = detector.getOverFieldName() == null || detector.getOverFieldName().isEmpty();


        if (Detector.ANALYSIS_FUNCTIONS.contains(detector.getFunction()) == false) {
            throw ExceptionsHelper.parseException(Messages.getMessage(Messages.JOB_CONFIG_UNKNOWN_FUNCTION, detector.getFunction()),
                    ErrorCodes.UNKNOWN_FUNCTION);
        }

        if (emptyField && emptyByField && emptyOverField) {
            if (!Detector.COUNT_WITHOUT_FIELD_FUNCTIONS.contains(detector.getFunction())) {
                throw ExceptionsHelper.parseException(Messages.getMessage(Messages.JOB_CONFIG_NO_ANALYSIS_FIELD_NOT_COUNT),
                        ErrorCodes.INVALID_FIELD_SELECTION);
            }
        }

        if (isSummarised && detector.getFunction().equals(Detector.METRIC)) {
            throw ExceptionsHelper.parseException(
                    Messages.getMessage(Messages.JOB_CONFIG_FUNCTION_INCOMPATIBLE_PRESUMMARIZED, Detector.METRIC),
                    ErrorCodes.INVALID_FUNCTION);
        }

        // check functions have required fields

        if (emptyField && Detector.FIELD_NAME_FUNCTIONS.contains(detector.getFunction())) {
            throw ExceptionsHelper.parseException(
                    Messages.getMessage(Messages.JOB_CONFIG_FUNCTION_REQUIRES_FIELDNAME, detector.getFunction()),
                    ErrorCodes.INVALID_FIELD_SELECTION);
        }

        if (!emptyField && (Detector.FIELD_NAME_FUNCTIONS.contains(detector.getFunction()) == false)) {
            throw ExceptionsHelper.parseException(
                    Messages.getMessage(Messages.JOB_CONFIG_FIELDNAME_INCOMPATIBLE_FUNCTION, detector.getFunction()),
                    ErrorCodes.INVALID_FIELD_SELECTION);
        }

        if (emptyByField && Detector.BY_FIELD_NAME_FUNCTIONS.contains(detector.getFunction())) {
            throw ExceptionsHelper.parseException(
                    Messages.getMessage(Messages.JOB_CONFIG_FUNCTION_REQUIRES_BYFIELD, detector.getFunction()),
                    ErrorCodes.INVALID_FIELD_SELECTION);
        }

        if (!emptyByField && Detector.NO_BY_FIELD_NAME_FUNCTIONS.contains(detector.getFunction())) {
            throw ExceptionsHelper.parseException(
                    Messages.getMessage(Messages.JOB_CONFIG_BYFIELD_INCOMPATIBLE_FUNCTION, detector.getFunction()),
                    ErrorCodes.INVALID_FIELD_SELECTION);
        }

        if (emptyOverField && Detector.OVER_FIELD_NAME_FUNCTIONS.contains(detector.getFunction())) {
            throw ExceptionsHelper.parseException(
                    Messages.getMessage(Messages.JOB_CONFIG_FUNCTION_REQUIRES_OVERFIELD, detector.getFunction()),
                    ErrorCodes.INVALID_FIELD_SELECTION);
        }

        if (!emptyOverField && Detector.NO_OVER_FIELD_NAME_FUNCTIONS.contains(detector.getFunction())) {
            throw ExceptionsHelper.parseException(
                    Messages.getMessage(Messages.JOB_CONFIG_OVERFIELD_INCOMPATIBLE_FUNCTION, detector.getFunction()),
                    ErrorCodes.INVALID_FIELD_SELECTION);
        }

        // field names cannot contain certain characters
        String[] fields = { detector.getFieldName(), detector.getByFieldName(), detector.getOverFieldName(),
                detector.getPartitionFieldName() };
        for (String field : fields) {
            verifyFieldName(field);
        }

        verifyExcludeFrequent(detector.getExcludeFrequent());
        verifyDetectorRules(detector);

        return true;
    }

    /**
     * Check that the setting of the excludeFrequent option is valid. This
     * validation is designed to match that applied by the C++ code.
     *
     * @param excludeFrequent
     *            The setting as a string.
     * @return true
     */
    static boolean verifyExcludeFrequent(String excludeFrequent) throws ElasticsearchParseException {
        // Not set is fine
        if (excludeFrequent == null || excludeFrequent.isEmpty()) {
            return true;
        }

        // Any of these words are fine
        for (String word : VALID_EXCLUDE_FREQUENT_SETTINGS) {
            if (word.equalsIgnoreCase(excludeFrequent)) {
                return true;
            }
        }

        // Convertible to a number (which will then be interpreted as a boolean)
        // is fine
        try {
            Long.parseLong(excludeFrequent);
        } catch (NumberFormatException nfe) {
            throw ExceptionsHelper.parseException(Messages.getMessage(Messages.JOB_CONFIG_INVALID_EXCLUDEFREQUENT_SETTING, excludeFrequent),
                    ErrorCodes.INVALID_EXCLUDEFREQUENT_SETTING);
        }

        return true;
    }

    /**
     * Check that the characters used in a field name will not cause problems.
     *
     * @param field
     *            The field name to be validated
     * @return true
     */
    public static boolean verifyFieldName(String field) throws ElasticsearchParseException {
        if (field != null && containsInvalidChar(field)) {
            throw ExceptionsHelper.parseException(
                    Messages.getMessage(Messages.JOB_CONFIG_INVALID_FIELDNAME_CHARS, field, Detector.PROHIBITED),
                    ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME);

        }
        return true;
    }

    private static boolean containsInvalidChar(String field) {
        for (Character ch : Detector.PROHIBITED_FIELDNAME_CHARACTERS) {
            if (field.indexOf(ch) >= 0) {
                return true;
            }
        }
        return field.chars().anyMatch(ch -> Character.isISOControl(ch));
    }

    private static void verifyDetectorRules(Detector detector) throws ElasticsearchParseException {
        if (detector.getDetectorRules() != null) {
            for (DetectionRule rule : detector.getDetectorRules()) {
                DetectionRuleVerifier.verify(rule, detector);
            }
        }
    }
}
