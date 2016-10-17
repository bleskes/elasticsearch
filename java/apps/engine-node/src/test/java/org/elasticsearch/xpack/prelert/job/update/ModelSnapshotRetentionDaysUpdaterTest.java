package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.junit.Before;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;

public class ModelSnapshotRetentionDaysUpdaterTest extends ESTestCase {

    private JobDetails job;

    @Before
    public void setJob() {
        job = new JobDetails();
        job.setModelSnapshotRetentionDays(null);
    }

    public void testUpdate_GivenText() throws IOException {
        JsonNode node = TextNode.valueOf("wrong");

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid update value for modelSnapshotRetentionDays: value must be an exact number of days", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenNegativeInteger() throws IOException {
        JsonNode node = LongNode.valueOf(-7);

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid update value for modelSnapshotRetentionDays: value must be an exact number of days", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenInteger() throws JobException, IOException {
        JsonNode node = LongNode.valueOf(7);

        createUpdater().update(node);

        assertThat(job.getModelSnapshotRetentionDays(), equalTo(7L));
    }

    public void testUpdate_GivenNull() throws JobException, IOException {
        JsonNode node = NullNode.getInstance();

        createUpdater().update(node);

        assertThat(job.getModelSnapshotRetentionDays(), is(nullValue()));
    }

    private ModelSnapshotRetentionDaysUpdater createUpdater() {
        return new ModelSnapshotRetentionDaysUpdater(job, "modelSnapshotRetentionDays");
    }
}
