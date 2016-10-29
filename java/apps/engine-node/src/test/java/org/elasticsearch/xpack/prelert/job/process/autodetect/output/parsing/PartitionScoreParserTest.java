package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.results.PartitionScore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PartitionScoreParserTest extends ESTestCase {

    @Test
    public void testParse() throws IOException {
        String json = "{\"probability\":0.1,\"partitionFieldName\":\"part1\"," +
                "\"partitionFieldValue\":\"p0\",\"normalizedProbability\":0.2}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        PartitionScore score = new PartitionScoreParser(parser).parseJson();

        assertEquals("part1", score.getPartitionFieldName());
        assertEquals("p0", score.getPartitionFieldValue());
        assertEquals(0.2, score.getAnomalyScore(), 0.0001);
        assertEquals(0.1, score.getProbability(), 0.0001);
    }

    private static JsonParser createJsonParser(String input) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new JsonFactory().createParser(inputStream);
    }

}
