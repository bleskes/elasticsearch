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

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.action.PutSchedulerAction.Request;
import org.elasticsearch.xpack.ml.scheduler.SchedulerConfig;
import org.elasticsearch.xpack.ml.scheduler.SchedulerConfigTests;
import org.elasticsearch.xpack.ml.support.AbstractStreamableXContentTestCase;
import org.junit.Before;

import java.util.Arrays;

public class PutSchedulerActionRequestTests extends AbstractStreamableXContentTestCase<Request> {

    private String schedulerId;

    @Before
    public void setUpSchedulerId() {
        schedulerId = SchedulerConfigTests.randomValidSchedulerId();
    }

    @Override
    protected Request createTestInstance() {
        SchedulerConfig.Builder schedulerConfig = new SchedulerConfig.Builder(schedulerId, randomAsciiOfLength(10));
        schedulerConfig.setIndexes(Arrays.asList(randomAsciiOfLength(10)));
        schedulerConfig.setTypes(Arrays.asList(randomAsciiOfLength(10)));
        return new Request(schedulerConfig.build());
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

    @Override
    protected Request parseInstance(XContentParser parser) {
        return Request.parseRequest(schedulerId, parser);
    }

}
