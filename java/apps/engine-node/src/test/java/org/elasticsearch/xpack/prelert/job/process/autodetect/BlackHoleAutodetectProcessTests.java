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
package org.elasticsearch.xpack.prelert.job.process.autodetect;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.FlushAcknowledgement;
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing.FlushAcknowledgementParser;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;

public class BlackHoleAutodetectProcessTests extends ESTestCase {

    public void testFlushJob_writesAck() throws Exception {
        try (BlackHoleAutodetectProcess process = new BlackHoleAutodetectProcess()) {

            String flushId = process.flushJob(InterimResultsParams.builder().build());

            JsonParser jsonParser = new JsonFactory().createParser(process.out());
            jsonParser.nextToken(); // FlushAcknowledgementParser expects this to be called first
            FlushAcknowledgement ack = new FlushAcknowledgementParser(jsonParser).parseJson();
            assertEquals(flushId, ack.getId());
        }
    }
}