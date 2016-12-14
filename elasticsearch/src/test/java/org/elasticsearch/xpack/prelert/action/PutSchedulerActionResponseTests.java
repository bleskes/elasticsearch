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

import org.elasticsearch.xpack.prelert.action.PutSchedulerAction.Response;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.SchedulerConfigTests;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

import java.util.Arrays;

public class PutSchedulerActionResponseTests extends AbstractStreamableTestCase<Response> {

    @Override
    protected Response createTestInstance() {
        SchedulerConfig.Builder schedulerConfig = new SchedulerConfig.Builder(
                SchedulerConfigTests.randomValidSchedulerId(), randomAsciiOfLength(10));
        schedulerConfig.setIndexes(Arrays.asList(randomAsciiOfLength(10)));
        schedulerConfig.setTypes(Arrays.asList(randomAsciiOfLength(10)));
        return new Response(randomBoolean(), schedulerConfig.build());
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
