package org.elasticsearch.xpack.prelert.job.process.autodetect;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.FlushAcknowledgement;
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing.FlushAcknowledgementParser;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;

public class BlackHoleAutodetectProcessTest extends ESTestCase {

    public void testFlushJob_writesAck() throws Exception {
        BlackHoleAutodetectProcess process = new BlackHoleAutodetectProcess();

        String flushId = process.flushJob(InterimResultsParams.builder().build());

        JsonParser jsonParser = new JsonFactory().createParser(process.out());
        jsonParser.nextToken(); // FlushAcknowledgementParser expects this to be called first
        FlushAcknowledgement ack = new FlushAcknowledgementParser(jsonParser).parseJson();
        assertEquals(flushId, ack.getId());
    }
}