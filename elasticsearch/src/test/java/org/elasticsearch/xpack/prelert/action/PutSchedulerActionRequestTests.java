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

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.action.PutSchedulerAction.Request;
import org.elasticsearch.xpack.prelert.scheduler.SchedulerConfig;
import org.elasticsearch.xpack.prelert.scheduler.SchedulerConfigTests;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableXContentTestCase;
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
    protected Request parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return Request.parseRequest(schedulerId, parser, () -> matcher);
    }

}
