package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.junit.Before;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;

public class CategorizationFiltersUpdaterTest extends ESTestCase {

    private static final String CATEGORIZATION_FIELD = "myCategory";

    private JobDetails job;

    @Before
    public void setJob() {
        job = new JobDetails();
        Detector detector = new Detector();
        detector.setFunction("count");
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setDetectors(Arrays.asList(detector));
        analysisConfig.setCategorizationFilters(null);
        job.setAnalysisConfig(analysisConfig);
    }

    public void testUpdate_GivenTextNode() throws Exception {
        JsonNode node = TextNode.valueOf("5");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater("myJob").update(node));
        assertEquals("Invalid update value for categorizationFilters: value must be an array of strings; actual was: \"5\"", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenArrayWithNonTextualElements() throws Exception {
        JsonNode node = new ObjectMapper().readTree("[\"a\", 3]");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater("myJob").update(node));
        assertEquals("Invalid update value for categorizationFilters: value must be an array of strings; actual was: [\"a\",3]", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenJobHasNoCategorizationFieldName() throws Exception {
        JsonNode node = new ObjectMapper().readTree("[\"a\"]");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater("myJob").update(node));
        assertEquals("categorizationFilters require setting categorizationFieldName", e.getMessage());
        assertEquals(ErrorCodes.CATEGORIZATION_FILTERS_REQUIRE_CATEGORIZATION_FIELD_NAME.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenInvalidRegex() throws Exception {
        givenCategorizationFieldName();
        JsonNode node = new ObjectMapper().readTree("[\"[\"]");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater("myJob").update(node));
        assertEquals("categorizationFilters contains invalid regular expression '['", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenValid() throws Exception {
        JsonNode node = new ObjectMapper().readTree("[\"a\", \"b\"]");

        givenCategorizationFieldName();
        createUpdater("myJob").update(node);

        assertThat(job.getAnalysisConfig().getCategorizationFilters(), equalTo(Arrays.asList("a", "b")));
    }

    public void testCommit_GivenNull() throws Exception {
        job.getAnalysisConfig().setCategorizationFilters(Arrays.asList("foo"));
        JsonNode node = new ObjectMapper().readTree("null");

        givenCategorizationFieldName();
        createUpdater("myJob").update(node);

        assertThat(job.getAnalysisConfig().getCategorizationFilters(), is(nullValue()));
    }

    public void testCommit_GivenEmptyArray() throws Exception {
        job.getAnalysisConfig().setCategorizationFilters(Arrays.asList("foo"));
        JsonNode node = new ObjectMapper().readTree("[]");

        givenCategorizationFieldName();
        CategorizationFiltersUpdater updater = createUpdater("myJob");
        updater.update(node);

        assertThat(job.getAnalysisConfig().getCategorizationFilters(), is(nullValue()));
    }

    private CategorizationFiltersUpdater createUpdater(String jobId) {
        job.setId(jobId);
        return new CategorizationFiltersUpdater(job, "categorizationFilters");
    }

    private void givenCategorizationFieldName() {
        job.getAnalysisConfig().setCategorizationFieldName(CATEGORIZATION_FIELD);
    }
}
