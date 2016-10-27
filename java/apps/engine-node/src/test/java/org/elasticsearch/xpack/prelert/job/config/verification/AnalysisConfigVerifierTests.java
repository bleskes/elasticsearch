/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
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
public class AnalysisConfigVerifierTests extends ESTestCase {

    public void testVerify_throws() {
        AnalysisConfig ac = new AnalysisConfig();

        // count works with no fields
        Detector d = new Detector.Builder("count", null).build();
        ac.setDetectors(Arrays.asList(new Detector[] { d }));
        AnalysisConfigVerifier.verify(ac);

        try {
            d = new Detector.Builder("distinct_count", null).build();
            ac.setDetectors(Arrays.asList(new Detector[] { d }));
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false); // shouldn't get here
        } catch (ElasticsearchParseException e) {
            assertEquals(1, e.getHeader("errorCode").size());
            assertEquals(ErrorCodes.INVALID_FIELD_SELECTION.getValueString(), e.getHeader("errorCode").get(0));
        }

        // should work now
        Detector.Builder builder = new Detector.Builder("distinct_count", "somefield");
        builder.setOverFieldName("over");
        ac.setDetectors(Arrays.asList(new Detector[]{builder.build()}));
        AnalysisConfigVerifier.verify(ac);

        builder = new Detector.Builder("info_content", "somefield");
        builder.setOverFieldName("over");
        d = builder.build();
        ac.setDetectors(Arrays.asList(new Detector[]{builder.build()}));
        AnalysisConfigVerifier.verify(ac);

        builder.setByFieldName("by");
        ac.setDetectors(Arrays.asList(new Detector[]{builder.build()}));
        AnalysisConfigVerifier.verify(ac);

