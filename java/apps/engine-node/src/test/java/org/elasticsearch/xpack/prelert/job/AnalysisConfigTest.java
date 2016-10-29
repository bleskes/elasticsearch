
package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.detectionrules.DetectionRule;
import org.elasticsearch.xpack.prelert.job.detectionrules.RuleCondition;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


public class AnalysisConfigTest extends AbstractSerializingTestCase<AnalysisConfig> {

    @Override
    protected AnalysisConfig createTestInstance() {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        if (randomBoolean()) {
            analysisConfig.setBatchSpan(randomLong());
        }
        if (randomBoolean()) {
            analysisConfig.setBucketSpan(randomLong());
        }
        if (randomBoolean()) {
            analysisConfig.setCategorizationFieldName(randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            analysisConfig.setCategorizationFilters(Arrays.asList(generateRandomStringArray(10, 10, false)));
        }
        if (randomBoolean()) {
            List<Detector> detectors = new ArrayList<>();
            int numDetectors = randomIntBetween(0, 10);
            for (int i = 0; i < numDetectors; i++) {
                detectors.add(new Detector("_function" + i));
            }
            analysisConfig.setDetectors(detectors);
        }
        if (randomBoolean()) {
            analysisConfig.setInfluencers(Arrays.asList(generateRandomStringArray(10, 10, false)));
        }
        if (randomBoolean()) {
            analysisConfig.setLatency(randomLong());
        }
        if (randomBoolean()) {
            int numBucketSpans = randomIntBetween(0, 10);
            List<Long> multipleBucketSpans = new ArrayList<>();
            for (int i = 0; i < numBucketSpans; i++) {
                multipleBucketSpans.add(randomLong());
            }
            analysisConfig.setMultipleBucketSpans(multipleBucketSpans);
        }
        if (randomBoolean()) {
            analysisConfig.setMultivariateByFields(randomBoolean());
        }
        if (randomBoolean()) {
            analysisConfig.setOverlappingBuckets(randomBoolean());
        }
        if (randomBoolean()) {
            analysisConfig.setResultFinalizationWindow(randomLong());
        }
        if (randomBoolean()) {
            analysisConfig.setUsePerPartitionNormalization(randomBoolean());
        }
        return analysisConfig;
    }

    @Override
    protected Writeable.Reader<AnalysisConfig> instanceReader() {
        return AnalysisConfig::new;
    }

    @Override
    protected AnalysisConfig parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return AnalysisConfig.PARSER.apply(parser, () -> matcher);
    }

    public void testFieldConfiguration() {
        // Single detector, not pre-summarised
        AnalysisConfig ac = new AnalysisConfig();
        Detector det = new Detector("metric", "responsetime");
        det.setByFieldName("airline");
        det.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(det));

        Set<String> termFields = new TreeSet<String>(Arrays.asList(new String[]{
                "airline", "sourcetype"}));
        Set<String> analysisFields = new TreeSet<String>(Arrays.asList(new String[]{
                "responsetime", "airline", "sourcetype"}));

        assertEquals(termFields.size(), ac.termFields().size());
        assertEquals(analysisFields.size(), ac.analysisFields().size());

        for (String s : ac.termFields()) {
            assertTrue(termFields.contains(s));
        }

        for (String s : termFields) {
            assertTrue(ac.termFields().contains(s));
        }

        for (String s : ac.analysisFields()) {
            assertTrue(analysisFields.contains(s));
        }

        for (String s : analysisFields) {
            assertTrue(ac.analysisFields().contains(s));
        }

        assertEquals(1, ac.fields().size());
        assertTrue(ac.fields().contains("responsetime"));

        assertEquals(1, ac.byFields().size());
        assertTrue(ac.byFields().contains("airline"));

        assertEquals(1, ac.partitionFields().size());
        assertTrue(ac.partitionFields().contains("sourcetype"));

        assertNull(ac.getSummaryCountFieldName());

