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
package org.elasticsearch.xpack.ml.job.config;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.support.AbstractSerializingTestCase;

public class JobTaskStatusTests extends AbstractSerializingTestCase<JobTaskStatus> {

    @Override
    protected JobTaskStatus createTestInstance() {
        return new JobTaskStatus(randomFrom(JobState.values()), randomLong());
    }

    @Override
    protected Writeable.Reader<JobTaskStatus> instanceReader() {
        return JobTaskStatus::new;
    }

    @Override
    protected JobTaskStatus parseInstance(XContentParser parser) {
        return JobTaskStatus.fromXContent(parser);
    }
}
