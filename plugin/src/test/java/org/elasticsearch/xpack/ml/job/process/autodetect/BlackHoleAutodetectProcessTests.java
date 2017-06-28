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
package org.elasticsearch.xpack.ml.job.process.autodetect;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.process.autodetect.output.FlushAcknowledgement;
import org.elasticsearch.xpack.ml.job.process.autodetect.params.FlushJobParams;
import org.elasticsearch.xpack.ml.job.results.AutodetectResult;

import java.util.Iterator;

public class BlackHoleAutodetectProcessTests extends ESTestCase {

    public void testFlushJob_writesAck() throws Exception {
        try (BlackHoleAutodetectProcess process = new BlackHoleAutodetectProcess("foo")) {
            String flushId = process.flushJob(FlushJobParams.builder().build());
            Iterator<AutodetectResult> iterator = process.readAutodetectResults();
            iterator.hasNext();
            AutodetectResult result = iterator.next();
            FlushAcknowledgement ack = result.getFlushAcknowledgement();
            assertEquals(flushId, ack.getId());
        }
    }
}