        // Single detector, pre-summarised
        analysisFields.add("summaryCount");
        ac.setSummaryCountFieldName("summaryCount");

        for (String s : ac.analysisFields()) {
            assertTrue(analysisFields.contains(s));
        }

        for (String s : analysisFields) {
            assertTrue(ac.analysisFields().contains(s));
        }

        assertEquals("summaryCount", ac.getSummaryCountFieldName());

        // Multiple detectors, not pre-summarised
        List<Detector> detectors = new ArrayList<>();

        ac = new AnalysisConfig();
        ac.setInfluencers(Arrays.asList("Influencer_Field"));
        det = new Detector("metric", "metric1");
        det.setByFieldName("by_one");
        det.setPartitionFieldName("partition_one");
        detectors.add(det);

        det = new Detector("metric", "metric2");
        det.setByFieldName("by_two");
        det.setOverFieldName("over_field");
        detectors.add(det);

        det = new Detector("metric", "metric2");
        det.setByFieldName("by_two");
        det.setPartitionFieldName("partition_two");
        detectors.add(det);

        ac.setDetectors(detectors);

        termFields = new TreeSet<String>(Arrays.asList(new String[]{
                "by_one", "by_two", "over_field",
                "partition_one", "partition_two", "Influencer_Field"}));
        analysisFields = new TreeSet<String>(Arrays.asList(new String[]{
                "metric1", "metric2", "by_one", "by_two", "over_field",
                "partition_one", "partition_two", "Influencer_Field"}));

        assertEquals(termFields.size(), ac.termFields().size());
        assertEquals(analysisFields.size(), ac.analysisFields().size());

        for (String s : ac.termFields()) {
            assertTrue(termFields.contains(s));
        }

        for (String s : termFields) {
            assertTrue(ac.termFields().contains(s));
        }

        for (String s : ac.analysisFields()) {
            assertTrue(analysisFields.contains(s));
        }

        for (String s : analysisFields) {
            assertTrue(ac.analysisFields().contains(s));
        }

        assertEquals(2, ac.fields().size());
        assertTrue(ac.fields().contains("metric1"));
        assertTrue(ac.fields().contains("metric2"));

        assertEquals(2, ac.byFields().size());
        assertTrue(ac.byFields().contains("by_one"));
        assertTrue(ac.byFields().contains("by_two"));

        assertEquals(1, ac.overFields().size());
        assertTrue(ac.overFields().contains("over_field"));

        assertEquals(2, ac.partitionFields().size());
        assertTrue(ac.partitionFields().contains("partition_one"));
        assertTrue(ac.partitionFields().contains("partition_two"));

        assertNull(ac.getSummaryCountFieldName());

        // Multiple detectors, pre-summarised
        analysisFields.add("summaryCount");
        ac.setSummaryCountFieldName("summaryCount");

        for (String s : ac.analysisFields()) {
            assertTrue(analysisFields.contains(s));
        }

        for (String s : analysisFields) {
            assertTrue(ac.analysisFields().contains(s));
        }

        assertEquals("summaryCount", ac.getSummaryCountFieldName());

