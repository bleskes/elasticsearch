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

import org.elasticsearch.xpack.prelert.action.GetSchedulersStatsAction.Response;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.scheduler.Scheduler;
import org.elasticsearch.xpack.prelert.scheduler.SchedulerStatus;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

import java.util.ArrayList;
import java.util.List;

public class GetSchedulersStatsActionResponseTests extends AbstractStreamableTestCase<Response> {

    @Override
    protected Response createTestInstance() {
        final Response result;

        int listSize = randomInt(10);
        List<Response.SchedulerStats> schedulerStatsList = new ArrayList<>(listSize);
        for (int j = 0; j < listSize; j++) {
            String schedulerId = randomAsciiOfLength(10);
            SchedulerStatus schedulerStatus = randomFrom(SchedulerStatus.values());

            Response.SchedulerStats schedulerStats = new Response.SchedulerStats(schedulerId, schedulerStatus);
            schedulerStatsList.add(schedulerStats);
        }

        result = new Response(new QueryPage<>(schedulerStatsList, schedulerStatsList.size(), Scheduler.RESULTS_FIELD));

        return result;
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
