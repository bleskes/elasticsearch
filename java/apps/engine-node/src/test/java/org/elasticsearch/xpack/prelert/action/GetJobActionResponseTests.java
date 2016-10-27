package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.action.GetJobAction.Response;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.AnalysisLimits;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.IgnoreDowntime;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.JobSchedulerStatus;
import org.elasticsearch.xpack.prelert.job.JobStatus;
import org.elasticsearch.xpack.prelert.job.ModelDebugConfig;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.SchedulerState;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;
import org.elasticsearch.xpack.prelert.job.transform.TransformType;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;
import org.elasticsearch.xpack.prelert.utils.SingleDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class GetJobActionResponseTests extends AbstractStreamableTestCase<GetJobAction.Response> {

    @Override
    protected Response createTestInstance() {
        final SingleDocument<JobDetails> result;
        if (randomBoolean()) {
            result = SingleDocument.empty(JobDetails.TYPE);
        } else {
            String jobId = randomAsciiOfLength(10);
            String description = randomBoolean() ? randomAsciiOfLength(10) : null;
            JobStatus jobStatus = randomFrom(JobStatus.values());
            SchedulerState jobSchedulerState = new SchedulerState(randomFrom(JobSchedulerStatus.values()), randomPositiveLong(),
                    randomPositiveLong());
            Date createTime = new Date(randomPositiveLong());
            Date finishedTime = randomBoolean() ? new Date(randomPositiveLong()) : null;
            Date lastDataTime = randomBoolean() ? new Date(randomPositiveLong()) : null;
            long timeout = randomPositiveLong();
            AnalysisConfig analysisConfig = new AnalysisConfig();
            AnalysisLimits analysisLimits = new AnalysisLimits(randomPositiveLong(), randomPositiveLong());
            SchedulerConfig.Builder schedulerConfig = new SchedulerConfig.Builder(SchedulerConfig.DataSource.FILE);
            schedulerConfig.setFilePath("/file/path");
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
            Map<String, Object> customConfig = randomBoolean() ? Collections.singletonMap(randomAsciiOfLength(10), randomAsciiOfLength(10))
                    : null;
            Double averageBucketProcessingTimeMs = randomBoolean() ? randomDouble() : null;
            JobDetails job = new JobDetails(jobId, description, jobStatus, jobSchedulerState, createTime, finishedTime, lastDataTime,
                    timeout, analysisConfig, analysisLimits, schedulerConfig.build(), dataDescription, modelSizeStats, transformConfigList,
                    modelDebugConfig, counts, ignoreDowntime, normalizationWindowDays, backgroundPersistInterval,
                    modelSnapshotRetentionDays, resultsRetentionDays, customConfig, averageBucketProcessingTimeMs);
            result = new SingleDocument<JobDetails>(JobDetails.TYPE, job);
        }
        return new Response(result);
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
