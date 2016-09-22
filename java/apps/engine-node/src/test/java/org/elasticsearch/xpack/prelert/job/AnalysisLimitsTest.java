
package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class AnalysisLimitsTest extends ESTestCase {

    public void testSetModelMemoryLimit_GivenNegative() {
        AnalysisLimits limits = new AnalysisLimits();

        limits.setModelMemoryLimit(-42);

        assertEquals(-1, limits.getModelMemoryLimit());
    }


    public void testSetModelMemoryLimit_GivenZero() {
        AnalysisLimits limits = new AnalysisLimits();

        limits.setModelMemoryLimit(0);

        assertEquals(0, limits.getModelMemoryLimit());
    }


    public void testSetModelMemoryLimit_GivenPositive() {
        AnalysisLimits limits = new AnalysisLimits();

        limits.setModelMemoryLimit(52);

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
        AnalysisLimits analysisLimits1 = new AnalysisLimits(5555, 3L);
        AnalysisLimits analysisLimits2 = new AnalysisLimits(5555, 3L);

        assertEquals(analysisLimits1.hashCode(), analysisLimits2.hashCode());
    }


    public void testToMap_GivenDefault() {
        AnalysisLimits defaultLimits = new AnalysisLimits();

        Map<String, Object> map = defaultLimits.toMap();

        assertEquals(1, map.size());
        assertEquals(0L, map.get(AnalysisLimits.MODEL_MEMORY_LIMIT));
    }


    public void testToMap_GivenFullyPopulated() {
        AnalysisLimits limits = new AnalysisLimits();
        limits.setCategorizationExamplesLimit(5L);
        limits.setModelMemoryLimit(1000L);

        Map<String, Object> map = limits.toMap();

        assertEquals(2, map.size());
        assertEquals(1000L, map.get(AnalysisLimits.MODEL_MEMORY_LIMIT));
        assertEquals(5L, map.get(AnalysisLimits.CATEGORIZATION_EXAMPLES_LIMIT));
    }
}
