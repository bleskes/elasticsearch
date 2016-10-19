package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.junit.Before;

import static org.hamcrest.core.IsEqual.equalTo;

public class JobDescriptionUpdaterTest extends ESTestCase {

    private static final String JOB_ID = "foo";

    private JobDetails job;

    @Before
    public void setJob() {
        job = new JobConfiguration().build();
        job.setId(JOB_ID);
        job.setDescription("Before");
    }

    public void testUpdate_GivenNonText() {
        JsonNode node = DoubleNode.valueOf(42.0);

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid update value for job description: value must be a string", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenText() throws JobException {
        JsonNode node = TextNode.valueOf("blah blah...");

        createUpdater().update(node);

        assertThat(job.getDescription(), equalTo("blah blah..."));
    }

    private JobDescriptionUpdater createUpdater() {
        return new JobDescriptionUpdater(job, "description");
    }
}
