
package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig.DataSource;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;
import org.elasticsearch.xpack.prelert.job.transform.TransformType;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class JobDetailsTest extends AbstractSerializingTestCase<JobDetails> {

    @Override
    protected JobDetails createTestInstance() {
        String jobId = randomAsciiOfLength(10);
        String description = randomBoolean() ? randomAsciiOfLength(10) : null;
        JobStatus jobStatus = randomFrom(JobStatus.values());
        JobSchedulerStatus jobSchedulerStatus = randomFrom(JobSchedulerStatus.values());
        Date createTime = new Date(randomPositiveLong());
        Date finishedTime = randomBoolean() ? new Date(randomPositiveLong()) : null;
        Date lastDataTime = randomBoolean() ? new Date(randomPositiveLong()) : null;
        long timeout = randomPositiveLong();
        AnalysisConfig analysisConfig = new AnalysisConfig();
        AnalysisLimits analysisLimits = new AnalysisLimits(randomPositiveLong(), randomPositiveLong());
        SchedulerConfig schedulerConfig = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH).build();
        DataDescription dataDescription = randomBoolean() ? new DataDescription() : null;
        ModelSizeStats modelSizeStats = randomBoolean() ? new ModelSizeStats() : null;
        int numTransformers = randomIntBetween(0, 32);
        List<TransformConfig> transformConfigList = new ArrayList<>(numTransformers);
        for (int i = 0; i < numTransformers; i++) {
            transformConfigList.add(new TransformConfig(TransformType.UPPERCASE.prettyName()));
        }
        ModelDebugConfig modelDebugConfig = randomBoolean() ? new ModelDebugConfig(randomDouble(), randomAsciiOfLength(10)) : null;
        DataCounts counts = randomBoolean() ? new DataCounts() : null;
        IgnoreDowntime ignoreDowntime = randomFrom(IgnoreDowntime.values());
        Long normalizationWindowDays = randomBoolean() ? randomLong() : null;
        Long backgroundPersistInterval = randomBoolean() ? randomLong() : null;
        Long modelSnapshotRetentionDays = randomBoolean() ? randomLong() : null;
        Long resultsRetentionDays = randomBoolean() ? randomLong() : null;
        Map<String, Object> customConfig =
                randomBoolean() ? Collections.singletonMap(randomAsciiOfLength(10), randomAsciiOfLength(10)) : null;
        Double averageBucketProcessingTimeMs = randomBoolean() ? randomDouble() : null;
        return new JobDetails(
                jobId, description, jobStatus, jobSchedulerStatus, createTime, finishedTime, lastDataTime, timeout,
                analysisConfig, analysisLimits, schedulerConfig, dataDescription, modelSizeStats, transformConfigList,
                modelDebugConfig, counts, ignoreDowntime, normalizationWindowDays, backgroundPersistInterval,
                modelSnapshotRetentionDays, resultsRetentionDays, customConfig, averageBucketProcessingTimeMs

        );
    }

    @Override
    protected Writeable.Reader<JobDetails> instanceReader() {
        return JobDetails::new;
    }

    @Override
    protected JobDetails parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return JobDetails.PARSER.apply(parser, () -> matcher);
    }

    public void testConstructor_GivenEmptyJobConfiguration() {
        JobDetails jobDetails = new JobConfiguration("foo").build();

        assertEquals("foo", jobDetails.getId());
        assertEquals(JobStatus.CLOSED, jobDetails.getStatus());
        assertNotNull(jobDetails.getCreateTime());
        assertEquals(600L, jobDetails.getTimeout());
        assertNotNull(jobDetails.getSchedulerStatus());
        assertNotNull(jobDetails.getAnalysisConfig());
        assertNull(jobDetails.getAnalysisLimits());
        assertNull(jobDetails.getCustomSettings());
        assertNull(jobDetails.getDataDescription());
        assertNull(jobDetails.getDescription());
        assertNull(jobDetails.getFinishedTime());
        assertNull(jobDetails.getIgnoreDowntime());
        assertNull(jobDetails.getLastDataTime());
        assertNull(jobDetails.getModelDebugConfig());
        assertNull(jobDetails.getModelSizeStats());
        assertNull(jobDetails.getRenormalizationWindowDays());
        assertNull(jobDetails.getBackgroundPersistInterval());
        assertNull(jobDetails.getModelSnapshotRetentionDays());
        assertNull(jobDetails.getResultsRetentionDays());
        assertNull(jobDetails.getSchedulerConfig());
        assertNull(jobDetails.getTransforms());
        assertNotNull(jobDetails.allFields());
        assertTrue(jobDetails.allFields().isEmpty());
    }


    public void testConstructor_GivenJobConfigurationWithIgnoreDowntime() {
        JobConfiguration jobConfiguration = new JobConfiguration("foo");
        jobConfiguration.setIgnoreDowntime(IgnoreDowntime.ONCE);
        JobDetails jobDetails = jobConfiguration.build();

        assertEquals("foo", jobDetails.getId());
        assertEquals(IgnoreDowntime.ONCE, jobDetails.getIgnoreDowntime());
    }


    public void testConstructor_GivenJobConfigurationWithElasticsearchScheduler_ShouldFillDefaults() {
        SchedulerConfig.Builder schedulerConfig = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        expectThrows(NullPointerException.class, () ->schedulerConfig.setQuery(null));
    }


    public void testEquals_GivenSameReference() {
        JobDetails jobDetails = new JobConfiguration().build();
        assertTrue(jobDetails.equals(jobDetails));
    }


    public void testEquals_GivenDifferentClass() {
        JobDetails jobDetails = new JobConfiguration().build();
        assertFalse(jobDetails.equals("a string"));
    }


    public void testEquals_GivenEqualJobDetails() throws URISyntaxException {
        ModelSizeStats modelSizeStats = new ModelSizeStats();
        Map<String, URI> endpoints = new HashMap<>();
        endpoints.put("buckets", new URI("http://localhost:8080/buckets"));

        JobDetails jobDetails1 = new JobConfiguration().build();
        jobDetails1.setId("foo");
        jobDetails1.setAnalysisConfig(new AnalysisConfig());
        jobDetails1.setAnalysisLimits(new AnalysisLimits(0L, null));
        jobDetails1.setCounts(new DataCounts());
        jobDetails1.setCreateTime(new Date(0));
        jobDetails1.setCustomSettings(new HashMap<>());
        jobDetails1.setDataDescription(new DataDescription());
        jobDetails1.setDescription("Blah blah");
        jobDetails1.setFinishedTime(new Date(1000));
        jobDetails1.setIgnoreDowntime(IgnoreDowntime.ALWAYS);
        jobDetails1.setLastDataTime(new Date(500));
        jobDetails1.setModelDebugConfig(new ModelDebugConfig(1.0, null));
        jobDetails1.setModelSizeStats(modelSizeStats);
        jobDetails1.setRenormalizationWindowDays(60L);
        jobDetails1.setBackgroundPersistInterval(10000L);
        jobDetails1.setModelSnapshotRetentionDays(10L);
        jobDetails1.setResultsRetentionDays(30L);
        jobDetails1.setSchedulerConfig(new SchedulerConfig.Builder(DataSource.FILE).build());
        jobDetails1.setSchedulerStatus(JobSchedulerStatus.STOPPED);
        jobDetails1.setStatus(JobStatus.RUNNING);
        jobDetails1.setTimeout(3600L);
        jobDetails1.setTransforms(Collections.emptyList());


        JobDetails jobDetails2 = new JobConfiguration().build();
        jobDetails2.setId("foo");
        jobDetails2.setAnalysisConfig(new AnalysisConfig());
        jobDetails2.setAnalysisLimits(new AnalysisLimits(0L, null));
        jobDetails2.setCounts(new DataCounts());
        jobDetails2.setCreateTime(new Date(0));
        jobDetails2.setCustomSettings(new HashMap<>());
        jobDetails2.setDataDescription(new DataDescription());
        jobDetails2.setDescription("Blah blah");
        jobDetails2.setFinishedTime(new Date(1000));
        jobDetails2.setIgnoreDowntime(IgnoreDowntime.ALWAYS);
        jobDetails2.setLastDataTime(new Date(500));
        jobDetails2.setModelDebugConfig(new ModelDebugConfig(1.0, null));
        jobDetails2.setModelSizeStats(modelSizeStats);
        jobDetails2.setRenormalizationWindowDays(60L);
        jobDetails2.setBackgroundPersistInterval(10000L);
        jobDetails2.setModelSnapshotRetentionDays(10L);
        jobDetails2.setResultsRetentionDays(30L);
        jobDetails2.setSchedulerConfig(new SchedulerConfig.Builder(DataSource.FILE).build());
        jobDetails2.setSchedulerStatus(JobSchedulerStatus.STOPPED);
        jobDetails2.setStatus(JobStatus.RUNNING);
        jobDetails2.setTimeout(3600L);
        jobDetails2.setTransforms(Collections.emptyList());

        assertTrue(jobDetails1.equals(jobDetails2));
        assertTrue(jobDetails2.equals(jobDetails1));
        assertEquals(jobDetails1.hashCode(), jobDetails2.hashCode());
    }


    public void testEquals_GivenDifferentIds() {
        JobConfiguration jobConfiguration = new JobConfiguration("foo");
        JobDetails jobDetails1 = jobConfiguration.build();
        jobConfiguration.setId("bar");
        JobDetails jobDetails2 = jobConfiguration.build();
        Date createTime = new Date();
        jobDetails1.setCreateTime(createTime);
        jobDetails2.setCreateTime(createTime);

        assertFalse(jobDetails1.equals(jobDetails2));
    }


    public void testEquals_GivenDifferentSchedulerStatus() {
        JobConfiguration jobConfiguration = new JobConfiguration("foo");
        JobDetails jobDetails1 = jobConfiguration.build();
        jobDetails1.setSchedulerStatus(JobSchedulerStatus.STOPPED);
        jobConfiguration.setId("bar");
        JobDetails jobDetails2 = jobConfiguration.build();
        jobDetails2.setSchedulerStatus(JobSchedulerStatus.STARTED);

        assertFalse(jobDetails1.equals(jobDetails2));
    }


    public void testEquals_GivenDifferentRenormalizationWindowDays() {
        JobConfiguration jobDetails1 = new JobConfiguration();
        jobDetails1.setRenormalizationWindowDays(3L);
        JobConfiguration jobDetails2 = new JobConfiguration();
        jobDetails2.setRenormalizationWindowDays(4L);

        assertFalse(jobDetails1.equals(jobDetails2));
    }


    public void testEquals_GivenDifferentBackgroundPersistInterval() {
        JobConfiguration jobDetails1 = new JobConfiguration();
        jobDetails1.setBackgroundPersistInterval(10000L);
        JobConfiguration jobDetails2 = new JobConfiguration();
        jobDetails2.setBackgroundPersistInterval(8000L);

        assertFalse(jobDetails1.equals(jobDetails2));
    }


    public void testEquals_GivenDifferentModelSnapshotRetentionDays() {
        JobConfiguration jobDetails1 = new JobConfiguration();
        jobDetails1.setModelSnapshotRetentionDays(10L);
        JobConfiguration jobDetails2 = new JobConfiguration();
        jobDetails2.setModelSnapshotRetentionDays(8L);

        assertFalse(jobDetails1.equals(jobDetails2));
    }


    public void testEquals_GivenDifferentResultsRetentionDays() {
        JobConfiguration jobDetails1 = new JobConfiguration();
        jobDetails1.setResultsRetentionDays(30L);
        JobConfiguration jobDetails2 = new JobConfiguration();
        jobDetails2.setResultsRetentionDays(4L);

        assertFalse(jobDetails1.equals(jobDetails2));
    }


    public void testEquals_GivenDifferentCustomSettings() {
        JobConfiguration jobDetails1 = new JobConfiguration();
        Map<String, Object> customSettings1 = new HashMap<>();
        customSettings1.put("key1", "value1");
        jobDetails1.setCustomSettings(customSettings1);
        JobConfiguration jobDetails2 = new JobConfiguration();
        Map<String, Object> customSettings2 = new HashMap<>();
        customSettings2.put("key2", "value2");
        jobDetails2.setCustomSettings(customSettings2);

        assertFalse(jobDetails1.equals(jobDetails2));
    }


    public void testEquals_GivenDifferentIgnoreDowntime() {
        JobDetails job1 = new JobConfiguration().build();
        job1.setIgnoreDowntime(IgnoreDowntime.NEVER);
        JobDetails job2 = new JobConfiguration().build();
        job2.setIgnoreDowntime(IgnoreDowntime.ONCE);

        assertFalse(job1.equals(job2));
        assertFalse(job2.equals(job1));
    }
}
