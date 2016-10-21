package org.elasticsearch.xpack.prelert.job.results;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

public class PartitionScoreTests extends AbstractSerializingTestCase<PartitionScore> {

    @Override
    protected PartitionScore createTestInstance() {
        return new PartitionScore(randomAsciiOfLengthBetween(1, 20), randomAsciiOfLengthBetween(1, 20), randomDouble(), randomDouble());
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
