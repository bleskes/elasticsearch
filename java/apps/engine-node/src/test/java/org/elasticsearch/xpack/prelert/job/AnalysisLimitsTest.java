
package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

import java.util.Map;

public class AnalysisLimitsTest extends AbstractSerializingTestCase<AnalysisLimits> {

    @Override
    protected AnalysisLimits createTestInstance() {
        return new AnalysisLimits(randomLong(), randomBoolean() ? randomPositiveLong() : null);
    }

    @Override
    protected Writeable.Reader<AnalysisLimits> instanceReader() {
        return AnalysisLimits::new;
    }

    @Override
    protected AnalysisLimits parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return AnalysisLimits.PARSER.apply(parser, () -> matcher);
    }

    public void testSetModelMemoryLimit_GivenNegative() {
        AnalysisLimits limits = new AnalysisLimits(-42, null);
        assertEquals(-1, limits.getModelMemoryLimit());
    }


    public void testSetModelMemoryLimit_GivenZero() {
        AnalysisLimits limits = new AnalysisLimits(0, null);
        assertEquals(0, limits.getModelMemoryLimit());
    }


    public void testSetModelMemoryLimit_GivenPositive() {
        AnalysisLimits limits = new AnalysisLimits(52L, null);
        assertEquals(52, limits.getModelMemoryLimit());
    }


    public void testEquals_GivenEqual() {
        AnalysisLimits analysisLimits1 = new AnalysisLimits(10, 20L);
        AnalysisLimits analysisLimits2 = new AnalysisLimits(10, 20L);

        assertTrue(analysisLimits1.equals(analysisLimits1));
        assertTrue(analysisLimits1.equals(analysisLimits2));
        assertTrue(analysisLimits2.equals(analysisLimits1));
    }


    public void testEquals_GivenDifferentModelMemoryLimit() {
        AnalysisLimits analysisLimits1 = new AnalysisLimits(10, 20L);
        AnalysisLimits analysisLimits2 = new AnalysisLimits(11, 20L);

        assertFalse(analysisLimits1.equals(analysisLimits2));
        assertFalse(analysisLimits2.equals(analysisLimits1));
    }


    public void testEquals_GivenDifferentCategorizationExamplesLimit() {
        AnalysisLimits analysisLimits1 = new AnalysisLimits(10, 20L);
        AnalysisLimits analysisLimits2 = new AnalysisLimits(10, 21L);

        assertFalse(analysisLimits1.equals(analysisLimits2));
        assertFalse(analysisLimits2.equals(analysisLimits1));
    }


    public void testHashCode_GivenEqual() {
        AnalysisLimits analysisLimits1 = new AnalysisLimits(5555L, 3L);
        AnalysisLimits analysisLimits2 = new AnalysisLimits(5555L, 3L);

        assertEquals(analysisLimits1.hashCode(), analysisLimits2.hashCode());
    }


    public void testToMap_GivenDefault() {
        AnalysisLimits defaultLimits = new AnalysisLimits(0, null);
        Map<String, Object> map = defaultLimits.toMap();
        assertEquals(1, map.size());
        assertEquals(0L, map.get(AnalysisLimits.MODEL_MEMORY_LIMIT.getPreferredName()));
    }


    public void testToMap_GivenFullyPopulated() {
        AnalysisLimits limits = new AnalysisLimits(1000L, 5L);
        Map<String, Object> map = limits.toMap();
        assertEquals(2, map.size());
        assertEquals(1000L, map.get(AnalysisLimits.MODEL_MEMORY_LIMIT.getPreferredName()));
        assertEquals(5L, map.get(AnalysisLimits.CATEGORIZATION_EXAMPLES_LIMIT.getPreferredName()));
    }

    public void testVerify_GivenNegativeCategorizationExamplesLimit() {
        ElasticsearchStatusException e =
                expectThrows(ElasticsearchStatusException.class, () -> new AnalysisLimits(1L, -1L));
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW,
                AnalysisLimits.CATEGORIZATION_EXAMPLES_LIMIT, 0, -1L);
        assertEquals(errorMessage, e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testVerify_GivenValid() {
        new AnalysisLimits(0L, 0L);
        new AnalysisLimits(1L, null);
        new AnalysisLimits(1L, 1L);
    }
}
