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
package org.elasticsearch.xpack.prelert.job.results;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
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

    public void testValidate_GivenSkipIsMinusOne() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> new PageParams(-1, 100));
        assertEquals("Parameter [skip] cannot be < 0", e.getMessage());
    }

    public void testValidate_GivenSkipIsMinusTen() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> new PageParams(-10, 100));
        assertEquals("Parameter [skip] cannot be < 0", e.getMessage());
    }

    public void testValidate_GivenTakeIsMinusOne() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> new PageParams(0, -1));
        assertEquals("Parameter [take] cannot be < 0", e.getMessage());
    }

    public void testValidate_GivenTakeIsMinusHundred() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> new PageParams(0, -100));
        assertEquals("Parameter [take] cannot be < 0", e.getMessage());
    }

    public void testValidate_GivenSkipAndTakeSumIsMoreThan10000() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> new PageParams(0, 10001));
        assertEquals("The sum of parameters [skip] and [take] cannot be higher than 10000.", e.getMessage());
    }
}
