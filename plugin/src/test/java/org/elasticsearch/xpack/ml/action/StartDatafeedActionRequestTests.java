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

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.action.StartDatafeedAction.DatafeedParams;
import org.elasticsearch.xpack.ml.action.StartDatafeedAction.Request;
import org.elasticsearch.xpack.ml.support.AbstractStreamableXContentTestCase;

import static org.hamcrest.Matchers.equalTo;

public class StartDatafeedActionRequestTests extends AbstractStreamableXContentTestCase<StartDatafeedAction.Request> {

    @Override
    protected Request createTestInstance() {
        DatafeedParams params = new DatafeedParams(randomAlphaOfLength(10), randomNonNegativeLong());
        if (randomBoolean()) {
            params.setEndTime(randomNonNegativeLong());
        }
        if (randomBoolean()) {
            params.setTimeout(TimeValue.timeValueMillis(randomNonNegativeLong()));
        }
        return new Request(params);
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

    @Override
    protected Request parseInstance(XContentParser parser) {
        return Request.parseRequest(null, parser);
    }

    public void testParseDateOrThrow() {
        assertEquals(0L, StartDatafeedAction.DatafeedParams.parseDateOrThrow("0",
                StartDatafeedAction.START_TIME, () -> System.currentTimeMillis()));
        assertEquals(0L, StartDatafeedAction.DatafeedParams.parseDateOrThrow("1970-01-01T00:00:00Z",
                StartDatafeedAction.START_TIME, () -> System.currentTimeMillis()));
        assertThat(StartDatafeedAction.DatafeedParams.parseDateOrThrow("now",
                StartDatafeedAction.START_TIME, () -> 123456789L), equalTo(123456789L));

        Exception e = expectThrows(ElasticsearchParseException.class,
                () -> StartDatafeedAction.DatafeedParams.parseDateOrThrow("not-a-date",
                        StartDatafeedAction.START_TIME, () -> System.currentTimeMillis()));
        assertEquals("Query param 'start' with value 'not-a-date' cannot be parsed as a date or converted to a number (epoch).",
                e.getMessage());
    }
}
