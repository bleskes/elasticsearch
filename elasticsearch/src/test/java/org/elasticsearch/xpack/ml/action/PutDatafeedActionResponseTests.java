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
package org.elasticsearch.xpack.ml.action;

import org.elasticsearch.xpack.ml.action.PutDatafeedAction.Response;
import org.elasticsearch.xpack.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.ml.datafeed.DatafeedConfigTests;
import org.elasticsearch.xpack.ml.support.AbstractStreamableTestCase;

import java.util.Arrays;

public class PutDatafeedActionResponseTests extends AbstractStreamableTestCase<Response> {

    @Override
    protected Response createTestInstance() {
        DatafeedConfig.Builder datafeedConfig = new DatafeedConfig.Builder(
                DatafeedConfigTests.randomValidDatafeedId(), randomAsciiOfLength(10));
        datafeedConfig.setIndexes(Arrays.asList(randomAsciiOfLength(10)));
        datafeedConfig.setTypes(Arrays.asList(randomAsciiOfLength(10)));
        return new Response(randomBoolean(), datafeedConfig.build());
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
