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
package org.elasticsearch.xpack.ml.job.config;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.support.AbstractSerializingTestCase;

import java.util.Collections;
import java.util.Date;

import static org.elasticsearch.xpack.ml.job.config.JobTests.randomValidJobId;

public class JobBuilderTests extends AbstractSerializingTestCase<Job.Builder> {
    @Override
    protected Job.Builder createTestInstance() {
        Job.Builder builder = new Job.Builder();
        if (randomBoolean()) {
            builder.setId(randomValidJobId());
        }
        if (randomBoolean()) {
            builder.setDescription(randomAlphaOfLength(10));
        }
        if (randomBoolean()) {
            builder.setCreateTime(new Date(randomNonNegativeLong()));
        }
        if (randomBoolean()) {
            builder.setFinishedTime(new Date(randomNonNegativeLong()));
        }
        if (randomBoolean()) {
            builder.setLastDataTime(new Date(randomNonNegativeLong()));
        }
        if (randomBoolean()) {
            builder.setAnalysisConfig(AnalysisConfigTests.createRandomized());
        }
        if (randomBoolean()) {
            builder.setAnalysisLimits(new AnalysisLimits(randomNonNegativeLong(),
                    randomNonNegativeLong()));
        }
        if (randomBoolean()) {
            DataDescription.Builder dataDescription = new DataDescription.Builder();
            dataDescription.setFormat(randomFrom(DataDescription.DataFormat.values()));
            builder.setDataDescription(dataDescription);
        }
        if (randomBoolean()) {
            builder.setModelPlotConfig(new ModelPlotConfig(randomBoolean(),
                    randomAlphaOfLength(10)));
        }
        if (randomBoolean()) {
            builder.setRenormalizationWindowDays(randomNonNegativeLong());
        }
        if (randomBoolean()) {
            builder.setBackgroundPersistInterval(TimeValue.timeValueHours(randomIntBetween(1, 24)));
        }
        if (randomBoolean()) {
            builder.setModelSnapshotRetentionDays(randomNonNegativeLong());
        }
        if (randomBoolean()) {
            builder.setResultsRetentionDays(randomNonNegativeLong());
        }
        if (randomBoolean()) {
            builder.setCustomSettings(Collections.singletonMap(randomAlphaOfLength(10),
                    randomAlphaOfLength(10)));
        }
        if (randomBoolean()) {
            builder.setModelSnapshotId(randomAlphaOfLength(10));
        }
        if (randomBoolean()) {
            builder.setResultsIndexName(randomValidJobId());
        }
        return builder;
    }

    @Override
    protected Writeable.Reader<Job.Builder> instanceReader() {
        return Job.Builder::new;
    }

    @Override
    protected Job.Builder parseInstance(XContentParser parser) {
        return Job.PARSER.apply(parser, null);
    }
}
