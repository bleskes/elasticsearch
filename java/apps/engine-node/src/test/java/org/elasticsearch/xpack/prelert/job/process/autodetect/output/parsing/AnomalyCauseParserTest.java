
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.results.AnomalyCause;
import org.elasticsearch.xpack.prelert.job.results.Influence;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AnomalyCauseParserTest extends ESTestCase {

    public void testParseJson() throws IOException {
        String json = "{"
                + "\"fieldName\" : \"groundspeed\","
                + "\"probability\" : 6.04434E-49,"
                + "\"byFieldName\" : \"status\","
                + "\"byFieldValue\" : \"Climb\","
                + "\"correlatedByFieldValue\" : \"Crash\","
                + "\"partitionFieldName\" : \"aircrafttype\","
                + "\"partitionFieldValue\" : \"A321\","
                + "\"function\" : \"low_mean\","
                + "\"functionDescription\" : \"mean\","
                + "\"typical\" : [ 442.616 ],"
                + "\"actual\" : [ 10.0 ],"
                + "\"influencers\" : {"
                + "\"host\": [\"web-server\", \"localhost\"],"
                + "\"user\": [\"cat\"]"
                + "},"
                + "\"overFieldName\" : \"callsign\","
                + "\"overFieldValue\" : \"HVN600\""
                + "}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonParser parser = new JsonFactory().createParser(inputStream);

        parser.nextToken();
        AnomalyCause cause = new AnomalyCauseParser(parser).parseJson();

        assertEquals("groundspeed", cause.getFieldName());
        assertEquals(6.04434E-49, cause.getProbability(), 0.0001);
        assertEquals("status", cause.getByFieldName());
        assertEquals("Climb", cause.getByFieldValue());
        assertEquals("Crash", cause.getCorrelatedByFieldValue());
        assertEquals("aircrafttype", cause.getPartitionFieldName());
        assertEquals("A321", cause.getPartitionFieldValue());
        assertEquals("low_mean", cause.getFunction());
        assertEquals("mean", cause.getFunctionDescription());
        assertEquals(442.616, cause.getTypical()[0], 0.001);
        assertEquals(10.0, cause.getActual()[0], 0.0001);
        assertEquals("callsign", cause.getOverFieldName());
        assertEquals("HVN600", cause.getOverFieldValue());

        List<Influence> influences = cause.getInfluencers();

        Influence host = influences.get(0);
        assertEquals("host", host.getInfluencerFieldName());
        assertEquals(2, host.getInfluencerFieldValues().size());
        assertEquals("web-server", host.getInfluencerFieldValues().get(0));
        assertEquals("localhost", host.getInfluencerFieldValues().get(1));

        Influence user = influences.get(1);
        assertEquals("user", user.getInfluencerFieldName());
        assertEquals(1, user.getInfluencerFieldValues().size());
        assertEquals("cat", user.getInfluencerFieldValues().get(0));
    }
}
