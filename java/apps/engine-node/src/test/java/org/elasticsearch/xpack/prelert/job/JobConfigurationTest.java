
package org.elasticsearch.xpack.prelert.job;


import org.elasticsearch.test.ESTestCase;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class JobConfigurationTest extends ESTestCase {
    /**
     * Test the {@link AnalysisConfig#analysisFields()} method which produces
     * a list of analysis fields from the detectors
     */

    public void testAnalysisConfigRequiredFields() {
        Detector d1 = new Detector();
        d1.setFieldName("field");
        d1.setByFieldName("by");
        d1.setFunction("max");

        Detector d2 = new Detector();
        d2.setFieldName("field2");
        d2.setOverFieldName("over");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setSummaryCountFieldName("agg");
        ac.setDetectors(Arrays.asList(d1, d2));

        List<String> analysisFields = ac.analysisFields();
        assertTrue(analysisFields.size() == 5);

        assertTrue(analysisFields.contains("agg"));
        assertTrue(analysisFields.contains("field"));
        assertTrue(analysisFields.contains("by"));
        assertTrue(analysisFields.contains("field2"));
        assertTrue(analysisFields.contains("over"));

        assertFalse(analysisFields.contains("max"));
        assertFalse(analysisFields.contains(""));
        assertFalse(analysisFields.contains(null));

        Detector d3 = new Detector();
        d3.setFunction("count");
        d3.setByFieldName("by2");
        d3.setPartitionFieldName("partition");

        ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(d1, d2, d3));

        analysisFields = ac.analysisFields();
        assertTrue(analysisFields.size() == 6);

        assertTrue(analysisFields.contains("partition"));
        assertTrue(analysisFields.contains("field"));
        assertTrue(analysisFields.contains("by"));
        assertTrue(analysisFields.contains("by2"));
        assertTrue(analysisFields.contains("field2"));
        assertTrue(analysisFields.contains("over"));

        assertFalse(analysisFields.contains("count"));
        assertFalse(analysisFields.contains("max"));
        assertFalse(analysisFields.contains(""));
        assertFalse(analysisFields.contains(null));
    }


    public void testDefaultRenormalizationWindowDays() {
        assertNull(new JobConfiguration().getRenormalizationWindowDays());
    }


    public void testSetRenormalizationWindowDays() {
        JobConfiguration config = new JobConfiguration();
        config.setRenormalizationWindowDays(3L);
        assertEquals(3L, config.getRenormalizationWindowDays().longValue());
    }


    public void testSetIgnoreDowntime() {
        JobConfiguration config = new JobConfiguration();
        config.setIgnoreDowntime(IgnoreDowntime.ONCE);
        assertEquals(IgnoreDowntime.ONCE, config.getIgnoreDowntime());
    }
}
