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
package org.elasticsearch.xpack.ml.job.process.logging;

import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.support.AbstractSerializingTestCase;

public class CppLogMessageTests extends AbstractSerializingTestCase<CppLogMessage> {

    public void testDefaultConstructor() {
        CppLogMessage msg = new CppLogMessage();
        assertEquals("", msg.getLogger());
        assertTrue(msg.getTimestamp().toString(), msg.getTimestamp().getTime() > 0);
        assertEquals("", msg.getLevel());
        assertEquals(0, msg.getPid());
        assertEquals("", msg.getThread());
        assertEquals("", msg.getMessage());
        assertEquals("", msg.getClazz());
        assertEquals("", msg.getMethod());
        assertEquals("", msg.getFile());
        assertEquals(0, msg.getLine());
    }

    @Override
    protected CppLogMessage createTestInstance() {
        CppLogMessage msg = new CppLogMessage();
        msg.setLogger("autodetect");
        msg.setLevel("INFO");
        msg.setPid(12345);
        msg.setThread("0x123456789");
        msg.setMessage("Very informative");
        msg.setClazz("CAnomalyDetector");
        msg.setMethod("detectAnomalies");
        msg.setFile("CAnomalyDetector.cc");
        msg.setLine(123);
        return msg;
    }

    @Override
    protected Reader<CppLogMessage> instanceReader() {
        return CppLogMessage::new;
    }

    @Override
    protected CppLogMessage parseInstance(XContentParser parser) {
        return CppLogMessage.PARSER.apply(parser, null);
    }
}