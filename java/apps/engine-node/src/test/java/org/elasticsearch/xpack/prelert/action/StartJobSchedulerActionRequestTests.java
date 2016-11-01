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
import org.elasticsearch.xpack.prelert.action.StartJobSchedulerAction.Request;
import org.elasticsearch.xpack.prelert.job.JobSchedulerStatus;
import org.elasticsearch.xpack.prelert.job.SchedulerState;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableXContentTestCase;

public class StartJobSchedulerActionRequestTests extends AbstractStreamableXContentTestCase<StartJobSchedulerAction.Request> {

    @Override
    protected Request createTestInstance() {
        SchedulerState state = new SchedulerState(JobSchedulerStatus.STARTED, randomLong(), randomLong());
        return new Request(randomAsciiOfLengthBetween(1, 20), state);
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

    @Override
    protected Request parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return Request.parseRequest(null, parser, () -> matcher);
    }

}
