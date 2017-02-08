/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.action;

import org.elasticsearch.xpack.ml.action.GetJobsAction.Response;
import org.elasticsearch.xpack.ml.action.util.QueryPage;
import org.elasticsearch.xpack.ml.job.config.AnalysisConfig;
import org.elasticsearch.xpack.ml.job.config.AnalysisLimits;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.config.Detector;
import org.elasticsearch.xpack.ml.job.config.IgnoreDowntime;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.config.ModelDebugConfig;
import org.elasticsearch.xpack.ml.support.AbstractStreamableTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class GetJobsActionResponseTests extends AbstractStreamableTestCase<GetJobsAction.Response> {

    @Override
    protected Response createTestInstance() {
        final Response result;

        int listSize = randomInt(10);
        List<Job> jobList = new ArrayList<>(listSize);
        for (int j = 0; j < listSize; j++) {
            String jobId = "job" + j;
            String description = randomBoolean() ? randomAsciiOfLength(100) : null;
            Date createTime = new Date(randomPositiveLong());
            Date finishedTime = randomBoolean() ? new Date(randomPositiveLong()) : null;
            Date lastDataTime = randomBoolean() ? new Date(randomPositiveLong()) : null;
            long timeout = randomPositiveLong();
            AnalysisConfig analysisConfig = new AnalysisConfig.Builder(
                    Collections.singletonList(new Detector.Builder("metric", "some_field").build())).build();
            AnalysisLimits analysisLimits = new AnalysisLimits(randomPositiveLong(), randomPositiveLong());
            DataDescription dataDescription = randomBoolean() ? new DataDescription.Builder().build() : null;
            ModelDebugConfig modelDebugConfig = randomBoolean() ? new ModelDebugConfig(randomDouble(), randomAsciiOfLength(10)) : null;
            IgnoreDowntime ignoreDowntime = randomFrom(IgnoreDowntime.values());
            Long normalizationWindowDays = randomBoolean() ? Long.valueOf(randomIntBetween(0, 365)) : null;
            Long backgroundPersistInterval = randomBoolean() ? Long.valueOf(randomIntBetween(3600, 86400)) : null;
            Long modelSnapshotRetentionDays = randomBoolean() ? Long.valueOf(randomIntBetween(0, 365)) : null;
            Long resultsRetentionDays = randomBoolean() ? Long.valueOf(randomIntBetween(0, 365)) : null;
            Map<String, Object> customConfig = randomBoolean() ? Collections.singletonMap(randomAsciiOfLength(10), randomAsciiOfLength(10))
                    : null;
            String modelSnapshotId = randomBoolean() ? randomAsciiOfLength(10) : null;
            String indexName = randomBoolean() ? "index" + j : null;
            Job job = new Job(jobId, description, createTime, finishedTime, lastDataTime,
                    analysisConfig, analysisLimits, dataDescription,
                    modelDebugConfig, ignoreDowntime, normalizationWindowDays, backgroundPersistInterval,
                    modelSnapshotRetentionDays, resultsRetentionDays, customConfig, modelSnapshotId, indexName, randomBoolean());

            jobList.add(job);
        }

        result = new Response(new QueryPage<>(jobList, jobList.size(), Job.RESULTS_FIELD));

        return result;
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
