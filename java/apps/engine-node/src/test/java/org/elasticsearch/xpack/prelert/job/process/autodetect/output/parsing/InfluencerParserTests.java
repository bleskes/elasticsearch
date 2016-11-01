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
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.results.Influencer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class InfluencerParserTests extends ESTestCase {
    public void testParse() throws IOException {
        String json = "{"
                + "\"probability\": 0.2,"
                + "\"initialAnomalyScore\": 10.0,"
                + "\"influencerFieldName\": \"inf-name\","
                + "\"influencerFieldValue\": \"inf-value\""
                + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        Influencer influencer = new InfluencerParser(parser).parseJson();

        assertEquals("inf-name", influencer.getInfluencerFieldName());
        assertEquals("inf-value", influencer.getInfluencerFieldValue());
        assertEquals(0.2, influencer.getProbability(), 0.0001);
        assertEquals(10.0, influencer.getInitialAnomalyScore(), 0.0001);
        assertEquals(10.0, influencer.getAnomalyScore(), 0.0001);
    }

    public void testParseJson() throws IOException {
        String json = "{\"probability\":0.9,\"initialAnomalyScore\":97.1948,\"influencerFieldName\":\"src_ip\","
                + "\"influencerFieldValue\":\"23.28.243.150\"},";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        Influencer inf = new InfluencerParser(parser).parseJson();

        assertEquals(0.9, inf.getProbability(), 0.0001);
        assertEquals(97.1948, inf.getInitialAnomalyScore(), 0.0001);
        assertEquals(97.1948, inf.getAnomalyScore(), 0.0001);
        assertEquals("src_ip", inf.getInfluencerFieldName());
        assertEquals("23.28.243.150", inf.getInfluencerFieldValue());


        json = "{\"probability\":0.4,\"initialAnomalyScore\":12.1948,\"influencerFieldName\":\"dst_ip\","
                + "\"influencerFieldValue\":\"23.28.243.1\"}";

        parser = createJsonParser(json);
        parser.nextToken();
        inf = new InfluencerParser(parser).parseJson();
        assertEquals(0.4, inf.getProbability(), 0.0001);
        assertEquals(12.1948, inf.getInitialAnomalyScore(), 0.0001);
        assertEquals(12.1948, inf.getAnomalyScore(), 0.0001);
        assertEquals("dst_ip", inf.getInfluencerFieldName());
        assertEquals("23.28.243.1", inf.getInfluencerFieldValue());
    }

    private static JsonParser createJsonParser(String input) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new JsonFactory().createParser(inputStream);
    }

}
