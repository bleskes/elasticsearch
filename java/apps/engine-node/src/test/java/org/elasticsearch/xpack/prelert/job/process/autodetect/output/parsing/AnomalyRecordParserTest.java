package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class AnomalyRecordParserTest extends ESTestCase {
    private static final double EPSILON = 0.000001;


    public void testParseJson_GivenParserDoesNotPointAtStartObject()
            throws ElasticsearchParseException, IOException {
        String input = "{}";
        JsonParser parser = createJsonParser(input);

        ESTestCase.expectThrows(ElasticsearchParseException.class, () -> new AnomalyRecordParser(parser).parseJson());
    }

    public void testParseJson_GivenEmptyInput() throws IOException {
        String input = "{}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        assertEquals(new AnomalyRecord(), new AnomalyRecordParser(parser).parseJson());
        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    public void testParseJson_GivenAnomalyRecordWithAllFieldsPopulatedAndValid()
            throws IOException {
        String input = "{\"detectorIndex\": 3,"
                + "\"probability\" : 0.01,"
                + "\"anomalyScore\" : 42.0,"
                + "\"normalizedProbability\" : 0.05,"
                + "\"byFieldName\" : \"someByFieldName\","
                + "\"byFieldValue\" : \"someByFieldValue\","
                + "\"partitionFieldName\" : \"somePartitionFieldName\","
                + "\"partitionFieldValue\" : \"somePartitionFieldValue\","
                + "\"function\" : \"someFunction\","
                + "\"functionDescription\" : \"someFunctionDesc\","
                + "\"typical\" : [ 3.3 ],"
                + "\"actual\" : [ 1.3 ],"
                + "\"fieldName\" : \"someFieldName\","
                + "\"overFieldName\" : \"someOverFieldName\","
                + "\"overFieldValue\" : \"someOverFieldValue\","
                + "\"causes\" : [{\"probability\" : 0.01}, {\"probability\" : 0.02}],"
                + "\"influencers\" : {"
                + "\"host\": [{\"web-server\": 0.8}, {\"localhost\": 0.7}],"
                + "\"user\": [{\"cat\": 1}, {\"dave\": 0.4},{\"jo\": 0.1}]"
                + "},"
                + "\"isInterim\" : true"
                + "}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        AnomalyRecord anomalyRecord = new AnomalyRecordParser(parser).parseJson();

        assertEquals(3, anomalyRecord.getDetectorIndex());
        assertEquals(0.01, anomalyRecord.getProbability(), EPSILON);
        assertEquals(42.0, anomalyRecord.getAnomalyScore(), EPSILON);
        assertEquals(0.05, anomalyRecord.getNormalizedProbability(), EPSILON);
        assertEquals(0.05, anomalyRecord.getInitialNormalizedProbability(), EPSILON);
        assertEquals("someByFieldName", anomalyRecord.getByFieldName());
        assertEquals("someByFieldValue", anomalyRecord.getByFieldValue());
        assertEquals("somePartitionFieldName", anomalyRecord.getPartitionFieldName());
        assertEquals("somePartitionFieldValue", anomalyRecord.getPartitionFieldValue());
        assertEquals("someFunction", anomalyRecord.getFunction());
        assertEquals("someFunctionDesc", anomalyRecord.getFunctionDescription());
        assertEquals(3.3, anomalyRecord.getTypical().get(0), EPSILON);
        assertEquals(1.3, anomalyRecord.getActual().get(0), EPSILON);
        assertEquals("someFieldName", anomalyRecord.getFieldName());
        assertEquals("someOverFieldName", anomalyRecord.getOverFieldName());
        assertEquals("someOverFieldValue", anomalyRecord.getOverFieldValue());
        assertTrue(anomalyRecord.isInterim());
        assertEquals(2, anomalyRecord.getCauses().size());
        assertEquals(0.01, anomalyRecord.getCauses().get(0).getProbability(), EPSILON);
        assertEquals(0.02, anomalyRecord.getCauses().get(1).getProbability(), EPSILON);

        assertEquals(2, anomalyRecord.getInfluencers().size());
        assertEquals("host", anomalyRecord.getInfluencers().get(0).getInfluencerFieldName());
        assertEquals("user", anomalyRecord.getInfluencers().get(1).getInfluencerFieldName());

        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    public void testParseJson_GivenMultivariateAnomalyRecordWithAllFieldsPopulatedAndValid()
            throws IOException {
        String input = "{\"detectorIndex\": 3,"
                + "\"probability\" : 0.01,"
                + "\"anomalyScore\" : 42.0,"
                + "\"normalizedProbability\" : 0.05,"
                + "\"byFieldName\" : \"someByFieldName\","
                + "\"byFieldValue\" : \"someByFieldValue\","
                + "\"partitionFieldName\" : \"somePartitionFieldName\","
                + "\"partitionFieldValue\" : \"somePartitionFieldValue\","
                + "\"function\" : \"lat_long\","
                + "\"functionDescription\" : \"lat_long\","
                + "\"typical\" : [ 3.3, 75 ],"
                + "\"actual\" : [ -13, 7.34 ],"
                + "\"fieldName\" : \"someFieldName\","
                + "\"influencers\" : {"
                + "\"host\": [{\"web-server\": 0.8}, {\"localhost\": 0.7}],"
                + "\"user\": [{\"cat\": 1}, {\"dave\": 0.4},{\"jo\": 0.1}]"
                + "}"
                + "}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        AnomalyRecord anomalyRecord = new AnomalyRecordParser(parser).parseJson();

        assertEquals(3, anomalyRecord.getDetectorIndex());
        assertEquals(0.01, anomalyRecord.getProbability(), EPSILON);
        assertEquals(42.0, anomalyRecord.getAnomalyScore(), EPSILON);
        assertEquals(0.05, anomalyRecord.getNormalizedProbability(), EPSILON);
        assertEquals(0.05, anomalyRecord.getInitialNormalizedProbability(), EPSILON);
        assertEquals("someByFieldName", anomalyRecord.getByFieldName());
        assertEquals("someByFieldValue", anomalyRecord.getByFieldValue());
        assertEquals("somePartitionFieldName", anomalyRecord.getPartitionFieldName());
        assertEquals("somePartitionFieldValue", anomalyRecord.getPartitionFieldValue());
        assertEquals("lat_long", anomalyRecord.getFunction());
        assertEquals("lat_long", anomalyRecord.getFunctionDescription());
        assertEquals(2, anomalyRecord.getTypical().size());
        assertEquals(3.3, anomalyRecord.getTypical().get(0), EPSILON);
        assertEquals(75, anomalyRecord.getTypical().get(1), EPSILON);
        assertEquals(2, anomalyRecord.getActual().size());
        assertEquals(-13, anomalyRecord.getActual().get(0), EPSILON);
        assertEquals(7.34, anomalyRecord.getActual().get(1), EPSILON);
        assertEquals("someFieldName", anomalyRecord.getFieldName());
        assertNull(anomalyRecord.getOverFieldName());
        assertNull(anomalyRecord.getOverFieldValue());
        assertFalse(anomalyRecord.isInterim());
        assertNull(anomalyRecord.getCauses());

        assertEquals(2, anomalyRecord.getInfluencers().size());
        assertEquals("host", anomalyRecord.getInfluencers().get(0).getInfluencerFieldName());
        assertEquals("user", anomalyRecord.getInfluencers().get(1).getInfluencerFieldName());

        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    private static final JsonParser createJsonParser(String input) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        return new JsonFactory().createParser(inputStream);
    }
}
