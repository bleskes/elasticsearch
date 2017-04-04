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
package org.elasticsearch.xpack.ml.job.config;

import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.support.AbstractSerializingTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MlFilterTests extends AbstractSerializingTestCase<MlFilter> {

    @Override
    protected MlFilter createTestInstance() {
        int size = randomInt(10);
        List<String> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(randomAlphaOfLengthBetween(1, 20));
        }
        return new MlFilter(randomAlphaOfLengthBetween(1, 20), items);
    }

    @Override
    protected Reader<MlFilter> instanceReader() {
        return MlFilter::new;
    }

    @Override
    protected MlFilter parseInstance(XContentParser parser) {
        return MlFilter.PARSER.apply(parser, null);
    }

    public void testNullId() {
        NullPointerException ex = expectThrows(NullPointerException.class, () -> new MlFilter(null, Collections.emptyList()));
        assertEquals(MlFilter.ID.getPreferredName() + " must not be null", ex.getMessage());
    }

    public void testNullItems() {
        NullPointerException ex =
                expectThrows(NullPointerException.class, () -> new MlFilter(randomAlphaOfLengthBetween(1, 20), null));
        assertEquals(MlFilter.ITEMS.getPreferredName() + " must not be null", ex.getMessage());
    }

}
