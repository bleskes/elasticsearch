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

import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.equalTo;

public class RuleActionTests extends ESTestCase {

    public void testForString() {
        assertEquals(RuleAction.FILTER_RESULTS, RuleAction.fromString("filter_results"));
        assertEquals(RuleAction.FILTER_RESULTS, RuleAction.fromString("FILTER_RESULTS"));
        assertEquals(RuleAction.FILTER_RESULTS, RuleAction.fromString("fiLTer_Results"));
    }

    public void testToString() {
        assertEquals("filter_results", RuleAction.FILTER_RESULTS.toString());
    }

    public void testReadFrom() throws Exception {
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            out.writeVInt(0);
            try (StreamInput in = out.bytes().streamInput()) {
                assertThat(RuleAction.readFromStream(in), equalTo(RuleAction.FILTER_RESULTS));
            }
        }
    }
}
