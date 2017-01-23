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

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xpack.ml.action.StopDatafeedAction.Request;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.metadata.MlMetadata;
import org.elasticsearch.xpack.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.ml.datafeed.DatafeedStatus;
import org.elasticsearch.xpack.ml.support.AbstractStreamableTestCase;

import static org.elasticsearch.xpack.ml.datafeed.DatafeedJobRunnerTests.createDatafeedJob;
import static org.elasticsearch.xpack.ml.datafeed.DatafeedJobRunnerTests.createDatafeedConfig;
import static org.hamcrest.Matchers.equalTo;

public class StopDatafeedActionRequestTests extends AbstractStreamableTestCase<StopDatafeedAction.Request> {

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
        Job job = createDatafeedJob().build();
        MlMetadata mlMetadata1 = new MlMetadata.Builder().putJob(job, false).build();
        Exception e = expectThrows(ResourceNotFoundException.class, () -> StopDatafeedAction.validate("foo", mlMetadata1));
        assertThat(e.getMessage(), equalTo("No datafeed with id [foo] exists"));

        DatafeedConfig datafeedConfig = createDatafeedConfig("foo", "foo").build();
        MlMetadata mlMetadata2 = new MlMetadata.Builder().putJob(job, false)
                .putDatafeed(datafeedConfig)
                .build();
        e = expectThrows(ElasticsearchStatusException.class, () -> StopDatafeedAction.validate("foo", mlMetadata2));
        assertThat(e.getMessage(), equalTo("datafeed already stopped, expected datafeed status [STARTED], but got [STOPPED]"));

        MlMetadata mlMetadata3 = new MlMetadata.Builder().putJob(job, false)
                .putDatafeed(datafeedConfig)
                .updateDatafeedStatus("foo", DatafeedStatus.STARTED)
                .build();
        StopDatafeedAction.validate("foo", mlMetadata3);
    }

}
