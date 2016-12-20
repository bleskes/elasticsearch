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

import org.elasticsearch.xpack.prelert.action.GetJobsStatsAction.Response;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.JobStatus;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.scheduler.SchedulerStatus;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class GetJobsStatsActionResponseTests extends AbstractStreamableTestCase<Response> {

    @Override
    protected Response createTestInstance() {
        final Response result;

        int listSize = randomInt(10);
        List<Response.JobStats> jobStatsList = new ArrayList<>(listSize);
        for (int j = 0; j < listSize; j++) {
            String jobId = randomAsciiOfLength(10);

            DataCounts dataCounts = new DataCounts(randomAsciiOfLength(10), randomIntBetween(1, 1_000_000),
                    randomIntBetween(1, 1_000_000), randomIntBetween(1, 1_000_000), randomIntBetween(1, 1_000_000),
                    randomIntBetween(1, 1_000_000), randomIntBetween(1, 1_000_000), randomIntBetween(1, 1_000_000),
                    new DateTime(randomDateTimeZone()).toDate(), new DateTime(randomDateTimeZone()).toDate());

            ModelSizeStats sizeStats = null;
            if (randomBoolean()) {
                sizeStats = new ModelSizeStats.Builder("foo").build();
            }
            JobStatus jobStatus = randomFrom(EnumSet.allOf(JobStatus.class));

            Response.JobStats jobStats = new Response.JobStats(jobId, dataCounts, sizeStats, jobStatus);
            jobStatsList.add(jobStats);
        }

        result = new Response(new QueryPage<>(jobStatsList, jobStatsList.size(), Job.RESULTS_FIELD));

        return result;
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
