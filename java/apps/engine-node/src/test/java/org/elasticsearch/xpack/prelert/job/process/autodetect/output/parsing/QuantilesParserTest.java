package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;

public class QuantilesParserTest extends ESTestCase {
    public void testParseJson() throws IOException {
        String input = "{\"timestamp\": 1,"
                + " \"quantileState\": \"quantile-state\""
                + "}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        Quantiles quantile = new QuantilesParser(parser).parseJson();
        assertEquals("quantile-state", quantile.getQuantileState());

        assertEquals(new Date(1000L), quantile.getTimestamp());

        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    private static JsonParser createJsonParser(String input) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        return new JsonFactory().createParser(inputStream);
    }

}
