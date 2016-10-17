package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.DoubleNode;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.junit.Before;

import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;

public class CustomSettingsUpdaterTest extends ESTestCase {

    private static final String JOB_ID = "foo";

    private JobDetails job;

    @Before
    public void setJob() {
        job = new JobDetails();
        job.setId(JOB_ID);
        job.setCustomSettings(null);
    }

    public void testPrepareUpdate_GivenValueIsNotAnObject() throws JobException {
        JsonNode node = DoubleNode.valueOf(42.0);

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid update value for customSettings: value must be an object", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testPrepareUpdate_GivenObject() throws JobException, IOException {
        JsonNode node = new ObjectMapper().readTree("{\"a\":1}");

        createUpdater().update(node);

        assertThat(job.getCustomSettings().size(), equalTo(1));
        assertThat(job.getCustomSettings().get("a"), equalTo(1));
    }

    private CustomSettingsUpdater createUpdater() {
        return new CustomSettingsUpdater(job, "customSettings");
    }
}
