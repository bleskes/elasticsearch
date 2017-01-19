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
package org.elasticsearch.xpack.ml.datafeed;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.support.AbstractSerializingTestCase;

public class DatafeedTests extends AbstractSerializingTestCase<Datafeed> {

    @Override
    protected Datafeed createTestInstance() {
        return new Datafeed(DatafeedConfigTests.createRandomizedDatafeedConfig(randomAsciiOfLength(10)),
                randomFrom(DatafeedStatus.values()));
    }

    @Override
    protected Writeable.Reader<Datafeed> instanceReader() {
        return Datafeed::new;
    }

    @Override
    protected Datafeed parseInstance(XContentParser parser) {
        return Datafeed.PARSER.apply(parser, null);
    }
}
