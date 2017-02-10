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
package org.elasticsearch.xpack.ml.notifications;

import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.support.AbstractSerializingTestCase;
import org.elasticsearch.xpack.ml.utils.time.TimeUtils;
import org.junit.Before;

import java.util.Date;

public class AuditActivityTests extends AbstractSerializingTestCase<AuditActivity> {
    private long startMillis;

    @Before
    public void setStartTime() {
        startMillis = System.currentTimeMillis();
    }

    public void testDefaultConstructor() {
        AuditActivity activity = new AuditActivity();
        assertEquals(0, activity.getTotalJobs());
        assertEquals(0, activity.getTotalDetectors());
        assertEquals(0, activity.getRunningJobs());
        assertEquals(0, activity.getRunningDetectors());
        assertNull(activity.getTimestamp());
    }

    public void testNewActivity() {
        AuditActivity activity = AuditActivity.newActivity(10, 100, 5, 50);
        assertEquals(10, activity.getTotalJobs());
        assertEquals(100, activity.getTotalDetectors());
        assertEquals(5, activity.getRunningJobs());
        assertEquals(50, activity.getRunningDetectors());
        assertDateBetweenStartAndNow(activity.getTimestamp());
    }

    private void assertDateBetweenStartAndNow(Date timestamp) {
        long timestampMillis = timestamp.getTime();
        assertTrue(timestampMillis >= startMillis);
        assertTrue(timestampMillis <= System.currentTimeMillis());
    }

    @Override
    protected AuditActivity parseInstance(XContentParser parser) {
        return AuditActivity.PARSER.apply(parser, null);
    }

    @Override
    protected AuditActivity createTestInstance() {
        AuditActivity message = new AuditActivity();
        if (randomBoolean()) {
            message.setRunningJobs(randomInt());
        }
        if (randomBoolean()) {
            message.setRunningDetectors(randomInt());
        }
        if (randomBoolean()) {
            message.setTotalJobs(randomInt());
        }
        if (randomBoolean()) {
            message.setTotalDetectors(randomInt());
        }
        if (randomBoolean()) {
            message.setTimestamp(new Date(TimeUtils.dateStringToEpoch(randomTimeValue())));
        }
        return message;
    }

    @Override
    protected Reader<AuditActivity> instanceReader() {
        return AuditActivity::new;
    }
}
