package org.elasticsearch.xpack.prelert.job.update;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.*;
import org.elasticsearch.xpack.prelert.job.detectionrules.DetectionRule;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.IsEqual.equalTo;

public class JobUpdaterTest extends ESTestCase {

    private static final String JOB_ID = "foo";

    private JobDetails job;

    @Before
    public void setJob() throws UnknownJobException {
        job = new JobConfiguration().build();
        job.setId(JOB_ID);
    }

    public void testUpdate_GivenEmptyString() {
        String update = "";

        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class, () -> new JobUpdater(job).update(update));
        assertEquals("JSON parse error reading the job update", e.getMessage());
        assertEquals(ErrorCodes.JOB_CONFIG_PARSE_ERROR.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenEmptyObject() {
        String update = "{}";

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> new JobUpdater(job).update(update));
        assertEquals("Update requires JSON that contains a non-empty object", e.getMessage());
        assertEquals(ErrorCodes.JOB_CONFIG_PARSE_ERROR.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenNoObject() {
        String update = "\"description\":\"foobar\"";

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> new JobUpdater(job).update(update));
        assertEquals("Update requires JSON that contains a non-empty object", e.getMessage());
        assertEquals(ErrorCodes.JOB_CONFIG_PARSE_ERROR.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenInvalidKey() {
        String update = "{\"dimitris\":\"foobar\"}";

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> new JobUpdater(job).update(update));
        assertEquals("Invalid key 'dimitris'", e.getMessage());
        assertEquals(ErrorCodes.INVALID_UPDATE_KEY.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenValidDescriptionUpdate() throws JobException {
        String update = "{\"description\":\"foobar\"}";

        new JobUpdater(job).update(update);

        assertThat(job.getDescription(), equalTo("foobar"));
        // TODO verify nothing was written to process
        // TODO verify update audit: verify(m_Auditor).info("Job updated: [description]");
    }

    public void testUpdate_GivenTwoValidUpdates() throws JobException {
        String update = "{\"description\":\"foobar\", \"modelDebugConfig\":{\"boundsPercentile\":33.9}}";

        new JobUpdater(job).update(update);

        assertThat(job.getDescription(), equalTo("foobar"));
        assertThat(job.getModelDebugConfig(), equalTo(new ModelDebugConfig(null, 33.9, null)));

        String expectedConfig = "[modelDebugConfig]\nboundspercentile = 33.9\nterms = \n";
        // TODO verify config written ^^
    }

    public void testUpdate_GivenValidBackgroundPersistIntervalUpdate() throws JobException {
        String update = "{\"backgroundPersistInterval\": 7200}";

        new JobUpdater(job).update(update);

        assertThat(job.getBackgroundPersistInterval(), equalTo(7200L));
        // TODO verify nothing was written to process
    }

    public void testUpdate_GivenValidCustomSettingsUpdate() throws JobException {
        String update = "{\"customSettings\": {\"radio\":\"head\"}}";

        new JobUpdater(job).update(update);

        Map<String, Object> expected = new HashMap<>();
        expected.put("radio", "head");
        assertThat(job.getCustomSettings().size(), equalTo(1));
        assertThat(job.getCustomSettings().get("radio"), equalTo("head"));
        // TODO verify nothing was written to process
    }

    public void testUpdate_GivenValidIgnoreDowntimeUpdate() throws JobException {
        String update = "{\"ignoreDowntime\": \"always\"}";

        new JobUpdater(job).update(update);

        assertThat(job.getIgnoreDowntime(), equalTo(IgnoreDowntime.ALWAYS));
        // TODO verify nothing was written to process
    }

    public void testUpdate_GivenValidRenormalizationWindowDaysUpdate() throws JobException {
        String update = "{\"renormalizationWindowDays\": 3}";

        new JobUpdater(job).update(update);

        assertThat(job.getRenormalizationWindowDays(), equalTo(3L));
        // TODO verify nothing was written to process
    }

    public void testUpdate_GivenValidModelSnapshotRetentionDaysUpdate() throws JobException {
        String update = "{\"modelSnapshotRetentionDays\": 9}";

        new JobUpdater(job).update(update);

        assertThat(job.getModelSnapshotRetentionDays(), equalTo(9L));
        // TODO verify nothing was written to process
    }

    public void testUpdate_GivenValidResultsRetentionDaysUpdate() throws JobException {
        String update = "{\"resultsRetentionDays\": 3}";

        new JobUpdater(job).update(update);

        assertThat(job.getResultsRetentionDays(), equalTo(3L));
        // TODO verify nothing was written to process
    }

    public void testUpdate_GivenValidCategorizationFiltersUpdate() throws JobException {
        String update = "{\"categorizationFilters\": [\"SQL.*\"]}";

        AnalysisConfig analysisConfig = new AnalysisConfig();
        Detector detector = new Detector("count");
        detector.setByFieldName("prelertCategory");
        analysisConfig.setDetectors(Arrays.asList(detector));
        analysisConfig.setCategorizationFieldName("myCategory");
        job.setAnalysisConfig(analysisConfig);

        new JobUpdater(job).update(update);

        assertThat(job.getAnalysisConfig().getCategorizationFilters(), equalTo(Arrays.asList("SQL.*")));
    }

    public void testUpdate_GivenValidDetectorDescriptionUpdate() throws JobException {
        String update = "{\"detectors\": [{\"index\":0,\"description\":\"the A train\"}]}";

        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setDetectors(Arrays.asList(new Detector("count")));
        job.setAnalysisConfig(analysisConfig);

        new JobUpdater(job).update(update);

        assertThat(job.getAnalysisConfig().getDetectors().get(0).getDetectorDescription(), equalTo("the A train"));
    }

    public void testUpdate_GivenValidDetectorRulesUpdate() throws JobException {
        String update = "{\"detectors\": [{\"index\":0,\"detectorRules\":[]}]}";

        AnalysisConfig analysisConfig = new AnalysisConfig();
        Detector detector = new Detector("count");
        analysisConfig.setDetectors(Arrays.asList(detector));
        job.setAnalysisConfig(analysisConfig);
        List<DetectionRule> rules = new ArrayList<>();

        new JobUpdater(job).update(update);

        assertThat(job.getAnalysisConfig().getDetectors().get(0).getDetectorRules(), equalTo(rules));
    }

    public void testUpdate_GivenValidSchedulerConfigUpdate() throws JobException {
        SchedulerConfig.Builder schedulerConfig = new SchedulerConfig.Builder(SchedulerConfig.DataSource.ELASTICSEARCH);
        schedulerConfig.setBaseUrl("http://localhost:9200");
        schedulerConfig.setIndexes(Arrays.asList("index1"));
        schedulerConfig.setTypes(Arrays.asList("type1"));
        job.setSchedulerConfig(schedulerConfig.build());
        String update = "{\"schedulerConfig\": {"
                + "\"dataSource\":\"ELASTICSEARCH\","
                + "\"baseUrl\":\"http://localhost:9200\","
                + "\"indexes\":[\"index1\", \"index2\"],"
                + "\"types\":[\"type1\", \"type2\"]"
                + "}}";

        new JobUpdater(job).update(update);

        SchedulerConfig.Builder expected = new SchedulerConfig.Builder(SchedulerConfig.DataSource.ELASTICSEARCH);
        expected.setBaseUrl("http://localhost:9200");
        expected.setIndexes(Arrays.asList("index1", "index2"));
        expected.setTypes(Arrays.asList("type1", "type2"));
        Map<String, Object> query = new HashMap<>();
        Map<String, Object> subQuery = new HashMap<>();
        query.put("match_all", subQuery);
        expected.setQuery(query);
        expected.setQueryDelay(60L);
        expected.setRetrieveWholeSource(false);
        expected.setScrollSize(1000);

        assertThat(job.getSchedulerConfig(), equalTo(expected.build()));
    }

    public void testUpdate_GivenValidAnalysisLimitsUpdate() throws JobException {
        AnalysisLimits analysisLimits = new AnalysisLimits(100L, 4L);
        job.setStatus(JobStatus.CLOSED);
        job.setAnalysisLimits(analysisLimits);

        String update = "{\"analysisLimits\": {"
                + "\"modelMemoryLimit\":1000,"
                + "\"categorizationExamplesLimit\":10"
                + "}}";

        new JobUpdater(job).update(update);

        AnalysisLimits newLimits = new AnalysisLimits(1000L, 10L);
        assertThat(job.getAnalysisLimits(), equalTo(newLimits));
    }
}
