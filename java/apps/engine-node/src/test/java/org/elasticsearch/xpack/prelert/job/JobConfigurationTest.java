package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.config.verification.JobConfigurationVerifier;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;
import org.elasticsearch.xpack.prelert.job.transform.TransformType;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class JobConfigurationTest extends AbstractSerializingTestCase<JobConfiguration> {

    @Override
    protected JobConfiguration createTestInstance() {
        JobConfiguration jobConfiguration = new JobConfiguration();
        jobConfiguration.setId(randomAsciiOfLength(10));
        if (randomBoolean()) {
            jobConfiguration.setDescription(randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            jobConfiguration.setTimeout(randomPositiveLong());
        }
        if (randomBoolean()) {
            AnalysisLimits analysisLimits = new AnalysisLimits(randomPositiveLong(), randomPositiveLong());
            jobConfiguration.setAnalysisLimits(analysisLimits);
        }
        if (randomBoolean()) {
            SchedulerConfig.Builder builder = new SchedulerConfig.Builder(SchedulerConfig.DataSource.ELASTICSEARCH);
            builder.setBaseUrl("http://localhost");
            builder.setIndexes(Arrays.asList("index"));
            builder.setTypes(Arrays.asList("type"));
            jobConfiguration.setSchedulerConfig(builder);
        }
        if (randomBoolean()) {
            jobConfiguration.setDataDescription(new DataDescription());
        }
        if (randomBoolean()) {
            int numTransformers = randomIntBetween(0, 32);
            List<TransformConfig> transformConfigList = new ArrayList<>(numTransformers);
            for (int i = 0; i < numTransformers; i++) {
                transformConfigList.add(new TransformConfig(TransformType.UPPERCASE.prettyName()));
            }
            jobConfiguration.setTransforms(transformConfigList);
        }
        if (randomBoolean()) {
            jobConfiguration.setModelDebugConfig(new ModelDebugConfig(randomDouble(), randomAsciiOfLength(10)));
        }
        if (randomBoolean()) {
            jobConfiguration.setIgnoreDowntime(randomFrom(IgnoreDowntime.values()));
        }
        if (randomBoolean()) {
            jobConfiguration.setRenormalizationWindowDays(randomLong());
        }
        if (randomBoolean()) {
            jobConfiguration.setBackgroundPersistInterval(randomLong());
        }
        if (randomBoolean()) {
            jobConfiguration.setModelSnapshotRetentionDays(randomLong());
        }
        if (randomBoolean()) {
            jobConfiguration.setResultsRetentionDays(randomLong());
        }
        if (randomBoolean()) {
            jobConfiguration.setCustomSettings(Collections.singletonMap(randomAsciiOfLength(10), randomAsciiOfLength(10)));
        }
        return jobConfiguration;
    }

    @Override
    protected Writeable.Reader<JobConfiguration> instanceReader() {
        return JobConfiguration::new;
    }

    @Override
    protected JobConfiguration parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return JobConfiguration.PARSER.apply(parser, () -> matcher);
    }

    /**
     * Test the {@link AnalysisConfig#analysisFields()} method which produces a
     * list of analysis fields from the detectors
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
        assertEquals("Unexpected id length: " + id, JobConfigurationVerifier.MAX_JOB_ID_LENGTH, id.length());
        assertTrue(
                "Unexpected id ending: " + id + ", expected ending: "
                        + String.format(Locale.ROOT, "%05d", JobConfiguration.ID_SEQUENCE.get()),
                        id.endsWith(String.format(Locale.ROOT, "%05d", JobConfiguration.ID_SEQUENCE.get())));
    }

    public void testGenerateJobId_isShorterThanMaxHJobLength_withLongHostname_andSixDigitSequence() {
        String id = null;
        for (int i = 0; i < 100000; i++) {
            id = JobConfiguration.generateJobId("averyverylongstringthatcouldbeahostnameorfullyqualifieddomainname");
        }
        assertTrue(id.endsWith("-" + JobConfiguration.ID_SEQUENCE.get()));
        assertEquals(JobConfigurationVerifier.MAX_JOB_ID_LENGTH, id.length());
    }

    public void testSetAnalysisLimits() {
        JobConfiguration jobConfiguration = new JobConfiguration();
        jobConfiguration.setAnalysisLimits(new AnalysisLimits(42L, null));
        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class,
                () -> jobConfiguration.setAnalysisLimits(new AnalysisLimits(41L, null)));
        assertEquals("Invalid update value for analysisLimits: modelMemoryLimit cannot be decreased; existing is 42, update had 41",
                e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }
}
