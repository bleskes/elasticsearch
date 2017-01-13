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
package org.elasticsearch.xpack.ml.job.process.autodetect.output;

import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.support.AbstractSerializingTestCase;

public class FlushAcknowledgementTests extends AbstractSerializingTestCase<FlushAcknowledgement> {

    @Override
    protected FlushAcknowledgement parseInstance(XContentParser parser) {
        return FlushAcknowledgement.PARSER.apply(parser, null);
    }

    @Override
    protected FlushAcknowledgement createTestInstance() {
        return new FlushAcknowledgement(randomAsciiOfLengthBetween(1, 20));
    }

    @Override
    protected Reader<FlushAcknowledgement> instanceReader() {
        return FlushAcknowledgement::new;
    }

}
