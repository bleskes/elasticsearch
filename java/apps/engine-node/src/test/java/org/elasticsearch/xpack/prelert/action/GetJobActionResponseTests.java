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
package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.action.GetJobAction.Response;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.AnalysisLimits;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.IgnoreDowntime;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.ModelDebugConfig;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
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
        final SingleDocument<Job> result;
        if (randomBoolean()) {
            result = SingleDocument.empty(Job.TYPE);
        } else {
            String jobId = randomAsciiOfLength(10);
            String description = randomBoolean() ? randomAsciiOfLength(10) : null;
            Date createTime = new Date(randomPositiveLong());
            Date finishedTime = randomBoolean() ? new Date(randomPositiveLong()) : null;
            Date lastDataTime = randomBoolean() ? new Date(randomPositiveLong()) : null;
            long timeout = randomPositiveLong();
            AnalysisConfig analysisConfig = new AnalysisConfig();
            AnalysisLimits analysisLimits = new AnalysisLimits(randomPositiveLong(), randomPositiveLong());
            SchedulerConfig.Builder schedulerConfig = new SchedulerConfig.Builder(SchedulerConfig.DataSource.FILE);
            schedulerConfig.setFilePath("/file/path");
            DataDescription dataDescription = randomBoolean() ? new DataDescription.Builder().build() : null;
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
            String modelSnapshotId = randomBoolean() ? randomAsciiOfLength(10) : null;
            Job job = new Job(jobId, description, createTime, finishedTime, lastDataTime,
                    timeout, analysisConfig, analysisLimits, schedulerConfig.build(), dataDescription, modelSizeStats, transformConfigList,
                    modelDebugConfig, counts, ignoreDowntime, normalizationWindowDays, backgroundPersistInterval,
                    modelSnapshotRetentionDays, resultsRetentionDays, customConfig, averageBucketProcessingTimeMs, modelSnapshotId);
            result = new SingleDocument<Job>(Job.TYPE, job);
        }
        return new Response(result);
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
