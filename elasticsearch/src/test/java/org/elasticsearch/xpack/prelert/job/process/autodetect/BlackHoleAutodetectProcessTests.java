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

    public void testFlushJob_writesAck() throws Exception {
        try (BlackHoleAutodetectProcess process = new BlackHoleAutodetectProcess()) {

            String flushId = process.flushJob(InterimResultsParams.builder().build());

            XContentParser parser = XContentFactory.xContent(XContentType.JSON).createParser(process.getProcessOutStream());
            parser.nextToken(); // FlushAcknowledgementParser expects this to be
                                // called first
            AutodetectResult result = AutodetectResult.PARSER.apply(parser, () -> ParseFieldMatcher.STRICT);
            FlushAcknowledgement ack = result.getFlushAcknowledgement();
            assertEquals(flushId, ack.getId());
        }
    }
}