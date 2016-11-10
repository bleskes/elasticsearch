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
package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;
import org.joda.time.DateTime;

import java.util.function.Consumer;

public class PostDataActionResponseTests extends AbstractStreamableTestCase<PostDataAction.Response> {

    @Override
    protected PostDataAction.Response createTestInstance() {
        DataCounts counts = new DataCounts(randomAsciiOfLength(10), randomIntBetween(1, 1_000_000), randomIntBetween(1, 1_000_000),
                randomIntBetween(1, 1_000_000), randomIntBetween(1, 1_000_000), randomIntBetween(1, 1_000_000),
                randomIntBetween(1, 1_000_000), randomIntBetween(1, 1_000_000), randomIntBetween(1, 1_000_000),
                randomIntBetween(1, 1_000_000), randomIntBetween(1, 1_000_000), new DateTime(randomDateTimeZone()).toDate());

        return new PostDataAction.Response(counts);
    }

    private void ifRandomTrueSetRandomLong(Consumer<Long> consumer) {
        if (randomBoolean()) {
            consumer.accept(randomPositiveLong());
        }
    }

    @Override
    protected PostDataAction.Response createBlankInstance() {
        return new PostDataAction.Response("foo") ;
    }
}
