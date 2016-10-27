package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.detectionrules.DetectionRule;
import org.elasticsearch.xpack.prelert.job.detectionrules.RuleCondition;
import org.elasticsearch.xpack.prelert.job.detectionrules.RuleConditionType;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.junit.Before;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.IsEqual.equalTo;

public class DetectorsUpdaterTest extends ESTestCase {

    private static final String JOB_ID = "foo";

    private StringWriter configWriter;
    private JobDetails job;

    @Before
    public void setJob() throws UnknownJobException {
        job = new JobConfiguration().build();
        job.setId(JOB_ID);
        configWriter = new StringWriter();
    }

    public void testUpdate_GivenParamIsNotArray() throws IOException {
        JsonNode node = new ObjectMapper().readTree("{\"index\":0,\"description\":\"haha\"}");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid update value for detectors: value must be an array", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenParamIsNotJsonObject() throws IOException {
        JsonNode node = new ObjectMapper().readTree("[1,2,3]");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid update value for detectors: requires [index] and at least one of [description, detectorRules]", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenMissingDescriptionParam() throws IOException {
        JsonNode node = new ObjectMapper().readTree("[{\"index\":1}]");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid update value for detectors: requires [index] and at least one of [description, detectorRules]", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenIndexOnly() throws IOException {
        JsonNode node = new ObjectMapper().readTree("[{\"index\":0}]");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid update value for detectors: requires [index] and at least one of [description, detectorRules]", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenMissingIndexParam() throws IOException {
        JsonNode node = new ObjectMapper().readTree("[{\"description\":\"bar\",\"detectorRules\":[]}]");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid update value for detectors: requires [index] and at least one of [description, detectorRules]", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenUnknownParam() throws IOException {
        JsonNode node = new ObjectMapper().readTree("[{\"index\":\"1\",\"unknown\":[]}]");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid update value for detectors: requires [index] and at least one of [description, detectorRules]", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenEmptyObject() throws IOException {
        JsonNode node = new ObjectMapper().readTree("[{}]");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid update value for detectors: requires [index] and at least one of [description, detectorRules]", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenIndexIsNotInteger() throws IOException {
        JsonNode node = new ObjectMapper().readTree("[{\"index\":\"a string\", \"description\":\"bar\"}]");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid index: integer expected; actual was: a string", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenDescriptionIsNotString() throws IOException {
        JsonNode node = new ObjectMapper().readTree("[{\"index\":0, \"description\":1}]");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid description: string expected; actual was: 1", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenIndexIsNegative() throws IOException {
        givenJobHasNDetectors(2);

        JsonNode node = new ObjectMapper().readTree("[{\"index\":-1, \"description\":\"bar\"}]");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid index: valid range is [0, 1]; actual was: -1", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenIndexIsEqualToDetectorsCount() throws IOException {
        givenJobHasNDetectors(3);

        JsonNode node = new ObjectMapper().readTree("[{\"index\":3, \"description\":\"bar\"}]");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid index: valid range is [0, 2]; actual was: 3", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenIndexIsGreaterThanDetectorsCount() throws IOException {
        givenJobHasNDetectors(3);

        JsonNode node = new ObjectMapper().readTree("[{\"index\":4, \"description\":\"bar\"}]");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid index: valid range is [0, 2]; actual was: 4", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenMultipleParamsSecondInvalid() throws IOException {
        givenJobHasNDetectors(3);
        JsonNode node = new ObjectMapper().readTree(
                "[{\"index\":1, \"description\":\"Ipanema\"}, {\"index\":4, \"description\":\"A Train\"}]");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid index: valid range is [0, 2]; actual was: 4", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenValidDescription() throws JobException, IOException {
        JsonNode node = new ObjectMapper().readTree("[{\"index\":1, \"description\":\"Ipanema\"}]");
        givenJobHasNDetectors(3);

        DetectorsUpdater updater = createUpdater();
        updater.update(node);

        assertThat(job.getAnalysisConfig().getDetectors().get(1).getDetectorDescription(), equalTo("Ipanema"));
    }

