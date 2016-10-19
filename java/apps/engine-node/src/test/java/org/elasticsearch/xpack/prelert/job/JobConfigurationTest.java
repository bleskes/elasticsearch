package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.config.verification.JobConfigurationVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class JobConfigurationTest extends ESTestCase {

    /**
     * Test the {@link AnalysisConfig#analysisFields()} method which produces
     * a list of analysis fields from the detectors
     */
    public void testAnalysisConfigRequiredFields() {
        Detector d1 = new Detector("max", "field");
        d1.setByFieldName("by");

        Detector d2 = new Detector("metric", "field2");
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

        Detector d3 = new Detector("count");
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

    public void testGenerateJobId_doesnotIncludeHost() {
        Pattern pattern = Pattern.compile("[0-9]{14}-[0-9]{5,64}");
        String jobId = JobConfiguration.generateJobId(null);
        assertTrue(jobId, pattern.matcher(jobId).matches());
    }

    public void testGenerateJobId_IncludesHost() {
        Pattern pattern = Pattern.compile("[0-9]{14}-server-1-[0-9]{5,64}");
        String jobId = JobConfiguration.generateJobId("server-1");
        assertTrue(jobId, pattern.matcher(jobId).matches());
    }

    public void testGenerateJobId_isShorterThanMaxHJobLength() {
        assertTrue(JobConfiguration.generateJobId(null).length() < JobConfigurationVerifier.MAX_JOB_ID_LENGTH);
    }

    public void testGenerateJobId_isShorterThanMaxHJobLength_withLongHostname() {
        String id = JobConfiguration.generateJobId("averyverylongstringthatcouldbeahostnameorfullyqualifieddomainname");
        assertEquals(JobConfigurationVerifier.MAX_JOB_ID_LENGTH, id.length());
        assertTrue(id.endsWith(String.format("%05d", JobConfiguration.ID_SEQUENCE.get())));
    }

    public void testGenerateJobId_isShorterThanMaxHJobLength_withLongHostname_andSixDigitSequence() {
        String id = null;
        for (int i = 0; i < 100000; i++) {
            id = JobConfiguration.generateJobId("averyverylongstringthatcouldbeahostnameorfullyqualifieddomainname");
        }
        assertTrue(id.endsWith("-" + JobConfiguration.ID_SEQUENCE.get()));
        assertEquals(JobConfigurationVerifier.MAX_JOB_ID_LENGTH, id.length());
    }
}
