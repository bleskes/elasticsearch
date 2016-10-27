package org.elasticsearch.xpack.prelert.job.results;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

public class PageParamsTests extends AbstractSerializingTestCase<PageParams> {

    @Override
    protected PageParams parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return PageParams.PARSER.apply(parser, () -> matcher);
    }

    @Override
    protected PageParams createTestInstance() {
        int skip = randomInt(PageParams.MAX_SKIP_TAKE_SUM);
        int maxTake = PageParams.MAX_SKIP_TAKE_SUM - skip;
        int take = randomInt(maxTake);
        return new PageParams(skip, take);
    }

    @Override
    protected Reader<PageParams> instanceReader() {
        return PageParams::new;
    }

    @Override
    public void testJacksonSerialisation() throws Exception {
        // Skip Jackson serialisation tests for this class since its a new class
        // and doesn't get jackson serialised
    }

    public void testValidate_GivenSkipIsMinusOne() {
        ElasticsearchException e = expectThrows(ElasticsearchException.class, () -> new PageParams(-1, 100));
        assertEquals("Parameter [skip] cannot be < 0", e.getMessage());
        assertEquals(ErrorCodes.INVALID_SKIP_PARAM.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testValidate_GivenSkipIsMinusTen() {
        ElasticsearchException e = expectThrows(ElasticsearchException.class, () -> new PageParams(-10, 100));
        assertEquals("Parameter [skip] cannot be < 0", e.getMessage());
        assertEquals(ErrorCodes.INVALID_SKIP_PARAM.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testValidate_GivenTakeIsMinusOne() {
        ElasticsearchException e = expectThrows(ElasticsearchException.class, () -> new PageParams(0, -1));
        assertEquals("Parameter [take] cannot be < 0", e.getMessage());
        assertEquals(ErrorCodes.INVALID_TAKE_PARAM.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testValidate_GivenTakeIsMinusHundred() {
        ElasticsearchException e = expectThrows(ElasticsearchException.class, () -> new PageParams(0, -100));
        assertEquals("Parameter [take] cannot be < 0", e.getMessage());
        assertEquals(ErrorCodes.INVALID_TAKE_PARAM.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testValidate_GivenSkipAndTakeSumIsMoreThan10000() {
        ElasticsearchException e = expectThrows(ElasticsearchException.class, () -> new PageParams(0, 10001));
        assertEquals("The sum of parameters [skip] and [take] cannot be higher than 10000.", e.getMessage());
        assertEquals(ErrorCodes.INVALID_TAKE_PARAM.getValueString(), e.getHeader("errorCode").get(0));
    }

}
