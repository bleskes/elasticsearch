/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.config.JobStatus;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCounts;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.elasticsearch.xpack.ml.action.GetJobsStatsAction.TransportAction.determineJobIdsWithoutLiveStats;

public class GetJobsStatsActionTests extends ESTestCase {

    public void testDetermineJobIds() {
        List<String> result = determineJobIdsWithoutLiveStats(Collections.singletonList("id1"), Collections.emptyList());
        assertEquals(1, result.size());
        assertEquals("id1", result.get(0));

        result = determineJobIdsWithoutLiveStats(Collections.singletonList("id1"), Collections.singletonList(
                new GetJobsStatsAction.Response.JobStats("id1", new DataCounts("id1"), null, JobStatus.CLOSED)));
        assertEquals(0, result.size());

        result = determineJobIdsWithoutLiveStats(
                Arrays.asList("id1", "id2", "id3"), Collections.emptyList());
        assertEquals(3, result.size());
        assertEquals("id1", result.get(0));
        assertEquals("id2", result.get(1));
        assertEquals("id3", result.get(2));

        result = determineJobIdsWithoutLiveStats(
                Arrays.asList("id1", "id2", "id3"),
                Collections.singletonList(new GetJobsStatsAction.Response.JobStats("id1", new DataCounts("id1"), null, JobStatus.CLOSED))
        );
        assertEquals(2, result.size());
        assertEquals("id2", result.get(0));
        assertEquals("id3", result.get(1));

        result = determineJobIdsWithoutLiveStats(Arrays.asList("id1", "id2", "id3"), Arrays.asList(
                new GetJobsStatsAction.Response.JobStats("id1", new DataCounts("id1"), null, JobStatus.CLOSED),
                new GetJobsStatsAction.Response.JobStats("id3", new DataCounts("id3"), null, JobStatus.CLOSED)
        ));
        assertEquals(1, result.size());
        assertEquals("id2", result.get(0));

        result = determineJobIdsWithoutLiveStats(Arrays.asList("id1", "id2", "id3"),
                Arrays.asList(new GetJobsStatsAction.Response.JobStats("id1", new DataCounts("id1"), null, JobStatus.CLOSED),
                    new GetJobsStatsAction.Response.JobStats("id2", new DataCounts("id2"), null, JobStatus.CLOSED),
                    new GetJobsStatsAction.Response.JobStats("id3", new DataCounts("id3"), null, JobStatus.CLOSED)));
        assertEquals(0, result.size());
    }

}
