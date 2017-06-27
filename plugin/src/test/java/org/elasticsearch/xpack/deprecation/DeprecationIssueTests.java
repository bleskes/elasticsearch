/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.deprecation;

import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.common.xcontent.ToXContent.EMPTY_PARAMS;
import static org.elasticsearch.xpack.deprecation.DeprecationIssue.Level;
import static org.hamcrest.core.IsEqual.equalTo;

public class DeprecationIssueTests extends ESTestCase {
    DeprecationIssue issue;

    static DeprecationIssue createTestInstance() {
        String details = randomBoolean() ? randomAlphaOfLength(10) : null;
        return new DeprecationIssue(randomFrom(Level.values()), randomAlphaOfLength(10),
            randomAlphaOfLength(10), details);
    }

    @Before
    public void setup() {
        issue = createTestInstance();
    }

    public void testEqualsAndHashCode() {
        DeprecationIssue other = new DeprecationIssue(issue.getLevel(), issue.getMessage(), issue.getUrl(), issue.getDetails());
        assertThat(issue, equalTo(other));
        assertThat(other, equalTo(issue));
        assertThat(issue.hashCode(), equalTo(other.hashCode()));
    }

    public void testSerialization() throws IOException {
        BytesStreamOutput out = new BytesStreamOutput();
        issue.writeTo(out);
        StreamInput in = out.bytes().streamInput();
        DeprecationIssue other = new DeprecationIssue(in);
        assertThat(issue, equalTo(other));
    }

    public void testToXContent() throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        issue.toXContent(builder, EMPTY_PARAMS);
        Map<String, Object> toXContentMap = XContentHelper.convertToMap(builder.bytes(), false, builder.contentType()).v2();
        String level = (String) toXContentMap.get("level");
        String message = (String) toXContentMap.get("message");
        String url = (String) toXContentMap.get("url");
        if (issue.getDetails() != null) {
            assertTrue(toXContentMap.containsKey("details"));
        }
        String details = (String) toXContentMap.get("details");
        DeprecationIssue other = new DeprecationIssue(Level.fromString(level), message, url, details);
        assertThat(issue, equalTo(other));
    }
}
