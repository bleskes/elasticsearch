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
package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.action.GetSchedulersAction.Response;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.scheduler.Scheduler;
import org.elasticsearch.xpack.prelert.scheduler.SchedulerConfig;
import org.elasticsearch.xpack.prelert.scheduler.SchedulerConfigTests;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GetSchedulersActionResponseTests extends AbstractStreamableTestCase<Response> {

    @Override
    protected Response createTestInstance() {
        final Response result;

        int listSize = randomInt(10);
        List<SchedulerConfig> schedulerList = new ArrayList<>(listSize);
        for (int j = 0; j < listSize; j++) {
            String schedulerId = SchedulerConfigTests.randomValidSchedulerId();
            String jobId = randomAsciiOfLength(10);
            SchedulerConfig.Builder schedulerConfig = new SchedulerConfig.Builder(schedulerId, jobId);
            schedulerConfig.setIndexes(randomSubsetOf(2, Arrays.asList("index-1", "index-2", "index-3")));
            schedulerConfig.setTypes(randomSubsetOf(2, Arrays.asList("type-1", "type-2", "type-3")));
            schedulerConfig.setFrequency(randomPositiveLong());
            schedulerConfig.setQueryDelay(randomPositiveLong());
            if (randomBoolean()) {
                schedulerConfig.setQuery(Collections.singletonMap(randomAsciiOfLength(10), randomAsciiOfLength(10)));
            }
            if (randomBoolean()) {
                schedulerConfig.setScriptFields(Collections.singletonMap(randomAsciiOfLength(10), randomAsciiOfLength(10)));
            }
            if (randomBoolean()) {
                schedulerConfig.setScrollSize(randomIntBetween(0, Integer.MAX_VALUE));
            }
            if (randomBoolean()) {
                schedulerConfig.setAggregations(Collections.singletonMap(randomAsciiOfLength(10), randomAsciiOfLength(10)));
            }

            schedulerList.add(schedulerConfig.build());
        }

        result = new Response(new QueryPage<>(schedulerList, schedulerList.size(), Scheduler.RESULTS_FIELD));

        return result;
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
