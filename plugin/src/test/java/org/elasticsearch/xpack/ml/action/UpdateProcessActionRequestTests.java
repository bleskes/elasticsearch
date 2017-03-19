/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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

import org.elasticsearch.xpack.ml.job.config.JobUpdate;
import org.elasticsearch.xpack.ml.job.config.ModelPlotConfig;
import org.elasticsearch.xpack.ml.support.AbstractStreamableTestCase;

import java.util.List;

public class UpdateProcessActionRequestTests extends AbstractStreamableTestCase<UpdateProcessAction.Request> {


    @Override
    protected UpdateProcessAction.Request createTestInstance() {
        ModelPlotConfig config = null;
        if (randomBoolean()) {
            config = new ModelPlotConfig(randomBoolean(), randomAsciiOfLength(10));
        }
        List<JobUpdate.DetectorUpdate> updates = null;
        if (randomBoolean()) {
            int detectorUpdateCount = randomIntBetween(0, 5);
            for (int i = 0; i < detectorUpdateCount; i++) {
                new JobUpdate.DetectorUpdate(randomInt(), randomAsciiOfLength(10), null);
            }
        }
        return new UpdateProcessAction.Request(randomAsciiOfLength(10), config, updates);
    }

    @Override
    protected UpdateProcessAction.Request createBlankInstance() {
        return new UpdateProcessAction.Request();
    }
}