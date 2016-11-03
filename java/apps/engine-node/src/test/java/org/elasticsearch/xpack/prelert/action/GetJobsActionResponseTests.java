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

import org.elasticsearch.xpack.prelert.action.GetJobsAction.Response;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.xpack.prelert.job.JobDetailsTests.buildJobBuilder;
import static org.elasticsearch.xpack.prelert.job.JobDetailsTests.randomValidJobId;
import static org.elasticsearch.xpack.prelert.job.JobDetailsTests.createAnalysisConfig;

public class GetJobsActionResponseTests extends AbstractStreamableTestCase<GetJobsAction.Response> {

    @Override
    protected Response createTestInstance() {
        int listSize = randomInt(10);
        List<JobDetails> hits = new ArrayList<>(listSize);
        for (int j = 0; j < listSize; j++) {
            hits.add(buildJobBuilder(randomValidJobId()).build());
        }
        QueryPage<JobDetails> buckets = new QueryPage<>(hits, listSize);
        return new Response(buckets);
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
