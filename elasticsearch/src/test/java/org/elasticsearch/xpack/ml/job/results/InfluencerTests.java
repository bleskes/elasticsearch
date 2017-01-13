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
package org.elasticsearch.xpack.ml.job.results;

import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.support.AbstractSerializingTestCase;

import java.util.Date;

public class InfluencerTests extends AbstractSerializingTestCase<Influencer> {

    public  Influencer createTestInstance(String jobId) {
        Influencer influencer = new Influencer(jobId, randomAsciiOfLengthBetween(1, 20), randomAsciiOfLengthBetween(1, 20),
                new Date(randomPositiveLong()), randomPositiveLong(), randomIntBetween(1, 1000));
        influencer.setInterim(randomBoolean());
        influencer.setAnomalyScore(randomDouble());
        influencer.setInitialAnomalyScore(randomDouble());
        influencer.setProbability(randomDouble());
        return influencer;
    }
    @Override
    protected Influencer createTestInstance() {
        return createTestInstance(randomAsciiOfLengthBetween(1, 20));
    }

    @Override
    protected Reader<Influencer> instanceReader() {
        return Influencer::new;
    }

    @Override
    protected Influencer parseInstance(XContentParser parser) {
        return Influencer.PARSER.apply(parser, null);
    }

}
