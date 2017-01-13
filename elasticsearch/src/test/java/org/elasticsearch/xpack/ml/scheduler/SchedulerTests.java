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
package org.elasticsearch.xpack.ml.scheduler;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.support.AbstractSerializingTestCase;

public class SchedulerTests extends AbstractSerializingTestCase<Scheduler> {

    @Override
    protected Scheduler createTestInstance() {
        return new Scheduler(SchedulerConfigTests.createRandomizedSchedulerConfig(randomAsciiOfLength(10)),
                randomFrom(SchedulerStatus.values()));
    }

    @Override
    protected Writeable.Reader<Scheduler> instanceReader() {
        return Scheduler::new;
    }

    @Override
    protected Scheduler parseInstance(XContentParser parser) {
        return Scheduler.PARSER.apply(parser, null);
    }
}