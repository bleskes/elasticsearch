
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the configured fields in the analysis are correct
 * {@linkplain AnalysisConfig#analysisFields()}
 * {@linkplain AnalysisConfig#fields()}
 * {@linkplain AnalysisConfig#byFields()}
 * {@linkplain AnalysisConfig#overFields()}
 * {@linkplain AnalysisConfig#partitionFields()}
 */
public class AnalysisConfigVerifierTest extends ESTestCase {

    public void testVerify_throws()
            throws JobConfigurationException {
        AnalysisConfig ac = new AnalysisConfig();

        // no detector config
        Detector d = new Detector();
        ac.setDetectors(Arrays.asList(new Detector[]{d}));
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false); // shouldn't get here
        } catch (JobConfigurationException e) {
            assertEquals(ErrorCodes.INVALID_FIELD_SELECTION, e.getErrorCode());
        }

        // count works with no fields
        d.setFunction("count");
        AnalysisConfigVerifier.verify(ac);

        d.setFunction("distinct_count");
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false); // shouldn't get here
        } catch (JobConfigurationException e) {
            assertEquals(ErrorCodes.INVALID_FIELD_SELECTION, e.getErrorCode());
        }

        // should work now
        d.setFieldName("somefield");
        d.setOverFieldName("over");
        AnalysisConfigVerifier.verify(ac);

        d.setFunction("info_content");
        AnalysisConfigVerifier.verify(ac);

        d.setByFieldName("by");
        AnalysisConfigVerifier.verify(ac);

        d.setByFieldName(null);
        d.setFunction("made_up_function");
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false); // shouldn't get here
        } catch (JobConfigurationException e) {
            assertEquals(ErrorCodes.UNKNOWN_FUNCTION, e.getErrorCode());
        }

        ac.setBatchSpan(-1L);
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false); // shouldn't get here
        } catch (JobConfigurationException e) {
            assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        }

        ac = new AnalysisConfig();
        ac.setBucketSpan(-1L);
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false); // shouldn't get here
        } catch (JobConfigurationException e) {
            assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        }

        ac = new AnalysisConfig();
        ac.setPeriod(-1L);
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false); // shouldn't get here
        } catch (JobConfigurationException e) {
            assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        }

        ac = new AnalysisConfig();
        ac.setLatency(-1L);
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false); // shouldn't get here
        } catch (JobConfigurationException e) {
            assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        }
    }


    public void testVerify_GivenNegativeBucketSpan() {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(-1L);

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> AnalysisConfigVerifier.verify(analysisConfig));

        assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW, "bucketSpan", 0, -1), e.getMessage());
    }

    public void testVerify_GivenNegativeBatchSpan() {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBatchSpan(-1L);

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> AnalysisConfigVerifier.verify(analysisConfig));

        assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW, "batchSpan", 0, -1), e.getMessage());
    }


    public void testVerify_GivenNegativeLatency() {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setLatency(-1L);

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> AnalysisConfigVerifier.verify(analysisConfig));

        assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW, "latency", 0, -1), e.getMessage());
    }


    public void testVerify_GivenNegativePeriod() {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setPeriod(-1L);

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> AnalysisConfigVerifier.verify(analysisConfig));

        assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW, "period", 0, -1), e.getMessage());
    }


    public void testVerify_GivenDefaultConfig_ShouldBeInvalidDueToNoDetectors() {
        AnalysisConfig analysisConfig = new AnalysisConfig();

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> AnalysisConfigVerifier.verify(analysisConfig));

        assertEquals(ErrorCodes.INCOMPLETE_CONFIGURATION, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_NO_DETECTORS), e.getMessage());
    }


    public void testVerify_GivenValidConfig() throws JobConfigurationException {
        AnalysisConfig analysisConfig = createValidConfig();

        assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
    }


    public void testVerify_GivenValidConfigWithCategorizationFieldNameAndCategorizationFilters()
            throws JobConfigurationException {
        AnalysisConfig analysisConfig = createValidConfig();
        analysisConfig.setCategorizationFieldName("myCategory");
        analysisConfig.setCategorizationFilters(Arrays.asList("foo", "bar"));

        assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
    }


    public void testVerify_OverlappingBuckets() throws JobConfigurationException {
        AnalysisConfig analysisConfig;
        List<Detector> detectors;
        Detector detector;

        boolean onByDefault = false;

        /* Uncomment this when overlappingBuckets turned on by default
         */
        if (onByDefault) {
            // Test overlappingBuckets unset
            analysisConfig = new AnalysisConfig();
            analysisConfig.setBucketSpan(5000L);
            analysisConfig.setBatchSpan(0L);
            detectors = new ArrayList<>();
            detector = new Detector();
            detector.setFunction("count");
            detectors.add(detector);
            detector = new Detector();
            detector.setFunction("mean");
            detector.setFieldName("value");
            detectors.add(detector);
            analysisConfig.setDetectors(detectors);
            assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
            assertTrue(analysisConfig.getOverlappingBuckets());

            // Test overlappingBuckets unset
            analysisConfig = new AnalysisConfig();
            analysisConfig.setBucketSpan(5000L);
            analysisConfig.setBatchSpan(0L);
            detectors = new ArrayList<>();
            detector = new Detector();
            detector.setFunction("count");
            detectors.add(detector);
            detector = new Detector();
            detector.setFunction("rare");
            detector.setByFieldName("value");
            detectors.add(detector);
            analysisConfig.setDetectors(detectors);
            assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
            assertFalse(analysisConfig.getOverlappingBuckets());

            // Test overlappingBuckets unset
            analysisConfig = new AnalysisConfig();
            analysisConfig.setBucketSpan(5000L);
            analysisConfig.setBatchSpan(0L);
            detectors = new ArrayList<>();
            detector = new Detector();
            detector.setFunction("count");
            detectors.add(detector);
            detector = new Detector();
            detector.setFunction("min");
            detector.setFieldName("value");
            detectors.add(detector);
            detector = new Detector();
            detector.setFunction("max");
            detector.setFieldName("value");
            detectors.add(detector);
            analysisConfig.setDetectors(detectors);
            assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
            assertFalse(analysisConfig.getOverlappingBuckets());
        }

        // Test overlappingBuckets set
        analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(5000L);
        analysisConfig.setBatchSpan(0L);
        detectors = new ArrayList<>();
        detector = new Detector();
        detector.setFunction("count");
        detectors.add(detector);
        detector = new Detector();
        detector.setFunction("rare");
        detector.setByFieldName("value");
        detectors.add(detector);
        analysisConfig.setOverlappingBuckets(false);
        analysisConfig.setDetectors(detectors);
        assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
        assertFalse(analysisConfig.getOverlappingBuckets());

        // Test overlappingBuckets set
        analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(5000L);
        analysisConfig.setBatchSpan(0L);
        analysisConfig.setOverlappingBuckets(true);
        detectors = new ArrayList<>();
        detector = new Detector();
        detector.setFunction("count");
        detectors.add(detector);
        detector = new Detector();
        detector.setFunction("rare");
        detector.setByFieldName("value");
        detectors.add(detector);
        analysisConfig.setDetectors(detectors);
        try {
            AnalysisConfigVerifier.verify(analysisConfig);
            assertTrue(false); // shouldn't get here
        } catch (JobConfigurationException e) {
            assertEquals(ErrorCodes.INVALID_FUNCTION, e.getErrorCode());
        }

        // Test overlappingBuckets set
        analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(5000L);
        analysisConfig.setBatchSpan(0L);
        analysisConfig.setOverlappingBuckets(false);
        detectors = new ArrayList<>();
        detector = new Detector();
        detector.setFunction("count");
        detectors.add(detector);
        detector = new Detector();
        detector.setFunction("mean");
        detector.setFieldName("value");
        detectors.add(detector);
        analysisConfig.setDetectors(detectors);
        assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
        assertFalse(analysisConfig.getOverlappingBuckets());
    }


    public void testMultipleBucketsConfig()
            throws JobConfigurationException {
        AnalysisConfig ac = new AnalysisConfig();
        ac.setMultipleBucketSpans(Arrays.asList(10L, 15L, 20L, 25L, 30L, 35L));
        List<Detector> detectors = new ArrayList<>();
        Detector detector = new Detector();
        detector.setFunction("count");
        detectors.add(detector);
        ac.setDetectors(detectors);
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false);
        } catch (JobConfigurationException e) {
            assertEquals(ErrorCodes.INCOMPLETE_CONFIGURATION, e.getErrorCode());
        }

        ac.setBucketSpan(4L);
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false);
        } catch (JobConfigurationException e) {
            assertEquals(ErrorCodes.MULTIPLE_BUCKETSPANS_NOT_MULTIPLE, e.getErrorCode());
            String rex = String.format(".*'%d'.*'%d'.*", 10, 4);
            assertTrue(e.getMessage().matches(rex));
        }

        ac.setBucketSpan(5L);
        assertTrue(AnalysisConfigVerifier.verify(ac));

        AnalysisConfig ac2 = new AnalysisConfig();
        ac2.setBucketSpan(5L);
        ac2.setDetectors(detectors);
        ac2.setMultipleBucketSpans(Arrays.asList(10L, 15L, 20L, 25L, 30L));
        assertFalse(ac.equals(ac2));
        ac2.setMultipleBucketSpans(Arrays.asList(10L, 15L, 20L, 25L, 30L, 35L));
        assertEquals(ac, ac2);

        ac.setBucketSpan(222L);
        ac.setMultipleBucketSpans(Arrays.asList());
        assertTrue(AnalysisConfigVerifier.verify(ac));

        ac.setMultipleBucketSpans(Arrays.asList(222L));
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false);
        } catch (JobConfigurationException e) {
            assertEquals(ErrorCodes.MULTIPLE_BUCKETSPANS_NOT_MULTIPLE, e.getErrorCode());
            String rex = String.format(".*'%d'.*'%d'.*", 222, 222);
            assertTrue(e.getMessage().matches(rex));
        }

        ac.setMultipleBucketSpans(Arrays.asList(-444L, -888L));
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false);
        } catch (JobConfigurationException e) {
            assertEquals(ErrorCodes.MULTIPLE_BUCKETSPANS_NOT_MULTIPLE, e.getErrorCode());
            String rex = String.format(".*'%d'.*'%d'.*", -444, 222);
            assertTrue(e.getMessage().matches(rex));
        }
    }


    public void testVerify_GivenCategorizationFiltersButNoCategorizationFieldName() {

        AnalysisConfig config = createValidConfig();
        config.setCategorizationFilters(Arrays.asList("foo"));

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> AnalysisConfigVerifier.verify(config));

        assertEquals(ErrorCodes.CATEGORIZATION_FILTERS_REQUIRE_CATEGORIZATION_FIELD_NAME, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CATEGORIZATION_FILTERS_REQUIRE_CATEGORIZATION_FIELD_NAME), e.getMessage());
    }


    public void testVerify_GivenDuplicateCategorizationFilters() {

        AnalysisConfig config = createValidConfig();
        config.setCategorizationFieldName("myCategory");
        config.setCategorizationFilters(Arrays.asList("foo", "bar", "foo"));

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> AnalysisConfigVerifier.verify(config));

        assertEquals(ErrorCodes.CATEGORIZATION_FILTERS_CONTAIN_DUPLICATES, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CATEGORIZATION_FILTERS_CONTAINS_DUPLICATES), e.getMessage());
    }


    public void testVerify_GivenEmptyCategorizationFilter() {

        AnalysisConfig config = createValidConfig();
        config.setCategorizationFieldName("myCategory");
        config.setCategorizationFilters(Arrays.asList("foo", ""));

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> AnalysisConfigVerifier.verify(config));

        assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CATEGORIZATION_FILTERS_CONTAINS_EMPTY), e.getMessage());
    }


    public void testCheckDetectorsHavePartitionFields() {

        AnalysisConfig config = createValidConfig();
        config.setUsePerPartitionNormalization(true);

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> AnalysisConfigVerifier.verify(config));

        assertEquals(ErrorCodes.PER_PARTITION_NORMALIZATION_REQUIRES_PARTITION_FIELD, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_PER_PARTITION_NORMALIZATION_REQUIRES_PARTITION_FIELD), e.getMessage());
    }


    public void testCheckDetectorsHavePartitionFields_doesntThrowWhenValid()
            throws JobConfigurationException {
        AnalysisConfig config = createValidConfig();
        config.getDetectors().get(0).setPartitionFieldName("pField");
        config.setUsePerPartitionNormalization(true);

        AnalysisConfigVerifier.verify(config);
    }


    public void testCheckNoInfluencersAreSet() {

        AnalysisConfig config = createValidConfig();
        config.getDetectors().get(0).setPartitionFieldName("pField");
        config.setInfluencers(Arrays.asList("inf1", "inf2"));
        config.setUsePerPartitionNormalization(true);

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> AnalysisConfigVerifier.verify(config));

        assertEquals(ErrorCodes.PER_PARTITION_NORMALIZATION_CANNOT_USE_INFLUENCERS, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_PER_PARTITION_NORMALIZATION_CANNOT_USE_INFLUENCERS), e.getMessage());
    }


    public void testVerify_GivenCategorizationFiltersContainInvalidRegex() {

        AnalysisConfig config = createValidConfig();
        config.setCategorizationFieldName("myCategory");
        config.setCategorizationFilters(Arrays.asList("foo", "("));

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> AnalysisConfigVerifier.verify(config));

        assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CATEGORIZATION_FILTERS_CONTAINS_INVALID_REGEX, "("), e.getMessage());
    }

    private static AnalysisConfig createValidConfig() {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(3600L);
        analysisConfig.setBatchSpan(0L);
        analysisConfig.setLatency(0L);
        analysisConfig.setPeriod(0L);
        List<Detector> detectors = new ArrayList<>();
        Detector detector = new Detector();
        detector.setFieldName("count");
        detectors.add(detector);
        analysisConfig.setDetectors(detectors);
        return analysisConfig;
    }
}
