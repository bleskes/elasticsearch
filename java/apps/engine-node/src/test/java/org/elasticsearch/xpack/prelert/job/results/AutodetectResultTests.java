/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (C) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained therein, is the
 * exclusive property of Elasticsearch BV and its licensors, if any, and
 * is protected under applicable domestic and foreign law, and international
 * treaties. Reproduction, republication or distribution without the express
 * written consent of Elasticsearch BV is strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.results;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.FlushAcknowledgement;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

import java.util.Date;

public class AutodetectResultTests extends AbstractSerializingTestCase<AutodetectResult> {

    @Override
    protected AutodetectResult parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return AutodetectResult.PARSER.apply(parser, () -> matcher);
    }

    @Override
    protected AutodetectResult createTestInstance() {
        Bucket bucket;
        Quantiles quantiles;
        ModelSnapshot modelSnapshot;
        ModelSizeStats.Builder modelSizeStats;
        ModelDebugOutput modelDebugOutput;
        CategoryDefinition categoryDefinition;
        FlushAcknowledgement flushAcknowledgement;
        String jobId = "foo";
        if (randomBoolean()) {
            bucket = new Bucket(jobId);
            bucket.setId(randomAsciiOfLengthBetween(1, 20));
        } else {
            bucket = null;
        }
        if (randomBoolean()) {
            quantiles = new Quantiles(jobId, new Date(randomLong()), randomAsciiOfLengthBetween(1, 20));
        } else {
            quantiles = null;
        }
        if (randomBoolean()) {
            modelSnapshot = new ModelSnapshot(jobId);
            modelSnapshot.setDescription(randomAsciiOfLengthBetween(1, 20));
        } else {
            modelSnapshot = null;
        }
        if (randomBoolean()) {
            modelSizeStats = new ModelSizeStats.Builder(jobId);
            modelSizeStats.setId(randomAsciiOfLengthBetween(1, 20));
        } else {
            modelSizeStats = null;
        }
        if (randomBoolean()) {
            modelDebugOutput = new ModelDebugOutput(jobId);
            modelDebugOutput.setId(randomAsciiOfLengthBetween(1, 20));
        } else {
            modelDebugOutput = null;
        }
        if (randomBoolean()) {
            categoryDefinition = new CategoryDefinition(jobId);
            categoryDefinition.setCategoryId(randomLong());
        } else {
            categoryDefinition = null;
        }
        if (randomBoolean()) {
            flushAcknowledgement = new FlushAcknowledgement(randomAsciiOfLengthBetween(1, 20));
        } else {
            flushAcknowledgement = null;
        }
        return new AutodetectResult(bucket, quantiles, modelSnapshot, modelSizeStats == null ? null : modelSizeStats.build(),
                modelDebugOutput, categoryDefinition, flushAcknowledgement);
    }

    @Override
    protected Reader<AutodetectResult> instanceReader() {
        return AutodetectResult::new;
    }

}
