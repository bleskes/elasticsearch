package org.elasticsearch.xpack.prelert.job.results;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

import java.util.ArrayList;
import java.util.List;

public class InfluenceTests extends AbstractSerializingTestCase<Influence> {

    @Override
    protected Influence createTestInstance() {
        int size = randomInt(10);
        List<String> fieldValues = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            fieldValues.add(randomAsciiOfLengthBetween(1, 20));
        }
        return new Influence(randomAsciiOfLengthBetween(1, 30), fieldValues);
    }

    @Override
    protected Reader<Influence> instanceReader() {
        return Influence::new;
    }

    @Override
    protected Influence parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return Influence.PARSER.apply(parser, () -> matcher);
    }

}
