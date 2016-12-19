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
package org.elasticsearch.xpack.prelert.job.results;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

public class PartitionScoreTests extends AbstractSerializingTestCase<PartitionScore> {

    @Override
    protected PartitionScore createTestInstance() {
        return new PartitionScore(randomAsciiOfLengthBetween(1, 20), randomAsciiOfLengthBetween(1, 20), randomDouble(), randomDouble(),
                randomDouble());
    }

    @Override
    protected Reader<PartitionScore> instanceReader() {
        return PartitionScore::new;
    }

    @Override
    protected PartitionScore parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return PartitionScore.PARSER.apply(parser, () -> matcher);
    }

}