        ac = new AnalysisConfig();
        ac.setBucketSpan(1000L);
        ac.setMultipleBucketSpans(Arrays.asList(5000L, 10000L, 24000L));
        assertTrue(ac.getMultipleBucketSpans().contains(5000L));
        assertTrue(ac.getMultipleBucketSpans().contains(10000L));
        assertTrue(ac.getMultipleBucketSpans().contains(24000L));
    }


    public void testEquals_GivenSameReference() {
        AnalysisConfig config = new AnalysisConfig();
        assertTrue(config.equals(config));
    }


    public void testEquals_GivenDifferentClass() {
        assertFalse(new AnalysisConfig().equals("a string"));
    }


    public void testEquals_GivenNull() {
        assertFalse(new AnalysisConfig().equals(null));
    }


    public void testEquals_GivenEqualConfig() {
        AnalysisConfig config1 = createFullyPopulatedConfig();
        AnalysisConfig config2 = createFullyPopulatedConfig();

        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config1));
        assertEquals(config1.hashCode(), config2.hashCode());
    }


    public void testEquals_GivenDifferentBatchSpan() {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setBatchSpan(86400L);

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setBatchSpan(0L);

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }


    public void testEquals_GivenDifferentBucketSpan() {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setBucketSpan(1800L);

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setBucketSpan(3600L);

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }


    public void testEquals_GivenCategorizationField() {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setCategorizationFieldName("foo");

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setCategorizationFieldName("bar");

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }


    public void testEquals_GivenDifferentDetector() {
        AnalysisConfig config1 = new AnalysisConfig();
        Detector detector1 = new Detector("foo", "low_count");
        config1.setDetectors(Arrays.asList(detector1));

        AnalysisConfig config2 = new AnalysisConfig();
        Detector detector2 = new Detector("foo", "high_count");
        config2.setDetectors(Arrays.asList(detector2));

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }


    public void testEquals_GivenDifferentInfluencers() {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setInfluencers(Arrays.asList("foo"));

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setInfluencers(Arrays.asList("bar"));

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }


    public void testEquals_GivenDifferentLatency() {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setLatency(1800L);

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setLatency(3600L);

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }


    public void testEquals_GivenDifferentPeriod() {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setPeriod(1800L);

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setPeriod(3600L);

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }


    public void testEquals_GivenSummaryCountField() {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setSummaryCountFieldName("foo");

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setSummaryCountFieldName("bar");

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }


    public void testEquals_GivenMultivariateByField() {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setMultivariateByFields(true);

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setMultivariateByFields(false);

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }


    public void testEquals_GivenDifferentCategorizationFilters() {
        AnalysisConfig config1 = createFullyPopulatedConfig();
        AnalysisConfig config2 = createFullyPopulatedConfig();
        config2.setCategorizationFilters(Arrays.asList("foo", "bar"));

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }


    public void testBucketSpanOrDefault() {
        AnalysisConfig config1 = new AnalysisConfig();
        assertEquals(AnalysisConfig.DEFAULT_BUCKET_SPAN, config1.getBucketSpanOrDefault());
        config1.setBucketSpan(100L);
        assertEquals(100L, config1.getBucketSpanOrDefault());
    }


    public void testExtractReferencedLists() {
        DetectionRule rule1 = new DetectionRule();
        rule1.setRuleConditions(Arrays.asList(RuleCondition.createCategorical("foo", "list1")));
        DetectionRule rule2 = new DetectionRule();
        rule2.setRuleConditions(Arrays.asList(RuleCondition.createCategorical("foo", "list2")));
        Detector detector1 = new Detector("metric", "foo1");
        detector1.setDetectorRules(Arrays.asList(rule1));
        Detector detector2 = new Detector("metric", "foo2");
        detector2.setDetectorRules(Arrays.asList(rule2));
        AnalysisConfig config = new AnalysisConfig();
        config.setDetectors(Arrays.asList(detector1, detector2, new Detector("metric", "foo3")));

        assertEquals(new HashSet<String>(Arrays.asList("list1", "list2")), config.extractReferencedLists());
    }

    private static AnalysisConfig createFullyPopulatedConfig() {
        AnalysisConfig config = new AnalysisConfig();
        config.setBatchSpan(86400L);
        config.setBucketSpan(3600L);
        config.setCategorizationFieldName("cat");
        config.setCategorizationFilters(Arrays.asList("foo"));
        Detector detector1 = new Detector("foo", "count");
        config.setDetectors(Arrays.asList(detector1));
        config.setInfluencers(Arrays.asList("myInfluencer"));
        config.setLatency(3600L);
        config.setPeriod(100L);
        config.setSummaryCountFieldName("sumCount");
        return config;
    }
}
