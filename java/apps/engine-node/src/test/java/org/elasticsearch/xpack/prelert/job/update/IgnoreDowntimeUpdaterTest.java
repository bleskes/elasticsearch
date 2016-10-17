package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.IgnoreDowntime;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;

public class IgnoreDowntimeUpdaterTest extends ESTestCase {

    private static final String JOB_ID = "foo";

    private JobDetails job;

    @Before
    public void setJob() {
        job = new JobDetails();
        job.setId(JOB_ID);
        job.setIgnoreDowntime(null);
    }

    @Test
    public void testUpdate_GivenInvalidIntegerValue() {
        IntNode node = IntNode.valueOf(42);

        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class, () -> createUpdater().update(node));
        assertEquals("Invalid update value for ignoreDowntime: expected one of [NEVER, ONCE, ALWAYS]; actual was: 42", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    @Test
    public void testUpdate_GivenInvalidTextValue() {
        TextNode node = TextNode.valueOf("invalid");

        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class, () -> createUpdater().update(node));
        assertEquals("Invalid update value for ignoreDowntime: expected one of [NEVER, ONCE, ALWAYS]; actual was: \"invalid\"", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));;
    }

    @Test
    public void testUpdate_GivenValidValue() throws JobException {
        TextNode node = TextNode.valueOf("once");

        createUpdater().update(node);

        assertThat(job.getIgnoreDowntime(), equalTo(IgnoreDowntime.ONCE));
    }

    private IgnoreDowntimeUpdater createUpdater()
    {
        return new IgnoreDowntimeUpdater(job, "ignoreDowntime");
    }
}