    public void testUpdate_GivenRulesCannotBeParsed() throws IOException {
        givenJobHasNDetectors(3);
        JsonNode node = new ObjectMapper().readTree(
                "[{\"index\":1, \"detectorRules\":[{\"actionRule\":\"invalid\"}]}]");

        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class, () -> createUpdater().update(node));
        assertEquals("JSON parse error reading the update value for detectorRules", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenEmptyDescription() throws JobException, IOException {
        JsonNode node = new ObjectMapper().readTree("[{\"index\":1, \"description\":\"\"}]");
        givenJobHasNDetectors(3);
        Detector existingDetector = job.getAnalysisConfig().getDetectors().get(1);
        Detector newDetector = new Detector("mean", "responsetime");
        newDetector.setByFieldName("airline");
        newDetector.setOverFieldName(existingDetector.getOverFieldName());
        newDetector.setPartitionFieldName(existingDetector.getPartitionFieldName());
        newDetector.setExcludeFrequent(existingDetector.getExcludeFrequent());
        newDetector.setUseNull(existingDetector.isUseNull());
        newDetector.setDetectorRules(existingDetector.getDetectorRules());
        job.getAnalysisConfig().getDetectors().set(1, newDetector);

        DetectorsUpdater updater = createUpdater();
        updater.update(node);

        assertThat(job.getAnalysisConfig().getDetectors().get(1).getDetectorDescription(), equalTo("mean(responsetime) by airline"));
    }

    public void testUpdate_GivenMultipleValidParams() throws JobException, IOException {
        JsonNode node = new ObjectMapper().readTree(
                "[{\"index\":1, \"description\":\"Ipanema\"}, {\"index\":0, \"description\":\"A Train\"}]");
        givenJobHasNDetectors(3);

        DetectorsUpdater updater = createUpdater();
        updater.update(node);

        assertThat(job.getAnalysisConfig().getDetectors().get(0).getDetectorDescription(), equalTo("A Train"));
        assertThat(job.getAnalysisConfig().getDetectors().get(1).getDetectorDescription(), equalTo("Ipanema"));
    }

    public void testUpdate_GivenValidRules() throws JobException, IOException {
        JsonNode node = new ObjectMapper().readTree(
                "[{\"index\":0, \"detectorRules\":[{\"ruleConditions\":["
                        + "{\"conditionType\":\"numerical_actual\","
                        + "\"condition\":{\"operator\":\"LT\",\"value\":\"3\"},"
                        + "\"fieldName\":\"field\","
                        + "\"fieldValue\":\"value\""
                        + "}]}]}]");
        givenJobHasNDetectors(1);
        Detector existingDetector = job.getAnalysisConfig().getDetectors().get(0);
        Detector newDetector = new Detector("count");
        newDetector.setByFieldName(existingDetector.getByFieldName());
        newDetector.setOverFieldName(existingDetector.getOverFieldName());
        newDetector.setPartitionFieldName(existingDetector.getPartitionFieldName());
        newDetector.setExcludeFrequent(existingDetector.getExcludeFrequent());
        newDetector.setUseNull(existingDetector.isUseNull());
        newDetector.setDetectorRules(existingDetector.getDetectorRules());
        newDetector.setDetectorDescription(existingDetector.getDetectorDescription());
        job.getAnalysisConfig().getDetectors().set(0, newDetector);

        List<DetectionRule> rules = new ArrayList<>();
        DetectionRule rule = new DetectionRule();
        RuleCondition condition =
                new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, "field", "value", new Condition(Operator.LT, "3"), null);
        rule.setRuleConditions(Arrays.asList(condition));
        rules.add(rule);

        DetectorsUpdater updater = createUpdater();
        updater.update(node);

        assertThat(job.getAnalysisConfig().getDetectors().get(0).getDetectorRules(), equalTo(rules));
        String expectedRulesJson = new ObjectMapper().writeValueAsString(rules);
        String expectedConfig = "[detectorRules]\ndetectorIndex = 0\nrulesJson = "
                + expectedRulesJson + "\n";
        assertEquals(expectedConfig, configWriter.toString());
    }

    public void testUpdate_GivenValidDescriptionAndRules() throws JobException, IOException {
        JsonNode node = new ObjectMapper().readTree(
                "[{\"index\":0, \"description\":\"Ipanema\","
                        + "\"detectorRules\":[{\"ruleConditions\":["
                        + "{\"conditionType\":\"numerical_actual\","
                        + "\"condition\":{\"operator\":\"LT\",\"value\":\"3\"},"
                        + "\"fieldName\":\"field\","
                        + "\"fieldValue\":\"value\""
                        + "}]}]}]");
        givenJobHasNDetectors(1);
        Detector existingDetector = job.getAnalysisConfig().getDetectors().get(0);
        Detector newDetector = new Detector("count");
        newDetector.setByFieldName(existingDetector.getByFieldName());
        newDetector.setOverFieldName(existingDetector.getOverFieldName());
        newDetector.setPartitionFieldName(existingDetector.getPartitionFieldName());
        newDetector.setExcludeFrequent(existingDetector.getExcludeFrequent());
        newDetector.setUseNull(existingDetector.isUseNull());
        newDetector.setDetectorRules(existingDetector.getDetectorRules());
        newDetector.setDetectorDescription(existingDetector.getDetectorDescription());
        job.getAnalysisConfig().getDetectors().set(0, newDetector);

        List<DetectionRule> rules = new ArrayList<>();
        DetectionRule rule = new DetectionRule();
        RuleCondition condition =
                new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, "field", "value", new Condition(Operator.LT, "3"), null);
        rule.setRuleConditions(Arrays.asList(condition));
        rules.add(rule);

        DetectorsUpdater updater = createUpdater();
        updater.update(node);

        assertThat(job.getAnalysisConfig().getDetectors().get(0).getDetectorDescription(), equalTo("Ipanema"));
        assertThat(job.getAnalysisConfig().getDetectors().get(0).getDetectorRules(), equalTo(rules));
        String expectedRulesJson = new ObjectMapper().writeValueAsString(rules);
        String expectedConfig = "[detectorRules]\ndetectorIndex = 0\nrulesJson = "
                + expectedRulesJson + "\n";
        assertEquals(expectedConfig, configWriter.toString());
    }

    private DetectorsUpdater createUpdater() {
        return new DetectorsUpdater(job, "detectors", configWriter);
    }

    private void givenJobHasNDetectors(int n) {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        List<Detector> detectors = new ArrayList<>();
        for (int i = 0; i< n; i++)
        {
            Detector detector = new Detector("count");
            detector.setByFieldName("field");
            detectors.add(detector);
        }
        analysisConfig.setDetectors(detectors);
        job.setAnalysisConfig(analysisConfig);
    }
}
