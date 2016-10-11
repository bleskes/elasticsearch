
package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import static org.elasticsearch.xpack.prelert.job.process.autodetect.writer.WriterConstants.EQUALS;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.config.DefaultDetectorDescription;
import org.elasticsearch.xpack.prelert.job.detectionrules.DetectionRule;
import org.elasticsearch.xpack.prelert.lists.ListDocument;
import org.elasticsearch.xpack.prelert.utils.PrelertStrings;
import org.elasticsearch.xpack.prelert.utils.Strings;

public class FieldConfigWriter {
    private static final String DETECTOR_PREFIX = "detector.";
    private static final String DETECTOR_CLAUSE_SUFFIX = ".clause";
    private static final String DETECTOR_RULES_SUFFIX = ".rules";
    private static final String INFLUENCER_PREFIX = "influencer.";
    private static final String CATEGORIZATION_FIELD_OPTION = " categorizationfield=";
    private static final String CATEGORIZATION_FILTER_PREFIX = "categorizationfilter.";
    private static final String LIST_PREFIX = "list.";

    // Note: for the Engine API summarycountfield is currently passed as a
    // command line option to prelert_autodetect_api rather than in the field
    // config file

    private static final char NEW_LINE = '\n';

    private final AnalysisConfig config;
    private final Set<ListDocument> lists;
    private final OutputStreamWriter writer;
    private final Logger logger;
    private final ObjectMapper objectMapper;

    public FieldConfigWriter(AnalysisConfig config, Set<ListDocument> lists,
                             OutputStreamWriter writer, Logger logger) {
        this.config = Objects.requireNonNull(config);
        this.lists = Objects.requireNonNull(lists);
        this.writer = Objects.requireNonNull(writer);
        this.logger = Objects.requireNonNull(logger);
        objectMapper = new ObjectMapper();
    }

    /**
     * Write the Prelert autodetect field options to the outputIndex stream.
     *
     * @throws IOException
     */
    public void write() throws IOException {
        StringBuilder contents = new StringBuilder();

        writeDetectors(contents);
        writeLists(contents);
        writeAsEnumeratedSettings(CATEGORIZATION_FILTER_PREFIX, config.getCategorizationFilters(),
                contents, true);

        // As values are written as entire settings rather than part of a
        // clause no quoting is needed
        writeAsEnumeratedSettings(INFLUENCER_PREFIX, config.getInfluencers(), contents, false);

        logger.debug("FieldConfig:\n" + contents.toString());

        writer.write(contents.toString());
    }

    private void writeDetectors(StringBuilder contents) throws IOException {
        int counter = 0;
        for (Detector detector : config.getDetectors()) {
            int detectorId = counter++;
            writeDetectorClause(detectorId, detector, contents);
            writeDetectorRules(detectorId, detector, contents);
        }
    }

    private void writeDetectorClause(int detectorId, Detector detector, StringBuilder contents) {
        contents.append(DETECTOR_PREFIX).append(detectorId)
                .append(DETECTOR_CLAUSE_SUFFIX).append(EQUALS);

        DefaultDetectorDescription.appendOn(detector, contents);

        if (Strings.isNullOrEmpty(config.getCategorizationFieldName()) == false) {
            contents.append(CATEGORIZATION_FIELD_OPTION)
                    .append(quoteField(config.getCategorizationFieldName()));
        }

        contents.append(NEW_LINE);
    }

    private void writeDetectorRules(int detectorId, Detector detector, StringBuilder contents) throws IOException {
        List<DetectionRule> rules = detector.getDetectorRules();
        if (rules == null || rules.isEmpty()) {
            return;
        }

        contents.append(DETECTOR_PREFIX).append(detectorId)
                .append(DETECTOR_RULES_SUFFIX).append(EQUALS);

        String rulesAsJson = objectMapper.writeValueAsString(detector.getDetectorRules());
        contents.append(rulesAsJson);

        contents.append(NEW_LINE);
    }

    private void writeLists(StringBuilder buffer) throws JsonProcessingException {
        for (ListDocument list : lists) {
            String listAsJson = objectMapper.writeValueAsString(list.getItems());
            buffer.append(LIST_PREFIX).append(list.getId()).append(EQUALS).append(listAsJson)
                    .append(NEW_LINE);
        }
    }

    private static void writeAsEnumeratedSettings(String settingName, List<String> values, StringBuilder buffer, boolean quote) {
        if (values == null) {
            return;
        }

        int counter = 0;
        for (String value : values) {
            buffer.append(settingName).append(counter++).append(EQUALS)
                    .append(quote ? quoteField(value) : value).append(NEW_LINE);
        }
    }

    private static String quoteField(String field) {
        return PrelertStrings.doubleQuoteIfNotAlphaNumeric(field);
    }
}
