package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.FlushAcknowledgement;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FlushAcknowledgementParserTest extends ESTestCase {

    public void testParseJson() throws IOException {
        String input = "{\"flush\": \"job-id\"}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        FlushAcknowledgement ack = new FlushAcknowledgementParser(parser).parseJson();

        assertEquals("job-id", ack.getId());

        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }


    private static JsonParser createJsonParser(String input) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new JsonFactory().createParser(inputStream);
    }
}
