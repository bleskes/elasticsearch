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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ModelDebugConfigTests extends AbstractSerializingTestCase<ModelDebugConfig> {

    public void testConstructorDefaults() {
        assertThat(new ModelDebugConfig().isEnabled(), is(true));
        assertThat(new ModelDebugConfig().getTerms(), is(nullValue()));
    }

    @Override
    protected ModelDebugConfig createTestInstance() {
        return new ModelDebugConfig(randomBoolean(), randomAsciiOfLengthBetween(1, 30));
    }

    @Override
    protected Reader<ModelDebugConfig> instanceReader() {
        return ModelDebugConfig::new;
    }

    @Override
    protected ModelDebugConfig parseInstance(XContentParser parser) {
        return ModelDebugConfig.PARSER.apply(parser, null);
    }
}