        try {
            builder = new Detector.Builder("made_up_function", "somefield");
            builder.setOverFieldName("over");
            ac.setDetectors(Arrays.asList(new Detector[]{builder.build()}));
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false); // shouldn't get here
        } catch (ElasticsearchParseException e) {
            assertEquals(1, e.getHeader("errorCode").size());
            assertEquals(ErrorCodes.UNKNOWN_FUNCTION.getValueString(), e.getHeader("errorCode").get(0));
        }

        ac.setBatchSpan(-1L);
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false); // shouldn't get here
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        }

        ac = new AnalysisConfig();
        ac.setBucketSpan(-1L);
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false); // shouldn't get here
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        }

        ac = new AnalysisConfig();
        ac.setPeriod(-1L);
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false); // shouldn't get here
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        }

        ac = new AnalysisConfig();
        ac.setLatency(-1L);
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false); // shouldn't get here
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        }
    }


    public void testVerify_GivenNegativeBucketSpan() {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(-1L);

        ElasticsearchStatusException e = ESTestCase.expectThrows(
                ElasticsearchStatusException.class, () -> AnalysisConfigVerifier.verify(analysisConfig));

        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW, "bucketSpan", 0, -1), e.getMessage());
    }

    public void testVerify_GivenNegativeBatchSpan() {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBatchSpan(-1L);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> AnalysisConfigVerifier.verify(analysisConfig));

        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW, "batchSpan", 0, -1), e.getMessage());
    }


    public void testVerify_GivenNegativeLatency() {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setLatency(-1L);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> AnalysisConfigVerifier.verify(analysisConfig));

        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW, "latency", 0, -1), e.getMessage());
    }


    public void testVerify_GivenNegativePeriod() {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setPeriod(-1L);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> AnalysisConfigVerifier.verify(analysisConfig));

        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW, "period", 0, -1), e.getMessage());
    }


    public void testVerify_GivenDefaultConfig_ShouldBeInvalidDueToNoDetectors() {
        AnalysisConfig analysisConfig = new AnalysisConfig();

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> AnalysisConfigVerifier.verify(analysisConfig));

        assertEquals(ErrorCodes.INCOMPLETE_CONFIGURATION.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_NO_DETECTORS), e.getMessage());
    }


    public void testVerify_GivenValidConfig() {
        AnalysisConfig analysisConfig = createValidConfig();

        assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
    }


    public void testVerify_GivenValidConfigWithCategorizationFieldNameAndCategorizationFilters() {
        AnalysisConfig analysisConfig = createValidConfig();
        analysisConfig.setCategorizationFieldName("myCategory");
        analysisConfig.setCategorizationFilters(Arrays.asList("foo", "bar"));

        assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
    }


    public void testVerify_OverlappingBuckets() {
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
            detector = new Detector.Builder("count", null).build();
            detectors.add(detector);
            detector = new Detector.Builder("mean", "value").build();
            detectors.add(detector);
            analysisConfig.setDetectors(detectors);
            assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
            assertTrue(analysisConfig.getOverlappingBuckets());

            // Test overlappingBuckets unset
            analysisConfig = new AnalysisConfig();
            analysisConfig.setBucketSpan(5000L);
            analysisConfig.setBatchSpan(0L);
            detectors = new ArrayList<>();
            detector = new Detector.Builder("count", null).build();
            detectors.add(detector);
            detector = new Detector.Builder("rare", "value").build();
            detectors.add(detector);
            analysisConfig.setDetectors(detectors);
            assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
            assertFalse(analysisConfig.getOverlappingBuckets());

            // Test overlappingBuckets unset
            analysisConfig = new AnalysisConfig();
            analysisConfig.setBucketSpan(5000L);
            analysisConfig.setBatchSpan(0L);
            detectors = new ArrayList<>();
            detector = new Detector.Builder("count", null).build();
            detectors.add(detector);
            detector = new Detector.Builder("min", "value").build();
            detectors.add(detector);
            detector = new Detector.Builder("max", "value").build();
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
        detector = new Detector.Builder("count", null).build();
        detectors.add(detector);
        Detector.Builder builder = new Detector.Builder("rare", null);
        builder.setByFieldName("value");
        detectors.add(builder.build());
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
        detector = new Detector.Builder("count", null).build();
        detectors.add(detector);
        builder = new Detector.Builder("rare", null);
        builder.setByFieldName("value");
        detectors.add(builder.build());
        analysisConfig.setDetectors(detectors);
        try {
            AnalysisConfigVerifier.verify(analysisConfig);
            assertTrue(false); // shouldn't get here
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.INVALID_FUNCTION.getValueString(), e.getHeader("errorCode").get(0));
        }

        // Test overlappingBuckets set
        analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(5000L);
        analysisConfig.setBatchSpan(0L);
        analysisConfig.setOverlappingBuckets(false);
        detectors = new ArrayList<>();
        detector = new Detector.Builder("count", null).build();
        detectors.add(detector);
        detector = new Detector.Builder("mean", "value").build();
        detectors.add(detector);
        analysisConfig.setDetectors(detectors);
        assertTrue(AnalysisConfigVerifier.verify(analysisConfig));
        assertFalse(analysisConfig.getOverlappingBuckets());
    }


    public void testMultipleBucketsConfig() {
        AnalysisConfig ac = new AnalysisConfig();
        ac.setMultipleBucketSpans(Arrays.asList(10L, 15L, 20L, 25L, 30L, 35L));
        List<Detector> detectors = new ArrayList<>();
        Detector detector = new Detector.Builder("count", null).build();
        detectors.add(detector);
        ac.setDetectors(detectors);
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false);
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.INCOMPLETE_CONFIGURATION.getValueString(), e.getHeader("errorCode").get(0));
        }

        ac.setBucketSpan(4L);
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false);
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.MULTIPLE_BUCKETSPANS_NOT_MULTIPLE.getValueString(), e.getHeader("errorCode").get(0));
            assertEquals(Messages.getMessage(Messages.JOB_CONFIG_MULTIPLE_BUCKETSPANS_MUST_BE_MULTIPLE, 10, 4), e.getMessage());
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
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.MULTIPLE_BUCKETSPANS_NOT_MULTIPLE.getValueString(), e.getHeader("errorCode").get(0));
            assertEquals(Messages.getMessage(Messages.JOB_CONFIG_MULTIPLE_BUCKETSPANS_MUST_BE_MULTIPLE, 222, 222), e.getMessage());
        }

        ac.setMultipleBucketSpans(Arrays.asList(-444L, -888L));
        try {
            AnalysisConfigVerifier.verify(ac);
            assertTrue(false);
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.MULTIPLE_BUCKETSPANS_NOT_MULTIPLE.getValueString(), e.getHeader("errorCode").get(0));
            assertEquals(Messages.getMessage(Messages.JOB_CONFIG_MULTIPLE_BUCKETSPANS_MUST_BE_MULTIPLE, -444, 222), e.getMessage());
        }
    }


    public void testVerify_GivenCategorizationFiltersButNoCategorizationFieldName() {

        AnalysisConfig config = createValidConfig();
        config.setCategorizationFilters(Arrays.asList("foo"));

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> AnalysisConfigVerifier.verify(config));

        assertEquals(ErrorCodes.CATEGORIZATION_FILTERS_REQUIRE_CATEGORIZATION_FIELD_NAME.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CATEGORIZATION_FILTERS_REQUIRE_CATEGORIZATION_FIELD_NAME), e.getMessage());
    }


    public void testVerify_GivenDuplicateCategorizationFilters() {

        AnalysisConfig config = createValidConfig();
        config.setCategorizationFieldName("myCategory");
        config.setCategorizationFilters(Arrays.asList("foo", "bar", "foo"));

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> AnalysisConfigVerifier.verify(config));

        assertEquals(ErrorCodes.CATEGORIZATION_FILTERS_CONTAIN_DUPLICATES.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CATEGORIZATION_FILTERS_CONTAINS_DUPLICATES), e.getMessage());
    }


    public void testVerify_GivenEmptyCategorizationFilter() {

        AnalysisConfig config = createValidConfig();
        config.setCategorizationFieldName("myCategory");
        config.setCategorizationFilters(Arrays.asList("foo", ""));

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> AnalysisConfigVerifier.verify(config));

        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CATEGORIZATION_FILTERS_CONTAINS_EMPTY), e.getMessage());
    }


    public void testCheckDetectorsHavePartitionFields() {

        AnalysisConfig config = createValidConfig();
        config.setUsePerPartitionNormalization(true);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> AnalysisConfigVerifier.verify(config));

        assertEquals(ErrorCodes.PER_PARTITION_NORMALIZATION_REQUIRES_PARTITION_FIELD.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_PER_PARTITION_NORMALIZATION_REQUIRES_PARTITION_FIELD), e.getMessage());
    }


    public void testCheckDetectorsHavePartitionFields_doesntThrowWhenValid() {
        AnalysisConfig config = createValidConfig();
        Detector.Builder builder = new Detector.Builder(config.getDetectors().get(0));
        builder.setPartitionFieldName("pField");
        config.getDetectors().set(0, builder.build());
        config.setUsePerPartitionNormalization(true);

        AnalysisConfigVerifier.verify(config);
    }


    public void testCheckNoInfluencersAreSet() {

        AnalysisConfig config = createValidConfig();
        Detector.Builder builder = new Detector.Builder(config.getDetectors().get(0));
        builder.setPartitionFieldName("pField");
        config.getDetectors().set(0, builder.build());
        config.setInfluencers(Arrays.asList("inf1", "inf2"));
        config.setUsePerPartitionNormalization(true);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> AnalysisConfigVerifier.verify(config));

        assertEquals(ErrorCodes.PER_PARTITION_NORMALIZATION_CANNOT_USE_INFLUENCERS.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_PER_PARTITION_NORMALIZATION_CANNOT_USE_INFLUENCERS), e.getMessage());
    }


    public void testVerify_GivenCategorizationFiltersContainInvalidRegex() {

        AnalysisConfig config = createValidConfig();
        config.setCategorizationFieldName("myCategory");
        config.setCategorizationFilters(Arrays.asList("foo", "("));

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> AnalysisConfigVerifier.verify(config));

        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CATEGORIZATION_FILTERS_CONTAINS_INVALID_REGEX, "("), e.getMessage());
    }

    private static AnalysisConfig createValidConfig() {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(3600L);
        analysisConfig.setBatchSpan(0L);
        analysisConfig.setLatency(0L);
        analysisConfig.setPeriod(0L);
        List<Detector> detectors = new ArrayList<>();
        Detector detector = new Detector.Builder("count", null).build();
        detectors.add(detector);
        analysisConfig.setDetectors(detectors);
        return analysisConfig;
    }
}
