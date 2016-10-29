package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class ModelSnapshotParserTest extends ESTestCase {
    public void testParseJson_GivenInvalidJson() throws IOException {
        String input = "\"snapshotId\": \"123\" }";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        ESTestCase.expectThrows(ElasticsearchParseException.class, () -> new ModelSnapshotParser(parser).parseJson());
    }

    public void testParseJson_GivenModelSnapshotWithAllFieldsPopulatedAndValid()
            throws JsonParseException, IOException {
        String input = "{\"snapshotId\": \"123\","
                + " \"description\":\"Very interesting\","
                + " \"restorePriority\":123,"
                + " \"timestamp\":1234567890000,"
                + " \"snapshotDocCount\":3,"
                + " \"modelSizeStats\":{\"modelBytes\":54321},"
                + " \"quantiles\": {\"quantileState\": \"yabadabadoo\"},"
                + " \"latestRecordTimeStamp\": 1111111111111,"
                + " \"latestResultTimeStamp\" : 1010101010101}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        ModelSnapshot modelSnapshot = new ModelSnapshotParser(parser).parseJson();

        assertEquals(new Date(1234567890000L), modelSnapshot.getTimestamp());
        assertEquals("123", modelSnapshot.getSnapshotId());
        assertEquals("Very interesting", modelSnapshot.getDescription());
        assertEquals(123L, modelSnapshot.getRestorePriority());
        assertEquals(3, modelSnapshot.getSnapshotDocCount());
        assertNotNull(modelSnapshot.getModelSizeStats());
        assertEquals(54321L, modelSnapshot.getModelSizeStats().getModelBytes());
        assertEquals(new Date(1111111111111L), modelSnapshot.getLatestRecordTimeStamp());
        assertEquals(new Date(1010101010101L), modelSnapshot.getLatestResultTimeStamp());
        Quantiles q = new Quantiles();
        q.setQuantileState("yabadabadoo");
        assertEquals(q, modelSnapshot.getQuantiles());

        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    private static JsonParser createJsonParser(String input) throws JsonParseException,
    IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new JsonFactory().createParser(inputStream);
    }
}
