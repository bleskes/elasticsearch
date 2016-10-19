package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.ModelDebugConfig;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.junit.Before;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;

public class ModelDebugConfigUpdaterTest extends ESTestCase {

    private JobDetails job;
    private StringWriter m_ConfigWriter;

    @Before
    public void setJob() {
        job = new JobConfiguration().build();
        job.setModelDebugConfig(null);
        m_ConfigWriter = new StringWriter();
    }

    public void testUpdate_GivenInvalidJson() throws IOException {
        JsonNode node = new ObjectMapper().readTree("{\"invalidKey\":3.0}");

        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class, () -> createUpdater().update(node));
        assertEquals("JSON parse error reading the update value for ModelDebugConfig", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenBoundsPercentileIsOutOfBounds() throws JobException, IOException {
        JsonNode node = new ObjectMapper().readTree("{\"boundsPercentile\":300.0}");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid modelDebugConfig: boundsPercentile must be in the range [0, 100]", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenValid() throws JobException, IOException {
        String update = "{\"boundsPercentile\":67.3, \"terms\":\"a,b\"}";
        JsonNode node = new ObjectMapper().readTree(update);

        createUpdater().update(node);

        assertThat(job.getModelDebugConfig(), equalTo(new ModelDebugConfig(null, 67.3, "a,b")));
        String expectedConfig = "[modelDebugConfig]\nboundspercentile = 67.3\nterms = a,b\n";
        assertEquals(expectedConfig, m_ConfigWriter.toString());
    }

    public void testCommit_GivenNull() throws JobException, IOException {
        job.setModelDebugConfig(new ModelDebugConfig(null, 67.3, "a,b"));
        JsonNode node = NullNode.getInstance();

        createUpdater().update(node);

        assertThat(job.getModelDebugConfig(), is(nullValue()));
        String expectedConfig = "[modelDebugConfig]\nboundspercentile = -1.0\nterms = \n";
        assertEquals(expectedConfig, m_ConfigWriter.toString());
    }

    private ModelDebugConfigUpdater createUpdater() {
        return new ModelDebugConfigUpdater(job, "modelDebugConfig", m_ConfigWriter);
    }
}
