package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.results.ModelDebugOutput;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;

public class ModelDebugOutputParserTest extends ESTestCase {
    public void testParseJson_GivenInvalidJson() throws IOException {
        String input = "\"debugFeature\": \"sum\" }";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        ESTestCase.expectThrows(ElasticsearchParseException.class, () -> new ModelDebugOutputParser(parser).parseJson());
    }

    public void testParseJson_GivenModelDebugOutputWithAllFieldsPopulatedAndValid()
            throws IOException {
        String input = "{\"debugFeature\": \"sum\","
                + " \"partitionFieldName\":\"pn\","
                + " \"partitionFieldValue\":\"pv\","
                + " \"overFieldName\":\"on\","
                + " \"overFieldValue\":\"ov\","
                + " \"byFieldName\":\"bn\","
                + " \"byFieldValue\":\"bv\","
                + " \"timestamp\":1234567890000,"
                + " \"debugLower\":12.7,"
                + " \"debugMedian\":17.9,"
                + " \"debugUpper\":24.2,"
                + " \"actual\": 100.0}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        ModelDebugOutput modelDebugOutput = new ModelDebugOutputParser(parser).parseJson();

        assertEquals(new Date(1234567890000L), modelDebugOutput.getTimestamp());
        assertEquals("pn", modelDebugOutput.getPartitionFieldName());
        assertEquals("pv", modelDebugOutput.getPartitionFieldValue());
        assertEquals("on", modelDebugOutput.getOverFieldName());
        assertEquals("ov", modelDebugOutput.getOverFieldValue());
        assertEquals("bn", modelDebugOutput.getByFieldName());
        assertEquals("bv", modelDebugOutput.getByFieldValue());
        assertEquals("sum", modelDebugOutput.getDebugFeature());
        assertEquals(12.7, modelDebugOutput.getDebugLower(), 1e-10);
        assertEquals(17.9, modelDebugOutput.getDebugMedian(), 1e-10);
        assertEquals(24.2, modelDebugOutput.getDebugUpper(), 1e-10);
        assertEquals(100.0, modelDebugOutput.getActual(), 1e-10);

        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    private static JsonParser createJsonParser(String input) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        return new JsonFactory().createParser(inputStream);
    }
}
