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

import org.elasticsearch.Version;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xpack.ml.action.GetJobsStatsAction.Response;
import org.elasticsearch.xpack.ml.action.util.QueryPage;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.config.JobState;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCounts;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCountsTests;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSizeStats;
import org.elasticsearch.xpack.ml.support.AbstractStreamableTestCase;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.elasticsearch.common.unit.TimeValue.parseTimeValue;

public class GetJobStatsActionResponseTests extends AbstractStreamableTestCase<Response> {

    @Override
    protected Response createTestInstance() {
        final Response result;

        int listSize = randomInt(10);
        List<Response.JobStats> jobStatsList = new ArrayList<>(listSize);
        for (int j = 0; j < listSize; j++) {
            String jobId = randomAsciiOfLength(10);

            DataCounts dataCounts = new DataCountsTests().createTestInstance();

            ModelSizeStats sizeStats = null;
            if (randomBoolean()) {
                sizeStats = new ModelSizeStats.Builder("foo").build();
            }
            JobState jobState = randomFrom(EnumSet.allOf(JobState.class));

            DiscoveryNode node = null;
            if (randomBoolean()) {
                node = new DiscoveryNode("_id", new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300), Version.CURRENT);
            }
            String explanation = null;
            if (randomBoolean()) {
                explanation = randomAsciiOfLength(3);
            }
            TimeValue openTime = null;
            if (randomBoolean()) {
                openTime = parseTimeValue(randomPositiveTimeValue(), "open_time-Test");
            }
            Response.JobStats jobStats = new Response.JobStats(jobId, dataCounts, sizeStats, jobState, node, explanation, openTime);
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
