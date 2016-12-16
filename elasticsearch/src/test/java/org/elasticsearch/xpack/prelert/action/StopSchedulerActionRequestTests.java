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

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xpack.prelert.action.StopSchedulerAction.Request;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.scheduler.SchedulerConfig;
import org.elasticsearch.xpack.prelert.scheduler.SchedulerStatus;
import org.elasticsearch.xpack.prelert.job.metadata.PrelertMetadata;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

import static org.elasticsearch.xpack.prelert.scheduler.ScheduledJobRunnerTests.createScheduledJob;
import static org.elasticsearch.xpack.prelert.scheduler.ScheduledJobRunnerTests.createSchedulerConfig;
import static org.hamcrest.Matchers.equalTo;

public class StopSchedulerActionRequestTests extends AbstractStreamableTestCase<StopSchedulerAction.Request> {

    @Override
    protected Request createTestInstance() {
        Request r = new Request(randomAsciiOfLengthBetween(1, 20));
        r.setStopTimeout(TimeValue.timeValueSeconds(randomIntBetween(0, 999)));
        return r;
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

    public void testValidate() {
        Job job = createScheduledJob().build();
        PrelertMetadata prelertMetadata1 = new PrelertMetadata.Builder().putJob(job, false).build();
        Exception e = expectThrows(ResourceNotFoundException.class, () -> StopSchedulerAction.validate("foo", prelertMetadata1));
        assertThat(e.getMessage(), equalTo("No scheduler with id [foo] exists"));

        SchedulerConfig schedulerConfig = createSchedulerConfig("foo", "foo").build();
        PrelertMetadata prelertMetadata2 = new PrelertMetadata.Builder().putJob(job, false)
                .putScheduler(schedulerConfig)
                .build();
        e = expectThrows(ElasticsearchStatusException.class, () -> StopSchedulerAction.validate("foo", prelertMetadata2));
        assertThat(e.getMessage(), equalTo("scheduler already stopped, expected scheduler status [STARTED], but got [STOPPED]"));

        PrelertMetadata prelertMetadata3 = new PrelertMetadata.Builder().putJob(job, false)
                .putScheduler(schedulerConfig)
                .updateSchedulerStatus("foo", SchedulerStatus.STARTED)
                .build();
        StopSchedulerAction.validate("foo", prelertMetadata3);
    }

}
