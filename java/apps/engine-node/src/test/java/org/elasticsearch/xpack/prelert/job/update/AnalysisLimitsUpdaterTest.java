package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.AnalysisLimits;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.JobStatus;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;

import java.io.IOException;

import static org.hamcrest.core.IsEqual.equalTo;

public class AnalysisLimitsUpdaterTest extends ESTestCase {

    public void testPrepareUpdate_GivenJobIsNotClosed() throws JobException {
        JobDetails job = createJob("foo", JobStatus.RUNNING, 42L);

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater(job).update(null));
        assertEquals("Cannot update key 'analysisLimits' while job is not closed; current status is RUNNING", e.getMessage());
        assertEquals(ErrorCodes.JOB_NOT_CLOSED.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenNull() throws IOException {
        JobDetails job = createJob("foo", JobStatus.CLOSED, 42L);
        String update = "null";
        JsonNode node = new ObjectMapper().readTree(update);

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater(job).update(node));
        assertEquals("Invalid update value for analysisLimits: null", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenInvalidJson() throws IOException {
        JobDetails job = createJob("foo", JobStatus.CLOSED, 42L);
        String update = "{\"modelMemory!!Limit\":50}";
        JsonNode node = new ObjectMapper().readTree(update);

        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class, () -> createUpdater(job).update(node));
        assertEquals("JSON parse error reading the update value for analysisLimits", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenCategorizationExamplesLimitLessThanZero() throws IOException {
        JobDetails job = createJob("foo", JobStatus.CLOSED, 0L);
        String update = "{\"categorizationExamplesLimit\":-1}";
        JsonNode node = new ObjectMapper().readTree(update);

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater(job).update(node));
        assertEquals("categorizationExamplesLimit cannot be less than 0. Value = -1", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenModelMemoryLimitLessIsDecreased() throws IOException {
        JobDetails job = createJob("foo", JobStatus.CLOSED, 42L);
        String update = "{\"modelMemoryLimit\":41}";
        JsonNode node = new ObjectMapper().readTree(update);

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater(job).update(node));
        assertEquals("Invalid update value for analysisLimits: modelMemoryLimit cannot be decreased; existing is 42, update had 41", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdateAndCommit_GivenValid() throws IOException {
        JobDetails job = createJob("foo", JobStatus.CLOSED, 42L);
        String update = "{\"modelMemoryLimit\":43, \"categorizationExamplesLimit\": 5}";
        JsonNode node = new ObjectMapper().readTree(update);

        createUpdater(job).update(node);

        assertThat(job.getAnalysisLimits().getModelMemoryLimit(), equalTo(43L));
        assertThat(job.getAnalysisLimits().getCategorizationExamplesLimit(), equalTo(5L));
    }

    public void testUpdateAndCommit_GivenValidAndExistingLimitsIsNull() throws IOException {
        JobDetails job = createJob("foo", JobStatus.CLOSED, null);
        String update = "{\"modelMemoryLimit\":43, \"categorizationExamplesLimit\": 5}";
        JsonNode node = new ObjectMapper().readTree(update);

        createUpdater(job).update(node);

        assertThat(job.getAnalysisLimits().getModelMemoryLimit(), equalTo(43L));
        assertThat(job.getAnalysisLimits().getCategorizationExamplesLimit(), equalTo(5L));
    }

    private static JobDetails createJob(String jobId, JobStatus jobStatus, Long memoryLimit) {
        JobDetails job = new JobDetails();
        job.setId(jobId);
        job.setStatus(jobStatus);
        if (memoryLimit != null) {
            AnalysisLimits analysisLimits = new AnalysisLimits();
            analysisLimits.setModelMemoryLimit(memoryLimit);
            job.setAnalysisLimits(analysisLimits);
        }
        return job;
    }

    private AnalysisLimitsUpdater createUpdater(JobDetails job) {
        return new AnalysisLimitsUpdater(job, "analysisLimits");
    }
}
